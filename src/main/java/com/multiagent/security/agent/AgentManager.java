package com.multiagent.security.agent;

import com.multiagent.security.bus.EventBus;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;

import java.util.*;

public class AgentManager {
    private final Map<String, Agent> agents = new LinkedHashMap<>();
    private final EventBus bus;
    private final CaseStore caseStore;

    public AgentManager(EventBus bus, CaseStore caseStore) {
        this.bus = bus;
        this.caseStore = caseStore;
    }

    public void register(Agent a) {
        agents.put(a.id(), a);
        bus.register(a);
    }

    public Collection<Agent> list() { return agents.values(); }

    public Optional<Agent> get(String id) { return Optional.ofNullable(agents.get(id)); }

    public void trigger(String id, Event e) {
        Agent a = agents.get(id);
        if (a != null) a.onEvent(e);
    }

    public CaseStore getCaseStore() { return caseStore; }
}
