package com.multiagent.security.db;

import com.multiagent.security.model.CaseFile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CaseRepository {
    public void save(CaseFile c) throws Exception {
        try (Connection conn = Db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("MERGE INTO cases (id, title, summary, risk, createdAt) KEY(id) VALUES (?, ?, ?, ?, ?)");
            ps.setString(1, c.getId());
            ps.setString(2, c.getTitle());
            ps.setString(3, c.getSummary());
            ps.setInt(4, c.getRiskScore());
            ps.setString(5, c.getCreatedAt());
            ps.executeUpdate();
        }
    }

    public List<CaseFile> list() throws Exception {
        List<CaseFile> out = new ArrayList<>();
        try (Connection conn = Db.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT id, title, summary, risk, createdAt FROM cases ORDER BY createdAt DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                CaseFile c = new CaseFile(rs.getString("title"), rs.getString("summary"), rs.getInt("risk"), rs.getString("id"), rs.getString("createdAt"));
                out.add(c);
            }
        }
        return out;
    }
}
