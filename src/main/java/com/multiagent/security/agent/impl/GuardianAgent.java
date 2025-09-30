package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.Event;
import com.multiagent.security.model.CaseStore;

import java.util.Map;

public class GuardianAgent extends AbstractAgent {
    public GuardianAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "guardian"; }

    @Override
    public String name() { return "Vulnerability Guardian"; }

    @Override
    public void onEvent(Event e) {
        if ("vuln_scan".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            int critical = ((Number)d.getOrDefault("critical",0)).intValue();
            if (critical > 0) {
                createCase("Critical vulnerabilities found", "Found " + critical + " critical vulns", 90);
            }
        }
    }
}
