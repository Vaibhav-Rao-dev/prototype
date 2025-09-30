package com.multiagent.security.db;

import com.multiagent.security.model.CaseFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CaseRepositoryTest {
    @BeforeAll
    public static void init() throws Exception { Db.init(); }

    @Test
    public void saveAndList() throws Exception {
        CaseRepository repo = new CaseRepository();
        CaseFile c = new CaseFile("test","summary", 10);
        repo.save(c);
        List<CaseFile> all = repo.list();
        assertTrue(all.stream().anyMatch(x -> x.getId().equals(c.getId())));
    }
}
