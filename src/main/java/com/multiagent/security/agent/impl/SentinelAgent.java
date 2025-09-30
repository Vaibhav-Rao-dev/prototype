package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class SentinelAgent extends AbstractAgent {
    public SentinelAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "sentinel"; }

    @Override
    public String name() { return "Endpoint Sentinel"; }

    @Override
    public void onEvent(Event e) {
        if ("endpoint".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            boolean ransomwareSigns = (boolean)d.getOrDefault("ransomwareSigns", false);
            if (ransomwareSigns) {
                createCase("Ransomware behavior", "Endpoint shows rapid file encryption patterns", 95);
            }
        }
    }
}
