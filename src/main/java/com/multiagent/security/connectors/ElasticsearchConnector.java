package com.multiagent.security.connectors;

import com.multiagent.security.model.Event;
import com.multiagent.security.bus.EventBus;

import java.util.Map;

/**
 * Very small connector helper that converts an ES-like payload into Events and publishes to the EventBus.
 */
public class ElasticsearchConnector {
    private final EventBus bus;

    public ElasticsearchConnector(EventBus bus) { this.bus = bus; }

    public void ingest(Map payload) {
        // simple mapping: if documents contain severity or error fields, create a log_batch event
        Object hits = payload.get("hits");
        int count = 0;
        if (hits instanceof Map) {
            Object total = ((Map)hits).get("total");
            if (total instanceof Map) count = ((Number)((Map)total).getOrDefault("value",0)).intValue();
            else if (total instanceof Number) count = ((Number)total).intValue();
        }
        bus.publish(new Event("log_batch", Map.of("count", count, "raw", payload)));
    }
}
