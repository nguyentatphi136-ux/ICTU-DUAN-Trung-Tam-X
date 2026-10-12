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

public class PermissionDAO {
    private static final Set<String> ADMIN_CORE_PERMISSIONS = Set.of(
            "ROLE_PERMISSION_READ", "ROLE_PERMISSION_UPDATE", "USER_READ", "USER_ROLE_ASSIGN", "MENU_VIEW"
    );

    public boolean hasPermission(long userId, String permissionCode) throws SQLException {
        List<String> currentGrants = new ArrayList<>();
        String sql = "SELECT DISTINCT p.permission_code FROM users u JOIN user_roles ur ON ur.user_id = u.id "
                + "JOIN role_permissions rp ON rp.role_id = ur.role_id "
                + "JOIN permissions p ON p.id = rp.permission_id "
                + "WHERE u.id = ? AND u.status = 'ACTIVE'";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) currentGrants.add(result.getString("permission_code"));
            }
        }
        return PermissionPolicy.allows(currentGrants, permissionCode);
    }

    public List<Map<String, Object>> listRoles() throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        String sql = "SELECT r.id, r.role_code, r.role_name, r.description, "
                + "GROUP_CONCAT(p.permission_code ORDER BY p.permission_code SEPARATOR ',') AS grants "
                + "FROM roles r LEFT JOIN role_permissions rp ON rp.role_id = r.id "
                + "LEFT JOIN permissions p ON p.id = rp.permission_id "
                + "GROUP BY r.id, r.role_code, r.role_name, r.description ORDER BY r.id";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Map<String, Object> role = new LinkedHashMap<>();
                role.put("id", result.getInt("id"));
                role.put("roleCode", result.getString("role_code"));
                role.put("roleName", result.getString("role_name"));
                role.put("description", result.getString("description"));
                String grants = result.getString("grants");
                role.put("permissionCodes", grants == null ? List.of() : List.of(grants.split(",")));
                roles.add(role);
            }
        }
        return roles;
    }

    public List<Map<String, Object>> listPermissions() throws SQLException {
        List<Map<String, Object>> permissions = new ArrayList<>();
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT permission_code, permission_name, module, description "
                             + "FROM permissions ORDER BY module, permission_code");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Map<String, Object> permission = new LinkedHashMap<>();
                permission.put("permissionCode", result.getString("permission_code"));
                permission.put("permissionName", result.getString("permission_name"));
                permission.put("module", result.getString("module"));
                permission.put("description", result.getString("description"));
                permissions.add(permission);
            }
        }
        return permissions;
    }

    public List<String> permissionsForRole(String roleCode) throws SQLException {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT p.permission_code FROM roles r "
                + "JOIN role_permissions rp ON rp.role_id = r.id "
                + "JOIN permissions p ON p.id = rp.permission_id WHERE r.role_code = ? ORDER BY p.permission_code";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleCode);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) codes.add(result.getString("permission_code"));
            }
        }
        return codes;
    }

    public Map<String, Object> replaceRolePermissions(String roleCode, List<String> permissionCodes)
            throws SQLException {
        Set<String> requested = new LinkedHashSet<>(permissionCodes);
        if ("ADMIN".equals(roleCode) && !requested.containsAll(ADMIN_CORE_PERMISSIONS)) {
            throw new IllegalArgumentException("Không thể thu hồi các quyền quản trị cốt lõi.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int roleId;
                String roleName;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id, role_name FROM roles WHERE role_code = ? FOR UPDATE")) {
                    statement.setString(1, roleCode);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            return null;
                        }
                        roleId = result.getInt("id");
                        roleName = result.getString("role_name");
                    }
                }

                List<Integer> permissionIds = findPermissionIds(connection, requested);
                if (permissionIds.size() != requested.size()) {
                    throw new IllegalArgumentException("Có quyền không tồn tại trong hệ thống.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM role_permissions WHERE role_id = ?")) {
                    statement.setInt(1, roleId);
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)")) {
                    for (int permissionId : permissionIds) {
                        statement.setInt(1, roleId);
                        statement.setInt(2, permissionId);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
                Map<String, Object> updated = new LinkedHashMap<>();
                updated.put("roleCode", roleCode);
                updated.put("roleName", roleName);
                updated.put("permissionCodes", List.copyOf(requested));
                return updated;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<Map<String, Object>> menuForUser(long userId) throws SQLException {
        List<Map<String, Object>> menu = new ArrayList<>();
        String sql = "SELECT DISTINCT m.menu_code, m.title, m.href, m.icon, m.sort_order FROM users u "
                + "JOIN user_roles ur ON ur.user_id = u.id "
                + "JOIN role_permissions rp ON rp.role_id = ur.role_id "
                + "JOIN menu_permissions mp ON mp.permission_id = rp.permission_id "
                + "JOIN menus m ON m.id = mp.menu_id "
                + "WHERE u.id = ? AND u.status = 'ACTIVE' ORDER BY m.sort_order, m.menu_code";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("slug", result.getString("menu_code"));
                    item.put("title", result.getString("title"));
                    item.put("href", result.getString("href"));
                    item.put("icon", result.getString("icon"));
                    menu.add(item);
                }
            }
        }
        return menu;
    }

    public List<Map<String, Object>> listMenus() throws SQLException {
        List<Map<String, Object>> menus = new ArrayList<>();
        String sql = "SELECT m.menu_code, m.title, m.href, m.icon, m.sort_order, "
                + "GROUP_CONCAT(p.permission_code ORDER BY p.permission_code SEPARATOR ',') AS required_permissions "
                + "FROM menus m LEFT JOIN menu_permissions mp ON mp.menu_id = m.id "
                + "LEFT JOIN permissions p ON p.id = mp.permission_id "
                + "GROUP BY m.id, m.menu_code, m.title, m.href, m.icon, m.sort_order ORDER BY m.sort_order";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("slug", result.getString("menu_code"));
                item.put("title", result.getString("title"));
                item.put("href", result.getString("href"));
                item.put("icon", result.getString("icon"));
                String grants = result.getString("required_permissions");
                item.put("requiredPermissions", grants == null ? List.of() : List.of(grants.split(",")));
                menus.add(item);
            }
        }
        return menus;
    }

    private List<Integer> findPermissionIds(Connection connection, Set<String> codes) throws SQLException {
        if (codes.isEmpty()) return List.of();
        String placeholders = codes.stream().map(ignored -> "?").collect(Collectors.joining(","));
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM permissions WHERE permission_code IN (" + placeholders + ")")) {
            int index = 1;
            for (String code : codes) statement.setString(index++, code);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getInt("id"));
            }
        }
        return ids;
    }
}