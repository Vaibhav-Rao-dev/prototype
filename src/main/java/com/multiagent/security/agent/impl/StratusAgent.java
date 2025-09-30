package com.multiagent.security.agent.impl;

import com.multiagent.security.agent.AbstractAgent;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.Map;

public class StratusAgent extends AbstractAgent {
    public StratusAgent(CaseStore cs) { super(cs); }

    @Override
    public String id() { return "stratus"; }

    @Override
    public String name() { return "Cloud Stratus"; }

    @Override
    public void onEvent(Event e) {
        if ("cloud_scan".equals(e.getType())) {
            Map<String,Object> d = e.getData();
            int publicBuckets = ((Number)d.getOrDefault("publicBuckets",0)).intValue();
            if (publicBuckets > 0) {
                createCase("Public cloud resources", "Found " + publicBuckets + " public buckets/resources", 80);
            }
        }
    }
}
