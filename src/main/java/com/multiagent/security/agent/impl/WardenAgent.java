package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class WardenAgent extends AbstractAgent {
    public WardenAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "warden"; }

    @Override
    public String name() { return "Privileged Access Warden"; }

    @Override
    public void onEvent(Event e) {
        if ("privileged_action".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            boolean outsideHours = (boolean)d.getOrDefault("outsideHours", false);
            boolean suspiciousCmd = (boolean)d.getOrDefault("suspiciousCmd", false);
            if (outsideHours && suspiciousCmd) {
                createCase("Suspicious privileged action", "Privileged action outside hours running suspicious commands", 88);
            }
        }
    }
}
