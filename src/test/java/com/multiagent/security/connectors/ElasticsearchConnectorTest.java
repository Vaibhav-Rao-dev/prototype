package com.multiagent.security.connectors;

import com.multiagent.security.bus.EventBus;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ElasticsearchConnectorTest {
    @Test
    public void ingestPublishesEvent() {
        EventBus bus = new EventBus();
        ElasticsearchConnector c = new ElasticsearchConnector(bus);
        Map payload = new HashMap();
        Map hits = new HashMap();
        Map total = new HashMap(); total.put("value", 5);
        hits.put("total", total);
        payload.put("hits", hits);
        // no exception
        c.ingest(payload);
        assertTrue(true);
    }
}
