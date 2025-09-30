package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class OrchestratorAgent extends AbstractAgent {
    public OrchestratorAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "orchestrator"; }

    @Override
    public String name() { return "SOAR Orchestrator"; }

    @Override
    public void onEvent(Event e) {
        if ("case_created".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            String caseId = (String)d.getOrDefault("caseId", "unknown");
            // simulate running a playbook
            createCase("Playbook run for " + caseId, "Automated containment executed for case " + caseId, 50);
        }
    }
}
