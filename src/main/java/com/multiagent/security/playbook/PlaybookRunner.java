package com.multiagent.security.playbook;

import com.google.gson.Gson;
import com.multiagent.security.db.Db;
import com.multiagent.security.model.CaseFile;
import com.multiagent.security.model.CaseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaybookRunner {
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final Gson gson = new Gson();
    private final CaseStore cs;
    private final Logger log = LoggerFactory.getLogger(PlaybookRunner.class);

    public PlaybookRunner(CaseStore cs) { this.cs = cs; }

    public PlaybookRun run(String playbookId, Map<String,Object> definition) {
        PlaybookRun run = new PlaybookRun(playbookId, "running");
        // persist run
        try (var conn = Db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("MERGE INTO playbook_runs (id, playbook_id, status, createdAt) KEY(id) VALUES (?, ?, ?, ?)");
            ps.setString(1, run.getId()); ps.setString(2, run.getPlaybookId()); ps.setString(3, "running"); ps.setString(4, run.getCreatedAt()); ps.executeUpdate();
        } catch (Exception ex) { throw new RuntimeException(ex); }
        // execute steps in parallel where possible, but don't mark run completed while awaiting approvals
        Object stepsObj = definition.get("steps");
        List<Object> steps;
        if (stepsObj == null) {
            steps = List.of();
        } else if (stepsObj instanceof List) {
            steps = (List<Object>) stepsObj;
        } else {
            // single-step definition
            steps = List.of(stepsObj);
        }

        boolean hasApproval = false;
        // persist steps
        try (var conn = Db.getConnection()) {
            var ps = conn.prepareStatement("MERGE INTO playbook_run_steps (id, run_id, step_index, name, definition, status, requires_approval, approver_role, createdAt) KEY(id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            for (int i = 0; i < steps.size(); i++) {
                Object s = steps.get(i);
                String sid = UUID.randomUUID().toString();
                String defJson = gson.toJson(s);
                boolean needs = false; String approver = null;
                if (s instanceof Map) {
                    needs = Boolean.TRUE.equals(((Map)s).getOrDefault("requires_approval", false));
                    approver = (String)((Map)s).getOrDefault("approver_role", null);
                }
                if (needs) hasApproval = true;
                ps.setString(1, sid);
                ps.setString(2, run.getId());
                ps.setInt(3, i);
                ps.setString(4, "step-" + i);
                ps.setString(5, defJson);
                ps.setString(6, needs ? "pending" : "queued");
                ps.setBoolean(7, needs);
                ps.setString(8, approver);
                ps.setString(9, run.getCreatedAt());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            log.warn("Failed to persist playbook steps: {}", e.getMessage());
        }

        // build futures only for steps that do not require approval
        List<CompletableFuture<?>> futuresList = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            Object s = steps.get(i);
            final Object step = s;
            boolean needs = false;
            if (s instanceof Map) {
                needs = Boolean.TRUE.equals(((Map)s).getOrDefault("requires_approval", false));
            }
            if (needs) continue; // will be executed when approved

            CompletableFuture<?> f = CompletableFuture.runAsync(() -> {
                try {
                    if (step instanceof String) {
                        String act = (String)step;
                        if ("create_ticket".equals(act)) {
                            try (var conn = Db.getConnection()) {
                                var ps = conn.prepareStatement("MERGE INTO tickets (id, title, body) KEY(id) VALUES (?, ?, ?)");
                                String tid = UUID.randomUUID().toString();
                                ps.setString(1, tid); ps.setString(2, "Playbook Ticket"); ps.setString(3, "Created by playbook"); ps.executeUpdate();
                            }
                        } else if ("isolate_host".equals(act)) {
                            CaseFile cf = new CaseFile("Auto-isolate","Host isolated by playbook", 40);
                            cs.add(cf);
                        }
                    } else if (step instanceof Map) {
                        Map sm = (Map)step;
                        String act = (String)sm.getOrDefault("action", "");
                        if ("create_ticket".equals(act)) {
                            try (var conn = Db.getConnection()) {
                                var ps = conn.prepareStatement("MERGE INTO tickets (id, title, body) KEY(id) VALUES (?, ?, ?)");
                                String tid = UUID.randomUUID().toString();
                                ps.setString(1, tid); ps.setString(2, (String)sm.getOrDefault("title","Playbook Ticket")); ps.setString(3, (String)sm.getOrDefault("body","Created by playbook")); ps.executeUpdate();
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error executing playbook step: {}", ex.getMessage());
                }
            }, pool);
            futuresList.add(f);
        }

        if (futuresList.isEmpty()) {
            // No immediate steps to run. If approvals exist, mark run awaiting approval, else complete immediately
            try (var conn = Db.getConnection()) {
                var ps = conn.prepareStatement("UPDATE playbook_runs SET status = ? WHERE id = ?");
                ps.setString(1, hasApproval ? "awaiting_approval" : "completed");
                ps.setString(2, run.getId());
                ps.executeUpdate();
            } catch (Exception ex) {
                log.warn("Failed to update playbook run status: {}", ex.getMessage());
            }
        } else {
            final boolean finalHasApproval = hasApproval;
            CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0])).whenComplete((r,t) -> {
                try (var conn = Db.getConnection()) {
                    var ps = conn.prepareStatement("UPDATE playbook_runs SET status = ? WHERE id = ?");
                    ps.setString(1, finalHasApproval ? "awaiting_approval" : "completed");
                    ps.setString(2, run.getId());
                    ps.executeUpdate();
                } catch (Exception ex) {
                    log.warn("Failed to set playbook run completion: {}", ex.getMessage());
                }
            });
        }
        return run;
    }

    public boolean approveStep(String runId, int stepIndex, String approverRole) {
        try (var conn = Db.getConnection()) {
            var ps = conn.prepareStatement("SELECT id, requires_approval, approver_role, definition FROM playbook_run_steps WHERE run_id = ? AND step_index = ?");
            ps.setString(1, runId); ps.setInt(2, stepIndex);
            var rs = ps.executeQuery();
            if (!rs.next()) return false;
            boolean needs = rs.getBoolean("requires_approval");
            String approver = rs.getString("approver_role");
            String stepId = rs.getString("id");
            String def = rs.getString("definition");
            if (!needs) return false;
            if (approver != null && !approver.equals(approverRole)) return false;
            // set step to running
            var ups = conn.prepareStatement("UPDATE playbook_run_steps SET status = ? WHERE id = ?");
            ups.setString(1, "running"); ups.setString(2, stepId); ups.executeUpdate();
            // execute definition now
            Object s = gson.fromJson(def, Object.class);
            if (s instanceof Map) {
                Map sm = (Map)s;
                String act = (String)sm.getOrDefault("action", "");
                if ("create_ticket".equals(act)) {
                    var tps = conn.prepareStatement("MERGE INTO tickets (id, title, body) KEY(id) VALUES (?, ?, ?)");
                    String tid = java.util.UUID.randomUUID().toString();
                    tps.setString(1, tid); tps.setString(2, (String)sm.getOrDefault("title","Playbook Ticket")); tps.setString(3, (String)sm.getOrDefault("body","Created by playbook")); tps.executeUpdate();
                }
            }
            var fup = conn.prepareStatement("UPDATE playbook_run_steps SET status = ? WHERE id = ?");
            fup.setString(1, "completed"); fup.setString(2, stepId); fup.executeUpdate();
            return true;
        } catch (Exception ex) { return false; }
    }
}
