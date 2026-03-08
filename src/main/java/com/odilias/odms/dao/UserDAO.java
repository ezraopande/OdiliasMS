package com.odilias.odms.dao;

import com.odilias.odms.model.User;
import com.odilias.odms.util.DatabaseConnection;
import com.odilias.odms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String username, String password) throws SQLException {
        String query = "SELECT u.*, r.role_name FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int attempts = rs.getInt("failed_attempts");
                    String statusStr = rs.getString("status");

                    if ("LOCKED".equals(statusStr)) {
                        throw new SQLException("Account is locked due to security policy. Contact Admin.");
                    }
                    if ("INACTIVE".equals(statusStr)) {
                        throw new SQLException("Account is deactivated. Contact Admin.");
                    }

                    if (attempts >= 3) {
                        lockAccount(id);
                        throw new SQLException("Account has been automatically locked due to multiple failures.");
                    }

                    String storedHash = rs.getString("password_hash");
                    if (SecurityUtil.checkPassword(password, storedHash)) {
                        resetFailedAttempts(id);
                        updateLastLogin(id);

                        User user = new User();
                        user.setId(id);
                        user.setUsername(rs.getString("username"));
                        user.setFullName(rs.getString("full_name"));
                        user.setRole(User.Role.valueOf(rs.getString("role_name")));
                        user.setStatus(User.Status.valueOf(statusStr));

                        AuditDAO.log(id, "AUTH", "User logged in successfully");
                        return user;
                    } else {
                        incrementFailedAttempts(id);
                        AuditDAO.log(id, "AUTH", "Failed login attempt for user: " + username);
                    }
                }
            }
        }
        return null;
    }

    private void incrementFailedAttempts(int userId) throws SQLException {
        String q = "UPDATE users SET failed_attempts = failed_attempts + 1 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, userId);
            s.executeUpdate();
        }
    }

    private void resetFailedAttempts(int userId) throws SQLException {
        String q = "UPDATE users SET failed_attempts = 0 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, userId);
            s.executeUpdate();
        }
    }

    private void lockAccount(int userId) throws SQLException {
        String q = "UPDATE users SET status = 'LOCKED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, userId);
            s.executeUpdate();
            AuditDAO.log(userId, "AUTH", "Account locked due to failed attempts.");
        }
    }

    public void unlockAccount(int userId) throws SQLException {
        String q = "UPDATE users SET status = 'ACTIVE', failed_attempts = 0 WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, userId);
            s.executeUpdate();
            AuditDAO.log(userId, "ADMIN", "Account unlocked by administrator.");
        }
    }

    private void updateLastLogin(int userId) throws SQLException {
        String q = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement s = conn.prepareStatement(q)) {
            s.setInt(1, userId);
            s.executeUpdate();
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT u.id, u.username, u.full_name, r.role_name, u.email, u.contact_number, u.status " +
                "FROM users u JOIN roles r ON u.role_id = r.id";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(User.Role.valueOf(rs.getString("role_name")));
                user.setEmail(rs.getString("email"));
                user.setContactNumber(rs.getString("contact_number"));
                user.setStatus(User.Status.valueOf(rs.getString("status")));
                users.add(user);
            }
        }
        return users;
    }

    public void createUser(User user, String password) throws SQLException {
        String query = "INSERT INTO users (username, password_hash, full_name, role_id, email, contact_number, status) "
                +
                "VALUES (?, ?, ?, (SELECT id FROM roles WHERE role_name = ?), ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, SecurityUtil.hashPassword(password));
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getRole().toString());
            stmt.setString(5, user.getEmail());
            stmt.setString(6, user.getContactNumber());
            stmt.setString(7, user.getStatus().toString());
            stmt.executeUpdate();
        }
    }

    public void updateUserStatus(int userId, User.Status status) throws SQLException {
        String query = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status.toString());
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void adminUpdateUser(User user, String newPassword) throws SQLException {
        String query;
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            query = "UPDATE users SET full_name=?, role_id=(SELECT id FROM roles WHERE role_name=?), email=?, contact_number=?, status=?, password_hash=? WHERE id=?";
        } else {
            query = "UPDATE users SET full_name=?, role_id=(SELECT id FROM roles WHERE role_name=?), email=?, contact_number=?, status=? WHERE id=?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getRole().name());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getContactNumber());
            stmt.setString(5, user.getStatus().name());
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                stmt.setString(6, SecurityUtil.hashPassword(newPassword));
                stmt.setInt(7, user.getId());
            } else {
                stmt.setInt(6, user.getId());
            }
            stmt.executeUpdate();
        }
    }

    public boolean updatePassword(int userId, String currentPwd, String newPwd) throws SQLException {
        String query = "SELECT password_hash FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (SecurityUtil.checkPassword(currentPwd, storedHash)) {
                        String updateQuery = "UPDATE users SET password_hash = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                            updateStmt.setString(1, SecurityUtil.hashPassword(newPwd));
                            updateStmt.setInt(2, userId);
                            updateStmt.executeUpdate();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
