package com.multiagent.security.playbook;

import java.time.Instant;
import java.util.UUID;

public class PlaybookRun {
    private final String id;
    private final String playbookId;
    private final String status;
    private final String createdAt;

    public PlaybookRun(String playbookId, String status) {
        this.id = UUID.randomUUID().toString();
        this.playbookId = playbookId;
        this.status = status;
        this.createdAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public String getPlaybookId() { return playbookId; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
