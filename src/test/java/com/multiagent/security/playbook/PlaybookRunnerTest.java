package com.multiagent.security.playbook;

import com.multiagent.security.db.Db;
import com.multiagent.security.model.CaseStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlaybookRunnerTest {
    @BeforeAll
    public static void initDb() throws Exception { Db.init(); }

    @Test
    public void approveStepExecutes() throws Exception {
        CaseStore cs = new CaseStore();
        PlaybookRunner r = new PlaybookRunner(cs);
        Map def = Map.of("steps", List.of(Map.of("action","create_ticket","requires_approval", true, "approver_role","manager")));
        var run = r.run("pb-test", def);
        // now approve
        boolean ok = r.approveStep(run.getId(), 0, "manager");
        assertTrue(ok);
    }
}
