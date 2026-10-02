package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.security.PermissionPolicy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminDAO {
    public List<Map<String, Object>> listUsers() throws SQLException {
        List<Map<String, Object>> users = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, email, full_name, status FROM users ORDER BY id");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                long userId = result.getLong("id");
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("id", userId);
                user.put("email", result.getString("email"));
                user.put("fullName", result.getString("full_name"));
                user.put("status", result.getString("status"));
                users.add(user);
                userIds.add(userId);
            }
        }
        try (Connection connection = DBConnection.getConnection()) {
            for (int index = 0; index < users.size(); index++) {
                users.get(index).put("roles", roleCodes(connection, userIds.get(index)));
            }
        }
        return users;
    }

    public Map<String, Object> replaceUserRoles(long actorId, long targetId, List<String> roleCodes)
            throws SQLException {
        Set<String> requested = new LinkedHashSet<>(roleCodes);
        if (requested.isEmpty() || requested.contains("GUEST")) {
            throw new IllegalArgumentException("Tài khoản cần ít nhất một vai trò đăng nhập hợp lệ.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String email;
                String fullName;
                String status;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT email, full_name, status FROM users WHERE id = ? FOR UPDATE")) {
                    statement.setLong(1, targetId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            return null;
                        }
                        email = result.getString("email");
                        fullName = result.getString("full_name");
                        status = result.getString("status");
                    }
                }

                List<String> currentRoles = roleCodes(connection, targetId);
                if (!PermissionPolicy.mayReplaceRoles(actorId, targetId, currentRoles, requested)) {
                    throw new IllegalArgumentException("Bạn không thể tự thu hồi vai trò quản trị của chính mình.");
                }

                List<Integer> roleIds = findRoleIds(connection, requested);
                if (roleIds.size() != requested.size()) {
                    throw new IllegalArgumentException("Có vai trò không tồn tại hoặc không thể gán.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM user_roles WHERE user_id = ?")) {
                    statement.setLong(1, targetId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                    for (int roleId : roleIds) {
                        statement.setLong(1, targetId);
                        statement.setInt(2, roleId);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("id", targetId);
                user.put("email", email);
                user.put("fullName", fullName);
                user.put("status", status);
                user.put("roles", List.copyOf(requested));
                return user;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public Map<String, Object> assignRole(long actorId, long targetId, String roleCode) throws SQLException {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vai trò không hợp lệ.");
        }
        String normalized = roleCode.trim().toUpperCase(java.util.Locale.ROOT);
        List<String> current;
        try (Connection connection = DBConnection.getConnection()) {
            current = roleCodes(connection, targetId);
        }
        if (current.contains(normalized)) {
            throw new IllegalStateException("Người dùng đã có vai trò này.");
        }
        List<String> updated = new ArrayList<>(current);
        updated.add(normalized);
        return replaceUserRoles(actorId, targetId, updated);
    }

    public Map<String, Object> revokeRole(long actorId, long targetId, String roleCode) throws SQLException {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã vai trò không hợp lệ.");
        }
        String normalized = roleCode.trim().toUpperCase(java.util.Locale.ROOT);
        List<String> current;
        try (Connection connection = DBConnection.getConnection()) {
            current = roleCodes(connection, targetId);
        }
        if (!current.contains(normalized)) {
            throw new IllegalStateException("Người dùng không có vai trò này.");
        }
        List<String> updated = new ArrayList<>(current);
        updated.remove(normalized);
        return replaceUserRoles(actorId, targetId, updated);
    }

    private List<String> roleCodes(Connection connection, long userId) throws SQLException {
        List<String> codes = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                        + "WHERE ur.user_id = ? ORDER BY CASE r.role_code WHEN 'ADMIN' THEN 0 ELSE 1 END, r.id")) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) codes.add(result.getString("role_code"));
            }
        }
        return codes;
    }

    private List<Integer> findRoleIds(Connection connection, Set<String> codes) throws SQLException {
        String placeholders = codes.stream().map(ignored -> "?").collect(Collectors.joining(","));
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM roles WHERE role_code IN (" + placeholders + ") AND role_code <> 'GUEST'")) {
            int index = 1;
            for (String code : codes) statement.setString(index++, code);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getInt("id"));
            }
        }
        return ids;
    }
}