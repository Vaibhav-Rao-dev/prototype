package com.multiagent.security.agent;

import com.multiagent.security.model.CaseFile;
import com.multiagent.security.model.CaseStore;
import com.multiagent.security.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAgent implements Agent {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final CaseStore caseStore;

    protected AbstractAgent(CaseStore caseStore) {
        this.caseStore = caseStore;
    }

    protected void createCase(String title, String summary, int risk) {
        CaseFile c = new CaseFile(title, summary, risk);
        caseStore.add(c);
        log.info("Case created: {} {}", c.getId(), title);
    }
}
