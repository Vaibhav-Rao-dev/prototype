package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class AnalystAgent extends AbstractAgent {
    public AnalystAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "analyst"; }

    @Override
    public String name() { return "Log Analyst"; }

    @Override
    public void onEvent(Event e) {
        if ("log_batch".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            int errors = ((Number)d.getOrDefault("errorCount",0)).intValue();
            if (errors > 1000) {
                createCase("High error volume", "Large log error rate: " + errors, 60);
            }
        }
    }
}
