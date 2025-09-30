package com.multiagent.security.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Db {
    private static final String URL = "jdbc:h2:./data/multiagent;AUTO_SERVER=TRUE";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, "sa", "");
    }

    public static void init() throws SQLException {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS cases (id VARCHAR(64) PRIMARY KEY, title VARCHAR(255), summary CLOB, risk INT, createdAt VARCHAR(64))");
            s.execute("CREATE TABLE IF NOT EXISTS playbooks (id VARCHAR(64) PRIMARY KEY, name VARCHAR(255), definition CLOB)");
            s.execute("CREATE TABLE IF NOT EXISTS tickets (id VARCHAR(64) PRIMARY KEY, title VARCHAR(255), body CLOB)");
            s.execute("CREATE TABLE IF NOT EXISTS playbook_runs (id VARCHAR(64) PRIMARY KEY, playbook_id VARCHAR(64), status VARCHAR(32), createdAt VARCHAR(64))");
            s.execute("CREATE TABLE IF NOT EXISTS playbook_run_steps (id VARCHAR(64) PRIMARY KEY, run_id VARCHAR(64), step_index INT, name VARCHAR(255), definition CLOB, status VARCHAR(32), requires_approval BOOLEAN, approver_role VARCHAR(64), createdAt VARCHAR(64))");
        }
    }
}
