package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.security.PermissionPolicy;
import org.mindrot.jbcrypt.BCrypt;
import java.security.SecureRandom;
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
        return (List<Map<String, Object>>) searchUsers(null, null, null, 1, 100).get("items");
    }

    /**
     * Chức năng S1-08: Tìm kiếm, lọc và phân trang người dùng
     * - Tìm kiếm theo họ tên, email, số điện thoại, mã nhân sự
     * - Lọc theo vai trò và trạng thái
     * - Phân trang mặc định 20 dòng
     */
    public Map<String, Object> searchUsers(String keyword, String roleCode, String status, int page, int pageSize)
            throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        StringBuilder whereSql = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = "%" + keyword.trim() + "%";
            whereSql.append(" AND (u.full_name LIKE ? OR u.email LIKE ? OR u.phone LIKE ? OR u.user_code LIKE ?) ");
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            whereSql.append(" AND u.status = ? ");
            params.add(status.trim().toUpperCase(java.util.Locale.ROOT));
        }

        if (roleCode != null && !roleCode.trim().isEmpty() && !"ALL".equalsIgnoreCase(roleCode.trim())) {
            whereSql.append(" AND EXISTS (SELECT 1 FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.role_code = ?) ");
            params.add(roleCode.trim().toUpperCase(java.util.Locale.ROOT));
        }

        int totalCount = 0;
        List<Map<String, Object>> users = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection()) {
            // Count query
            String countSql = "SELECT COUNT(*) FROM users u " + whereSql;
            try (PreparedStatement stmt = connection.prepareStatement(countSql)) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) totalCount = rs.getInt(1);
                }
            }

            // Data query with pagination
            int offset = (page - 1) * pageSize;
            String dataSql = "SELECT u.id, u.user_code, u.email, u.full_name, u.phone, u.status, u.locked_reason, u.created_at "
                    + "FROM users u " + whereSql + " ORDER BY u.id DESC LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = connection.prepareStatement(dataSql)) {
                int paramIdx = 1;
                for (Object param : params) {
                    stmt.setObject(paramIdx++, param);
                }
                stmt.setInt(paramIdx++, pageSize);
                stmt.setInt(paramIdx, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        long userId = rs.getLong("id");
                        Map<String, Object> user = new LinkedHashMap<>();
                        user.put("id", userId);
                        user.put("userCode", rs.getString("user_code"));
                        user.put("email", rs.getString("email"));
                        user.put("fullName", rs.getString("full_name"));
                        user.put("phone", rs.getString("phone") != null ? rs.getString("phone") : "");
                        user.put("status", rs.getString("status"));
                        user.put("lockedReason", rs.getString("locked_reason"));
                        users.add(user);
                        userIds.add(userId);
                    }
                }
            }

            // Gán roles
            for (int i = 0; i < users.size(); i++) {
                users.get(i).put("roles", roleCodes(connection, userIds.get(i)));
            }
        }

        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        if (totalPages == 0) totalPages = 1;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", users);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", totalPages);
        return result;
    }

    /**
     * Chức năng S1-08: Tạo tài khoản người dùng mới
     * - Bắt buộc họ tên và email hợp lệ
     * - Chặn trùng email kèm thông báo lỗi cụ thể
     * - Tự động sinh mật khẩu tạm nếu chưa cung cấp
     * - Gán vai trò ban đầu
     * - Ghi nhật ký vào bảng audit_logs
     */
    public Map<String, Object> createUser(long actorId, String userCode, String email, String fullName,
                                         String phone, List<String> roleCodes, String tempPassword) throws SQLException {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Địa chỉ email không hợp lệ.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập họ và tên người dùng.");
        }

        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        String finalPassword = (tempPassword != null && !tempPassword.trim().isEmpty())
                ? tempPassword.trim() : generateTempPassword();

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Kiểm tra trùng email
                try (PreparedStatement checkStmt = connection.prepareStatement("SELECT id FROM users WHERE email = ?")) {
                    checkStmt.setString(1, normalizedEmail);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException("Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!");
                        }
                    }
                }

                String finalUserCode = (userCode != null && !userCode.trim().isEmpty())
                        ? userCode.trim().toUpperCase(java.util.Locale.ROOT)
                        : "NV" + (System.currentTimeMillis() % 1000000);

                String hashed = BCrypt.hashpw(finalPassword, BCrypt.gensalt(10));
                long newUserId = 0;
                String insertSql = "INSERT INTO users (user_code, email, password_hash, full_name, phone, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, finalUserCode);
                    insertStmt.setString(2, normalizedEmail);
                    insertStmt.setString(3, hashed);
                    insertStmt.setString(4, fullName.trim());
                    insertStmt.setString(5, phone != null ? phone.trim() : null);
                    insertStmt.executeUpdate();
                    try (ResultSet generated = insertStmt.getGeneratedKeys()) {
                        if (generated.next()) newUserId = generated.getLong(1);
                    }
                }

                // Gán vai trò ban đầu
                List<String> validRoles = (roleCodes != null && !roleCodes.isEmpty()) ? roleCodes : List.of("STUDENT");
                List<Integer> roleIds = findRoleIds(connection, new LinkedHashSet<>(validRoles));
                if (!roleIds.isEmpty()) {
                    try (PreparedStatement roleStmt = connection.prepareStatement(
                            "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                        for (int roleId : roleIds) {
                            roleStmt.setLong(1, newUserId);
                            roleStmt.setInt(2, roleId);
                            roleStmt.addBatch();
                        }
                        roleStmt.executeBatch();
                    }
                }

                recordAuditLog(connection, actorId, "CREATE_USER", "USER", String.valueOf(newUserId),
                        null, "{\"email\":\"" + normalizedEmail + "\",\"full_name\":\"" + fullName.trim() + "\"}");

                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", newUserId);
                result.put("userCode", finalUserCode);
                result.put("email", normalizedEmail);
                result.put("fullName", fullName.trim());
                result.put("phone", phone != null ? phone.trim() : "");
                result.put("status", "ACTIVE");
                result.put("roles", validRoles);
                result.put("tempPassword", finalPassword);

                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    /**
     * Chức năng S1-08: Sửa thông tin tài khoản người dùng
     */
    public Map<String, Object> updateUserDetails(long actorId, long targetId, String fullName, String phone) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldName = null;
                String oldPhone = null;
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT full_name, phone FROM users WHERE id = ? FOR UPDATE")) {
                    checkStmt.setLong(1, targetId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.commit();
                            return null;
                        }
                        oldName = rs.getString("full_name");
                        oldPhone = rs.getString("phone");
                    }
                }

                try (PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET full_name = ?, phone = ?, updated_at = NOW() WHERE id = ?")) {
                    updateStmt.setString(1, fullName.trim());
                    updateStmt.setString(2, phone != null ? phone.trim() : null);
                    updateStmt.setLong(3, targetId);
                    updateStmt.executeUpdate();
                }

                recordAuditLog(connection, actorId, "UPDATE_USER", "USER", String.valueOf(targetId),
                        "{\"full_name\":\"" + oldName + "\",\"phone\":\"" + oldPhone + "\"}",
                        "{\"full_name\":\"" + fullName.trim() + "\",\"phone\":\"" + phone + "\"}");

                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", targetId);
                result.put("fullName", fullName.trim());
                result.put("phone", phone != null ? phone.trim() : "");
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Edu@");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
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

    /**
     * Chức năng S1-10: Khóa và mở khóa tài khoản
     * - Bắt buộc ghi lý do khóa
     * - Chặn Admin tự khóa tài khoản của chính mình
     * - Thu hồi phiên đăng nhập khi khóa
     * - Kiểm tra các lớp học phụ trách cần bàn giao
     * - Ghi nhật ký vào bảng audit_logs
     */
    public Map<String, Object> updateUserStatus(long actorId, long targetId, String newStatus, String lockedReason)
            throws SQLException {
        if (newStatus == null || (!newStatus.equalsIgnoreCase("ACTIVE") && !newStatus.equalsIgnoreCase("LOCKED"))) {
            throw new IllegalArgumentException("Trạng thái tài khoản không hợp lệ (chỉ nhận ACTIVE hoặc LOCKED).");
        }
        String normalizedStatus = newStatus.toUpperCase(java.util.Locale.ROOT);

        if (actorId == targetId && !"ACTIVE".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Không thể tự khoá hoặc ngừng hoạt động tài khoản của chính mình.");
        }

        if ("LOCKED".equals(normalizedStatus) && (lockedReason == null || lockedReason.trim().isEmpty())) {
            throw new IllegalArgumentException("Bắt buộc phải ghi rõ lý do khi khoá tài khoản.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldStatus = null;
                String oldReason = null;
                String email = null;
                String fullName = null;

                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT email, full_name, status, locked_reason FROM users WHERE id = ? FOR UPDATE")) {
                    stmt.setLong(1, targetId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.commit();
                            return null;
                        }
                        email = rs.getString("email");
                        fullName = rs.getString("full_name");
                        oldStatus = rs.getString("status");
                        oldReason = rs.getString("locked_reason");
                    }
                }

                // Cập nhật trạng thái
                if ("LOCKED".equals(normalizedStatus)) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "UPDATE users SET status = 'LOCKED', locked_reason = ?, updated_at = NOW() WHERE id = ?")) {
                        stmt.setString(1, lockedReason.trim());
                        stmt.setLong(2, targetId);
                        stmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "UPDATE users SET status = 'ACTIVE', locked_reason = NULL, failed_login_attempts = 0, locked_until = NULL, updated_at = NOW() WHERE id = ?")) {
                        stmt.setLong(1, targetId);
                        stmt.executeUpdate();
                    }
                }

                // Ghi nhật ký audit_logs
                String action = "LOCKED".equals(normalizedStatus) ? "LOCK_USER" : "UNLOCK_USER";
                recordAuditLog(connection, actorId, action, "USER", String.valueOf(targetId),
                        "{\"status\":\"" + oldStatus + "\"}",
                        "{\"status\":\"" + normalizedStatus + "\",\"locked_reason\":\"" + (lockedReason != null ? lockedReason.trim() : "") + "\"}");

                // Kiểm tra lớp học phụ trách cần bàn giao
                List<Map<String, Object>> assignedClasses = findAssignedClasses(connection, targetId);

                connection.commit();

                // Thu hồi tất cả phiên khi khóa
                int revokedSessions = 0;
                if ("LOCKED".equals(normalizedStatus)) {
                    revokedSessions = com.ems.config.SessionBlacklist.revokeOtherSessions(targetId, null);
                }

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", targetId);
                result.put("email", email);
                result.put("fullName", fullName);
                result.put("status", normalizedStatus);
                result.put("lockedReason", "LOCKED".equals(normalizedStatus) ? lockedReason.trim() : null);
                result.put("revokedSessionsCount", revokedSessions);
                result.put("assignedClasses", assignedClasses);
                result.put("requiresHandover", !assignedClasses.isEmpty());

                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public List<Map<String, Object>> findAssignedClasses(Connection connection, long userId) {
        List<Map<String, Object>> classes = new ArrayList<>();
        String sql = "SELECT DISTINCT c.id, c.class_code, c.class_name, c.status, 'Giảng viên chính' AS role_in_class "
                + "FROM classes c JOIN class_subjects cs ON cs.class_id = c.id "
                + "WHERE cs.primary_instructor_id = ? AND c.status IN ('PLANNING', 'IN_PROGRESS') "
                + "UNION "
                + "SELECT DISTINCT c.id, c.class_code, c.class_name, c.status, 'Trợ giảng' AS role_in_class "
                + "FROM classes c JOIN class_subjects cs ON cs.class_id = c.id "
                + "JOIN class_subject_assistants csa ON csa.class_subject_id = cs.id "
                + "WHERE csa.ta_user_id = ? AND c.status IN ('PLANNING', 'IN_PROGRESS')";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("classId", rs.getLong("id"));
                    item.put("classCode", rs.getString("class_code"));
                    item.put("className", rs.getString("class_name"));
                    item.put("status", rs.getString("status"));
                    item.put("roleInClass", rs.getString("role_in_class"));
                    classes.add(item);
                }
            }
        } catch (SQLException ignored) {
            // Khi chưa có dữ liệu bảng lớp, trả về rỗng an toàn
        }
        return classes;
    }

    private void recordAuditLog(Connection connection, long actorId, String action, String entityType,
                                String entityId, String oldVal, String newVal) {
        String sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, actorId);
            stmt.setString(2, action);
            stmt.setString(3, entityType);
            stmt.setString(4, entityId);
            stmt.setString(5, oldVal);
            stmt.setString(6, newVal);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
            // Bỏ qua lỗi phụ nếu bảng audit_logs chưa bật
        }
    }
}