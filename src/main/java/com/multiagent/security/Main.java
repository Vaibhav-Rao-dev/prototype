package com.multiagent.security;

import com.google.gson.Gson;
import com.multiagent.security.agent.AgentManager;
import com.multiagent.security.agent.impl.*;
import com.multiagent.security.bus.EventBus;
import com.multiagent.security.model.CaseFile;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;
import spark.Spark;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.multiagent.security.playbook.PlaybookRunner;
import com.multiagent.security.playbook.PlaybookRun;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
    try { com.multiagent.security.db.Db.init(); } catch (Exception ex) { ex.printStackTrace(); }
    final EventBus bus = new EventBus();
    final CaseStore cs = new CaseStore();
        final AgentManager mgr = new AgentManager(bus, cs);

        // register agents
        mgr.register(new HunterAgent(cs));
        mgr.register(new GuardianAgent(cs));
        mgr.register(new AnalystAgent(cs));
        mgr.register(new PhalanxAgent(cs));
        mgr.register(new WardenAgent(cs));
        mgr.register(new StratusAgent(cs));
        mgr.register(new SentinelAgent(cs));
        mgr.register(new OrchestratorAgent(cs));

    final Gson gson = new Gson();

        Spark.port(4567);

    // serve UI by reading ui/index.html to avoid static files lifecycle issues
    Spark.get("/ui", (req,res) -> {
        res.type("text/html");
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("ui/index.html");
            if (java.nio.file.Files.exists(p)) {
                return java.nio.file.Files.readString(p);
            } else {
                res.status(404);
                return "UI not found";
            }
        } catch (Exception ex) {
            res.status(500);
            return "error reading UI";
        }
    });

        Spark.get("/agents", (req, res) -> {
            res.type("application/json");
            List<Map<String, String>> list = mgr.list().stream().map(a -> {
                Map<String,String> m = new HashMap<>();
                m.put("id", a.id());
                m.put("name", a.name());
                return m;
            }).collect(java.util.stream.Collectors.toList());
            return gson.toJson(list);
        });

        Spark.post("/agents/:id/trigger", (req, res) -> {
            String id = req.params(":id");
            Map body = gson.fromJson(req.body(), Map.class);
            Event e = new Event((String)body.getOrDefault("type", "custom"), body);
            mgr.trigger(id, e);
            res.type("application/json");
            return gson.toJson(Map.of("status","triggered","agent", id));
        });

        Spark.post("/publish", (req,res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            Event e = new Event((String)body.getOrDefault("type","custom"), body);
            bus.publish(e);
            res.type("application/json");
            return gson.toJson(Map.of("status","published","type", e.getType()));
        });

        Spark.get("/cases", (req, res) -> {
            res.type("application/json");
            List<CaseFile> cases = cs.list();
            return gson.toJson(cases);
        });

        // get single case details
        Spark.get("/cases/:id", (req, res) -> {
            res.type("application/json");
            String id = req.params(":id");
            // simple repo lookup
            CaseFile found = null;
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT id, title, summary, risk, createdAt FROM cases WHERE id = ?");
                ps.setString(1, id);
                var rs = ps.executeQuery();
                if (rs.next()) {
                    found = new CaseFile(rs.getString("title"), rs.getString("summary"), rs.getInt("risk"), rs.getString("id"), rs.getString("createdAt"));
                }
            } catch (Exception ex) { /* ignore */ }
            if (found == null) {
                res.status(404);
                return gson.toJson(Map.of("error", "not_found"));
            }
            // include a simple timeline placeholder if none present
            Map<String,Object> out = new HashMap<>();
            out.put("id", found.getId());
            out.put("title", found.getTitle());
            out.put("summary", found.getSummary());
            out.put("riskScore", found.getRiskScore());
            out.put("createdAt", found.getCreatedAt());
            out.put("timeline", java.util.List.of(Map.of("time", found.getCreatedAt(), "detail", "Case created")));
            return gson.toJson(out);
        });

        Spark.get("/ui", (req,res) -> {
            res.redirect("/index.html");
            return null;
        });

        // optional OIDC: if env KEYCLOAK_JWKS provided, create validator and register an auth before-filter
        final java.util.concurrent.atomic.AtomicReference<com.multiagent.security.auth.JwtValidator> jwtRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        final java.util.concurrent.atomic.AtomicReference<com.multiagent.security.auth.AuthFilter> authRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        String jwks = System.getenv("KEYCLOAK_JWKS");
        if (jwks != null && !jwks.isBlank()) {
            try {
                jwtRef.set(new com.multiagent.security.auth.JwtValidator(jwks));
                authRef.set(new com.multiagent.security.auth.AuthFilter(jwtRef.get()));
                // attach roles to each request if token present
                Spark.before((req, res) -> {
                    try {
                        var af = authRef.get();
                        if (af != null) {
                            var roles = af.extractRoles(req);
                            req.attribute("roles", roles);
                        }
                    } catch (SecurityException se) {
                        // missing token: leave unauthenticated; endpoints will enforce if required
                    } catch (Exception ex) { ex.printStackTrace(); }
                });
            } catch (Exception ex) { ex.printStackTrace(); }
        }

        // ingest Nuclei JSON (simple adapter)
        Spark.post("/ingest/nuclei", (req,res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            // expect body contains 'vulnerabilities' as list of maps
            Object vulns = body.getOrDefault("vulnerabilities", null);
            int critical = 0;
            if (vulns instanceof java.util.List) {
                for (Object o : (java.util.List)vulns) {
                    if (o instanceof java.util.Map) {
                        String severity = (String)((java.util.Map)o).getOrDefault("severity","info");
                        if ("critical".equalsIgnoreCase(severity)) critical++;
                    }
                }
            }
            Event e = new Event("vuln_scan", Map.of("critical", critical));
            bus.publish(e);
            res.type("application/json");
            return gson.toJson(Map.of("status","ingested","critical", critical));
        });

        // ingest Elasticsearch-style batch
        Spark.post("/ingest/elasticsearch", (req,res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            int errors = ((Number)body.getOrDefault("errorCount",0)).intValue();
            Event e = new Event("log_batch", Map.of("errorCount", errors));
            bus.publish(e);
            return gson.toJson(Map.of("status","ingested","errorCount", errors));
        });

        // Case actions: isolate/lock/scan/execute playbook
        Spark.post("/cases/:id/action", (req,res) -> {
            String id = req.params(":id");
            Map body = gson.fromJson(req.body(), Map.class);
            String action = (String)body.getOrDefault("action", "");
            Map params = (Map)body.getOrDefault("parameters", Map.of());
            // publish an Event so agents can react
            Map payload = new HashMap();
            payload.put("caseId", id);
            payload.put("action", action);
            payload.put("parameters", params);
            Event e = new Event("case_action", payload);
            bus.publish(e);
            res.type("application/json");
            return gson.toJson(Map.of("status","ok","action", action));
        });

        // playbook endpoints
        Spark.post("/playbooks", (req,res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            String id = java.util.UUID.randomUUID().toString();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("MERGE INTO playbooks (id, name, definition) KEY(id) VALUES (?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, (String)body.getOrDefault("name","playbook"));
                ps.setString(3, gson.toJson(body.getOrDefault("definition", new Object())));
                ps.executeUpdate();
            }
            res.type("application/json");
            return gson.toJson(Map.of("id", id));
        });

        // list playbooks
        Spark.get("/playbooks", (req,res) -> {
            res.type("application/json");
            var out = new java.util.ArrayList();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT id, name FROM playbooks ORDER BY name");
                var rs = ps.executeQuery();
                while (rs.next()) {
                    out.add(Map.of("id", rs.getString(1), "name", rs.getString(2)));
                }
            } catch (Exception ex) { /* ignore */ }
            return gson.toJson(out);
        });

        // helper: require roles on a request (returns true if ok, sets status on response otherwise)
        java.util.function.BiFunction<Request, String[], Boolean> requireRoles = (req, required) -> {
            var af = authRef.get();
            if (af == null) return true; // auth not enabled in dev
            Object r = req.attribute("roles");
            if (!(r instanceof java.util.List)) {
                req.attribute("auth_error", "unauthenticated");
                return false;
            }
            java.util.List roles = (java.util.List)r;
            for (String want : required) if (roles.contains(want)) return true;
            req.attribute("auth_error", "forbidden");
            return false;
        };
    final PlaybookRunner runner = new com.multiagent.security.playbook.PlaybookRunner(cs);
        Spark.post("/playbooks/:id/run", (req,res) -> {
            // optional auth check: require soc_analyst role
            var af = authRef.get();
            if (af != null) {
                var roles = req.attribute("roles");
                if (!(roles instanceof java.util.List)) { res.status(401); return gson.toJson(Map.of("error","unauth")); }
                if (!((java.util.List)roles).contains("soc_analyst")) { res.status(403); return gson.toJson(Map.of("error","forbidden")); }
            }
            String id = req.params(":id");
            Map body = gson.fromJson(req.body(), Map.class);
            // load playbook definition from DB
            Map definition = Map.of();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT definition FROM playbooks WHERE id = ?");
                ps.setString(1, id);
                var rs = ps.executeQuery();
                if (rs.next()) definition = gson.fromJson(rs.getString(1), Map.class);
            }
            PlaybookRun run = runner.run(id, definition);
            res.type("application/json");
            return gson.toJson(Map.of("status","running","runId", run.getId()));
        });

        // list playbook runs
        Spark.get("/playbook_runs", (req,res) -> {
            res.type("application/json");
            var list = new java.util.ArrayList();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT id, playbook_id, status, createdAt FROM playbook_runs ORDER BY createdAt DESC");
                var rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(Map.of("id", rs.getString(1), "playbookId", rs.getString(2), "status", rs.getString(3), "createdAt", rs.getString(4)));
                }
            }
            return gson.toJson(list);
        });

        // Hunter convenience endpoints: landscape, cases, search
        Spark.get("/hunter/landscape", (req,res) -> {
            res.type("application/json");
            // simple static demo landscape compatible with UI's expectations
            Map asset1 = Map.of("id","srv-1","x",60,"y",40,"status","normal");
            Map asset2 = Map.of("id","db-1","x",240,"y",40,"status","normal");
            Map asset3 = Map.of("id","fw-1","x",150,"y",160,"status","anomalous");
            Map link1 = Map.of("x1",72,"y1",40,"x2",228,"y2",40,"anomaly",false);
            Map link2 = Map.of("x1",150,"y1",52,"x2",150,"y2",148,"anomaly",true);
            return gson.toJson(Map.of("assets", java.util.List.of(asset1,asset2,asset3), "links", java.util.List.of(link1,link2)));
        });

        Spark.get("/hunter/cases", (req,res) -> {
            res.type("application/json");
            // currently return all cases as convenience
            List<CaseFile> cases = cs.list();
            return gson.toJson(cases);
        });

        Spark.post("/hunter/search", (req,res) -> {
            Map body = gson.fromJson(req.body(), Map.class);
            String nl = (String)body.getOrDefault("nl", "");
            // publish a hunt event and return a simple acknowledgement/result
            Event e = new Event("hunter_search", Map.of("nl", nl));
            bus.publish(e);
            // respond with a lightweight result for UI
            return gson.toJson(Map.of("status","submitted","query", nl));
        });

        // list steps for a run
        Spark.get("/playbook_runs/:id/steps", (req,res) -> {
            res.type("application/json");
            String runId = req.params(":id");
            var list = new java.util.ArrayList();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT step_index, name, status, requires_approval, approver_role, definition FROM playbook_run_steps WHERE run_id = ? ORDER BY step_index");
                ps.setString(1, runId);
                var rs = ps.executeQuery();
                while (rs.next()) {
                    list.add(Map.of("index", rs.getInt(1), "name", rs.getString(2), "status", rs.getString(3), "requiresApproval", rs.getBoolean(4), "approverRole", rs.getString(5), "definition", rs.getString(6)));
                }
            }
            return gson.toJson(list);
        });

        // run details: summary + steps
        Spark.get("/playbook_runs/:id", (req,res) -> {
            res.type("application/json");
            String runId = req.params(":id");
            Map summary = Map.of();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT id, playbook_id, status, createdAt FROM playbook_runs WHERE id = ?");
                ps.setString(1, runId);
                var rs = ps.executeQuery();
                if (rs.next()) summary = Map.of("id", rs.getString(1), "playbookId", rs.getString(2), "status", rs.getString(3), "createdAt", rs.getString(4));
            }
            // steps
            var steps = new java.util.ArrayList();
            try (var conn = com.multiagent.security.db.Db.getConnection()) {
                var ps = conn.prepareStatement("SELECT step_index, name, status, requires_approval, approver_role, definition FROM playbook_run_steps WHERE run_id = ? ORDER BY step_index");
                ps.setString(1, runId);
                var rs = ps.executeQuery();
                while (rs.next()) {
                    steps.add(Map.of("index", rs.getInt(1), "name", rs.getString(2), "status", rs.getString(3), "requiresApproval", rs.getBoolean(4), "approverRole", rs.getString(5), "definition", rs.getString(6)));
                }
            }
            return gson.toJson(Map.of("summary", summary, "steps", steps));
        });

        // approve a step
        Spark.post("/playbook_runs/:id/steps/:index/approve", (req,res) -> {
            // require auth if jwks configured
            var jv = jwtRef.get();
            if (jv != null) {
                String auth = req.headers("Authorization");
                if (auth == null || !auth.startsWith("Bearer ")) { res.status(401); return gson.toJson(Map.of("error","unauth")); }
                try { var roles = jv.getRoles(auth.substring(7)); if (!roles.contains("manager") && !roles.contains("orchestrator")) { res.status(403); return gson.toJson(Map.of("error","forbidden")); } } catch (Exception ex) { res.status(401); return gson.toJson(Map.of("error","invalid_token")); }
            }
            String runId = req.params(":id");
            int idx = Integer.parseInt(req.params(":index"));
            String role = req.queryParams("role");
            boolean ok = runner.approveStep(runId, idx, role==null?"manager":role);
            if (!ok) { res.status(400); return gson.toJson(Map.of("status","failed")); }
            return gson.toJson(Map.of("status","approved"));
        });

        log.info("Multi-agent security prototype running on http://localhost:4567");
    }
}
