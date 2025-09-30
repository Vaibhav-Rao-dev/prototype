package com.multiagent.security.model;

import com.multiagent.security.db.CaseRepository;

import java.util.List;

public class CaseStore {
    private final CaseRepository repo = new CaseRepository();

    public void add(CaseFile c) {
        try { repo.save(c); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<CaseFile> list() { try { return repo.list(); } catch (Exception e) { throw new RuntimeException(e); } }
}

