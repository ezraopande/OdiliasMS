package com.odilias.odms.dao;

import com.odilias.odms.model.AuditLog;
import com.odilias.odms.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {
    public static void log(Integer userId, String module, String action) {
        String query = "INSERT INTO audit_logs (user_id, module, action) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            if (userId != null)
                stmt.setInt(1, userId);
            else
                stmt.setNull(1, java.sql.Types.INTEGER);
            stmt.setString(2, module);
            stmt.setString(3, action);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<AuditLog> getRecentLogs() {
        List<AuditLog> logs = new ArrayList<>();
        String query = "SELECT a.*, u.username FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.timestamp DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                AuditLog log = new AuditLog();
                log.setId(rs.getInt("id"));
                log.setUserId(rs.getObject("user_id", Integer.class));
                log.setUsername(rs.getString("username") != null ? rs.getString("username") : "SYSTEM");
                log.setModule(rs.getString("module"));
                log.setAction(rs.getString("action"));
                log.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime());
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
}
