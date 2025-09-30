package com.multiagent.security.agent;

import com.multiagent.security.model.Event;

public interface Agent {
    String id();
    String name();
    void onEvent(Event e);
}
