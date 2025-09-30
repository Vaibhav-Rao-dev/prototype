package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class PhalanxAgent extends AbstractAgent {
    public PhalanxAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "phalanx"; }

    @Override
    public String name() { return "Email Defender (Phalanx)"; }

    @Override
    public void onEvent(Event e) {
        if ("email".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            int maliciousLinks = ((Number)d.getOrDefault("maliciousLinks",0)).intValue();
            if (maliciousLinks > 0) {
                createCase("Phishing campaign detected", "Email contains " + maliciousLinks + " malicious links", 75);
            }
        }
    }
}
