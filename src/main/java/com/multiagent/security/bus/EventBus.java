package com.multiagent.security.bus;

import com.multiagent.security.agent.Agent;
import com.multiagent.security.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class EventBus {
    private final Logger log = LoggerFactory.getLogger(EventBus.class);
    private final List<Agent> subscribers = new ArrayList<>();

    public void register(Agent a) { subscribers.add(a); log.info("Agent registered: {}", a.name()); }

    public void publish(Event e) {
        log.info("Publishing event {} to {} agents", e.getType(), subscribers.size());
        for (Agent a : subscribers) {
            try {
                a.onEvent(e);
            } catch (Exception ex) {
                log.error("Agent {} failed to handle event {}: {}", a.name(), e.getType(), ex.getMessage());
            }
        }
    }
}
