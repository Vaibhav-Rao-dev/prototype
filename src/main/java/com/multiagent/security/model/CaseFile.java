package com.multiagent.security.model;

import java.time.Instant;
import java.util.UUID;

public class CaseFile {
    private final String id;
    private final String title;
    private final String summary;
    private final int riskScore;
    // store createdAt as ISO string to simplify JSON serialization
    private final String createdAt;

    public CaseFile(String title, String summary, int riskScore) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.summary = summary;
        this.riskScore = riskScore;
        this.createdAt = Instant.now().toString();
    }

    // constructor used when loading from DB
    public CaseFile(String title, String summary, int riskScore, String id, String createdAt) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.riskScore = riskScore;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public int getRiskScore() { return riskScore; }
    public String getCreatedAt() { return createdAt; }
}
