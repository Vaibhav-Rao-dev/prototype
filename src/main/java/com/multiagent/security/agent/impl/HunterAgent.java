package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.Event;
import com.multiagent.security.model.CaseFile;
import com.multiagent.security.model.CaseStore;

import java.util.Map;

public class HunterAgent extends AbstractAgent {
    public HunterAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "hunter"; }

    @Override
    public String name() { return "Threat Hunter"; }

    @Override
    public void onEvent(Event e) {
        // simulate anomaly detection
        Map<String,Object> data = e.getData();
        if ("login".equals(e.getType()) && data != null && data.containsKey("failedCount")) {
            int failed = ((Number)data.get("failedCount")).intValue();
            if (failed > 50) {
                createCase("Brute force activity", "Detected >50 failed logins", 85);
            }
        }
    }
}
