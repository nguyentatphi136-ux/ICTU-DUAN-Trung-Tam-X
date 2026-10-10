package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.exception.ConflictException;
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
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

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

        StringBuilder whereSql = new StringBuilder(" WHERE u.deleted_at IS NULL ");
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
        String normalizedPhone = UserDAO.normalizePhone(phone);
        if (!UserDAO.isValidVietnamPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng di động Việt Nam (10 số, bắt đầu 03, 05, 07, 08, 09).");
        }
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
                if (UserDAO.findPhoneOwner(connection, normalizedPhone, 0) != null) {
                    throw new ConflictException("PHONE_DUPLICATE", "Số điện thoại đã được dùng cho tài khoản khác.");
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
                    insertStmt.setString(5, normalizedPhone.isEmpty() ? null : normalizedPhone);
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
                        null, GSON.toJson(Map.of("email", normalizedEmail, "full_name", fullName.trim())));

                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", newUserId);
                result.put("userCode", finalUserCode);
                result.put("email", normalizedEmail);
                result.put("fullName", fullName.trim());
                result.put("phone", normalizedPhone);
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
        String normalizedPhone = UserDAO.normalizePhone(phone);
        if (!UserDAO.isValidVietnamPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng di động Việt Nam (10 số, bắt đầu 03, 05, 07, 08, 09).");
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldName = null;
                String oldPhone = null;
                try (PreparedStatement checkStmt = connection.prepareStatement(
                        "SELECT full_name, phone FROM users WHERE id = ? AND deleted_at IS NULL FOR UPDATE")) {
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

                if (UserDAO.findPhoneOwner(connection, normalizedPhone, targetId) != null) {
                    throw new ConflictException("PHONE_DUPLICATE", "Số điện thoại đã được dùng cho tài khoản khác.");
                }
                try (PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET full_name = ?, phone = ?, updated_at = NOW() WHERE id = ?")) {
                    updateStmt.setString(1, fullName.trim());
                    updateStmt.setString(2, normalizedPhone.isEmpty() ? null : normalizedPhone);
                    updateStmt.setLong(3, targetId);
                    updateStmt.executeUpdate();
                }

                recordAuditLog(connection, actorId, "UPDATE_USER", "USER", String.valueOf(targetId),
                        GSON.toJson(Map.of("full_name", java.util.Objects.toString(oldName, ""), "phone", java.util.Objects.toString(oldPhone, ""))),
                        GSON.toJson(Map.of("full_name", fullName.trim(), "phone", normalizedPhone)));

                connection.commit();

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", targetId);
                result.put("fullName", fullName.trim());
                result.put("phone", normalizedPhone);
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

    public static final java.util.regex.Pattern VN_PHONE_REGEX = UserDAO.VN_PHONE_PATTERN;
    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    /** Giới hạn số dòng mỗi lần nhập để một tệp lỗi không làm treo máy chủ. */
    public static final int MAX_IMPORT_ROWS = 5000;

    /** Tên vai trò trong tệp (tiếng Việt không dấu hoặc mã) đổi ra mã vai trò trong bảng roles. */
    private static final Map<String, String> ROLE_ALIASES = Map.ofEntries(
            Map.entry("HOCVIEN", "STUDENT"), Map.entry("STUDENT", "STUDENT"),
            Map.entry("GIANGVIEN", "INSTRUCTOR"), Map.entry("INSTRUCTOR", "INSTRUCTOR"),
            Map.entry("TROGIANG", "TA"), Map.entry("TA", "TA"),
            Map.entry("TUVAN", "ADMISSIONS"), Map.entry("TUVANTUYENSINH", "ADMISSIONS"), Map.entry("ADMISSIONS", "ADMISSIONS"),
            Map.entry("KETOAN", "ACCOUNTANT"), Map.entry("ACCOUNTANT", "ACCOUNTANT"),
            Map.entry("QUANLYDAOTAO", "TRAINING_MANAGER"), Map.entry("TRAININGMANAGER", "TRAINING_MANAGER"));

    /** Mã vai trò từ ô trong tệp; trống thì là học viên, không nhận ra thì null. Không cho nhập tài khoản quản trị. */
    static String roleCode(String raw) {
        if (raw == null || raw.isBlank()) return "STUDENT";
        String key = java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                .toUpperCase(java.util.Locale.ROOT).replaceAll("[^A-Z]", "");
        return ROLE_ALIASES.get(key);
    }

    static String genderCode(String raw) {
        if (raw == null) return "OTHER";
        String g = java.text.Normalizer.normalize(raw.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        if (g.equals("nam") || g.equals("male") || g.equals("m")) return "MALE";
        if (g.equals("nu") || g.equals("female") || g.equals("f")) return "FEMALE";
        return "OTHER";
    }

    /** Số 9 chữ số do Excel làm mất số 0 đầu (ô kiểu số) được thêm lại số 0. */
    public static String importPhone(Object raw) {
        String phone = UserDAO.normalizePhone(raw == null ? null : String.valueOf(raw));
        return phone.matches("^[35789]\\d{8}$") ? "0" + phone : phone;
    }

    private static String text(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v == null ? "" : String.valueOf(v).trim();
    }

    /**
     * Đánh giá một dòng nhập (S2-01). Kết quả status:
     * - error: thiếu hoặc sai định dạng (họ tên, email, số điện thoại, vai trò, ngày sinh)
     * - duplicate: email hoặc số điện thoại trùng trong tệp hoặc đã có trong hệ thống
     * - ok: nhập được
     * issues là danh sách {type: INVALID | DUPLICATE, message}.
     */
    private Map<String, Object> evaluateImportRow(Connection connection, Map<String, Object> raw, int rowIdx,
                                                  Set<String> seenEmails, Set<String> seenPhones) throws SQLException {
        String fullName = text(raw, "fullName");
        if (fullName.isEmpty()) fullName = text(raw, "name");
        String email = text(raw, "email").toLowerCase(java.util.Locale.ROOT);
        String phone = importPhone(raw.get("phone"));
        String roleRaw = text(raw, "role");
        String role = roleCode(roleRaw);
        String dob = text(raw, "dateOfBirth");

        List<Map<String, String>> issues = new ArrayList<>();
        java.util.function.BiConsumer<String, String> add = (type, message) -> {
            Map<String, String> issue = new LinkedHashMap<>();
            issue.put("type", type);
            issue.put("message", message);
            issues.add(issue);
        };

        if (fullName.isEmpty()) add.accept("INVALID", "Thiếu họ và tên");
        if (email.isEmpty()) {
            add.accept("INVALID", "Thiếu địa chỉ email");
        } else if (!email.matches(EMAIL_REGEX)) {
            add.accept("INVALID", "Định dạng email không hợp lệ");
        } else if (!seenEmails.add(email)) {
            add.accept("DUPLICATE", "Email bị trùng với một dòng khác trong tệp");
        } else {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT full_name, deleted_at FROM users WHERE email = ?")) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        add.accept("DUPLICATE", rs.getTimestamp("deleted_at") != null
                                ? "Email thuộc tài khoản đang trong thùng rác (" + rs.getString("full_name") + ")"
                                : "Email đã được dùng cho tài khoản " + rs.getString("full_name"));
                    }
                }
            }
        }
        if (!phone.isEmpty()) {
            if (!VN_PHONE_REGEX.matcher(phone).matches()) {
                add.accept("INVALID", "Số điện thoại không đúng chuẩn di động VN (10 số, bắt đầu 03, 05, 07, 08, 09)");
            } else if (!seenPhones.add(phone)) {
                add.accept("DUPLICATE", "Số điện thoại bị trùng với một dòng khác trong tệp");
            } else {
                String owner = UserDAO.findPhoneOwner(connection, phone, 0);
                if (owner != null) add.accept("DUPLICATE", "Số điện thoại đã được dùng cho tài khoản " + owner);
            }
        }
        if (role == null) add.accept("INVALID", "Vai trò \"" + roleRaw + "\" không có trong hệ thống");
        if (!dob.isEmpty()) {
            try {
                java.sql.Date.valueOf(dob);
            } catch (IllegalArgumentException e) {
                add.accept("INVALID", "Ngày sinh phải có dạng YYYY-MM-DD");
            }
        }

        boolean invalid = issues.stream().anyMatch(i -> "INVALID".equals(i.get("type")));
        String status = invalid ? "error" : issues.isEmpty() ? "ok" : "duplicate";

        Map<String, Object> eval = new LinkedHashMap<>();
        eval.put("rowIndex", rowIdx);
        eval.put("fullName", fullName);
        eval.put("email", email);
        eval.put("phone", phone);
        eval.put("role", role != null ? role : roleRaw);
        eval.put("dateOfBirth", dob);
        eval.put("gender", genderCode(text(raw, "gender")));
        eval.put("address", text(raw, "address"));
        eval.put("status", status);
        eval.put("issues", issues);
        // Giữ các trường cũ cho trang JSP nhập người dùng.
        List<String> messages = issues.stream().map(i -> i.get("message")).collect(Collectors.toList());
        eval.put("isValid", "ok".equals(status));
        eval.put("errors", messages);
        eval.put("errorMessage", String.join(", ", messages));
        return eval;
    }

    /**
     * S2-01 AC2: Xem trước, tách ba nhóm hợp lệ / trùng / lỗi trước khi nhập.
     */
    public Map<String, Object> previewUsersBatch(List<Map<String, Object>> rows) throws SQLException {
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("Tệp có " + rows.size() + " dòng, vượt giới hạn " + MAX_IMPORT_ROWS + " dòng mỗi lần nhập.");
        }
        List<Map<String, Object>> evaluated = new ArrayList<>();
        Set<String> seenEmails = new java.util.HashSet<>();
        Set<String> seenPhones = new java.util.HashSet<>();
        int ok = 0, duplicate = 0, error = 0;
        try (Connection connection = DBConnection.getConnection()) {
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> eval = evaluateImportRow(connection, rows.get(i), i + 1, seenEmails, seenPhones);
                switch ((String) eval.get("status")) {
                    case "ok" -> ok++;
                    case "duplicate" -> duplicate++;
                    default -> error++;
                }
                evaluated.add(eval);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRows", rows.size());
        result.put("validRows", ok);
        result.put("duplicateRows", duplicate);
        result.put("errorRows", error);
        result.put("rows", evaluated);
        return result;
    }

    /**
     * S2-01 AC3: Nhập các dòng hợp lệ, bỏ qua dòng trùng và dòng lỗi, trả báo cáo tổng kết.
     */
    public Map<String, Object> importUsersBatch(long actorId, String fileName, List<Map<String, Object>> rows) throws SQLException {
        if (fileName == null || fileName.isBlank()) fileName = "import_users.xlsx";
        if (rows.size() > MAX_IMPORT_ROWS) {
            throw new IllegalArgumentException("Tệp có " + rows.size() + " dòng, vượt giới hạn " + MAX_IMPORT_ROWS + " dòng mỗi lần nhập.");
        }
        long stamp = System.currentTimeMillis();
        String batchCode = "IMP-" + stamp;

        List<Map<String, Object>> successUsers = new ArrayList<>();
        List<Map<String, Object>> skipped = new ArrayList<>();
        Set<String> seenEmails = new java.util.HashSet<>();
        Set<String> seenPhones = new java.util.HashSet<>();
        int duplicateCount = 0;
        int errorCount = 0;

        try (Connection connection = DBConnection.getConnection()) {
            for (int i = 0; i < rows.size(); i++) {
                int rowIdx = i + 1;
                Map<String, Object> eval = evaluateImportRow(connection, rows.get(i), rowIdx, seenEmails, seenPhones);
                String status = (String) eval.get("status");
                if (!"ok".equals(status)) {
                    if ("duplicate".equals(status)) duplicateCount++; else errorCount++;
                    skipped.add(skippedRow(eval, "duplicate".equals(status) ? "DUPLICATE" : "INVALID",
                            (String) eval.get("errorMessage")));
                    continue;
                }

                String email = (String) eval.get("email");
                String phone = (String) eval.get("phone");
                String dob = (String) eval.get("dateOfBirth");
                // Mã tài khoản theo lô và số dòng nên không trùng giữa các dòng cùng lô.
                String userCode = "U" + (stamp % 100000000L) + "-" + rowIdx;
                String tempPass = generateTempPassword();
                try {
                    connection.setAutoCommit(false);
                    long newUserId = 0;
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "INSERT INTO users (user_code, email, password_hash, full_name, phone, date_of_birth, gender, address, status, created_at, updated_at) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())", PreparedStatement.RETURN_GENERATED_KEYS)) {
                        stmt.setString(1, userCode);
                        stmt.setString(2, email);
                        stmt.setString(3, BCrypt.hashpw(tempPass, BCrypt.gensalt(10)));
                        stmt.setString(4, (String) eval.get("fullName"));
                        stmt.setString(5, phone.isEmpty() ? null : phone);
                        stmt.setDate(6, dob.isEmpty() ? null : java.sql.Date.valueOf(dob));
                        stmt.setString(7, (String) eval.get("gender"));
                        String address = (String) eval.get("address");
                        stmt.setString(8, address.isEmpty() ? null : address);
                        stmt.executeUpdate();
                        try (ResultSet rs = stmt.getGeneratedKeys()) {
                            if (rs.next()) newUserId = rs.getLong(1);
                        }
                    }
                    List<Integer> roleIds = findRoleIds(connection, Set.of((String) eval.get("role")));
                    try (PreparedStatement roleStmt = connection.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
                        for (int roleId : roleIds) {
                            roleStmt.setLong(1, newUserId);
                            roleStmt.setInt(2, roleId);
                            roleStmt.executeUpdate();
                        }
                    }
                    connection.commit();

                    Map<String, Object> succ = new LinkedHashMap<>();
                    succ.put("id", newUserId);
                    succ.put("rowIndex", rowIdx);
                    succ.put("userCode", userCode);
                    succ.put("fullName", eval.get("fullName"));
                    succ.put("email", email);
                    succ.put("phone", phone);
                    succ.put("role", eval.get("role"));
                    succ.put("tempPassword", tempPass);
                    successUsers.add(succ);
                } catch (SQLException | RuntimeException ex) {
                    connection.rollback();
                    errorCount++;
                    skipped.add(skippedRow(eval, "INVALID", "Không lưu được dòng này: " + ex.getMessage()));
                } finally {
                    connection.setAutoCommit(true);
                }
            }

            try {
                long batchId = 0;
                try (PreparedStatement bStmt = connection.prepareStatement(
                        "INSERT INTO user_import_batches (batch_code, actor_id, file_name, total_rows, success_rows, failed_rows, summary_note, created_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW())", PreparedStatement.RETURN_GENERATED_KEYS)) {
                    bStmt.setString(1, batchCode);
                    bStmt.setObject(2, actorId > 0 ? actorId : null);
                    bStmt.setString(3, fileName);
                    bStmt.setInt(4, rows.size());
                    bStmt.setInt(5, successUsers.size());
                    bStmt.setInt(6, duplicateCount + errorCount);
                    bStmt.setString(7, String.format("Nhập %d/%d dòng, bỏ qua %d dòng trùng và %d dòng lỗi.",
                            successUsers.size(), rows.size(), duplicateCount, errorCount));
                    bStmt.executeUpdate();
                    try (ResultSet rs = bStmt.getGeneratedKeys()) {
                        if (rs.next()) batchId = rs.getLong(1);
                    }
                }
                if (batchId > 0 && !skipped.isEmpty()) {
                    try (PreparedStatement eStmt = connection.prepareStatement(
                            "INSERT INTO user_import_errors (batch_id, row_index, raw_data, error_reason, created_at) VALUES (?, ?, ?, ?, NOW())")) {
                        for (Map<String, Object> err : skipped) {
                            Map<String, Object> rawData = new LinkedHashMap<>();
                            rawData.put("email", err.get("email"));
                            rawData.put("name", err.get("fullName"));
                            eStmt.setLong(1, batchId);
                            eStmt.setInt(2, (int) err.get("rowIndex"));
                            eStmt.setString(3, GSON.toJson(rawData));
                            eStmt.setString(4, (String) err.get("reason"));
                            eStmt.addBatch();
                        }
                        eStmt.executeBatch();
                    }
                }
            } catch (SQLException ignored) {
                // Bảng lưu lịch sử nhập chưa có thì vẫn trả báo cáo cho người dùng.
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("batchCode", batchCode);
        summary.put("fileName", fileName);
        summary.put("totalRows", rows.size());
        summary.put("successRows", successUsers.size());
        summary.put("duplicateRows", duplicateCount);
        summary.put("errorRows", errorCount);
        summary.put("failedRows", duplicateCount + errorCount);
        summary.put("successUsers", successUsers);
        summary.put("errors", skipped);
        return summary;
    }

    private static Map<String, Object> skippedRow(Map<String, Object> eval, String type, String reason) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("rowIndex", eval.get("rowIndex"));
        err.put("fullName", eval.get("fullName"));
        err.put("email", eval.get("email"));
        err.put("phone", eval.get("phone"));
        err.put("type", type);
        err.put("reason", reason);
        return err;
    }

    /**
     * Chuyển tài khoản vào thùng rác (xoá mềm): gán deleted_at, thu hồi mọi phiên đăng nhập.
     * Không tự xoá chính mình. Trả null nếu không tìm thấy hoặc đã nằm trong thùng rác.
     */
    public Map<String, Object> trashUser(long actorId, long targetId) throws SQLException {
        if (actorId == targetId) {
            throw new IllegalArgumentException("Không thể chuyển tài khoản của chính mình vào thùng rác.");
        }
        try (Connection connection = DBConnection.getConnection()) {
            int updated;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET deleted_at = NOW(), deleted_by = ? WHERE id = ? AND deleted_at IS NULL")) {
                stmt.setLong(1, actorId);
                stmt.setLong(2, targetId);
                updated = stmt.executeUpdate();
            }
            if (updated == 0) return null;
            recordAuditLog(connection, actorId, "TRASH_USER", "USER", String.valueOf(targetId), null, null);
        }
        com.ems.config.SessionBlacklist.revokeOtherSessions(targetId, null);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", targetId);
        return result;
    }

    /** Tài khoản trong thùng rác, mới xoá trước. */
    public List<Map<String, Object>> listTrashedUsers() throws SQLException {
        List<Map<String, Object>> items = new ArrayList<>();
        String sql = "SELECT u.id, u.user_code, u.email, u.full_name, u.deleted_at, d.full_name AS deleted_by_name "
                + "FROM users u LEFT JOIN users d ON d.id = u.deleted_by "
                + "WHERE u.deleted_at IS NOT NULL ORDER BY u.deleted_at DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", rs.getLong("id"));
                item.put("userCode", rs.getString("user_code"));
                item.put("email", rs.getString("email"));
                item.put("fullName", rs.getString("full_name"));
                item.put("deletedAt", rs.getTimestamp("deleted_at").toInstant().toString());
                item.put("deletedBy", rs.getString("deleted_by_name"));
                items.add(item);
            }
        }
        return items;
    }

    /** Khôi phục tài khoản khỏi thùng rác. Trả null nếu tài khoản không nằm trong thùng rác. */
    public Map<String, Object> restoreUser(long actorId, long targetId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE users SET deleted_at = NULL, deleted_by = NULL WHERE id = ? AND deleted_at IS NOT NULL")) {
                stmt.setLong(1, targetId);
                if (stmt.executeUpdate() == 0) return null;
            }
            recordAuditLog(connection, actorId, "RESTORE_USER", "USER", String.valueOf(targetId), null, null);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", targetId);
        return result;
    }

    /**
     * Xoá vĩnh viễn tài khoản đang trong thùng rác. Không hoàn tác được.
     * Còn dữ liệu khác tham chiếu tới tài khoản (lớp học, điểm, nhật ký...) thì báo 409 và giữ nguyên.
     */
    public boolean purgeUser(long actorId, long targetId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT 1 FROM users WHERE id = ? AND deleted_at IS NOT NULL FOR UPDATE")) {
                    check.setLong(1, targetId);
                    try (ResultSet rs = check.executeQuery()) {
                        if (!rs.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                    stmt.setLong(1, targetId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    stmt.setLong(1, targetId);
                    stmt.executeUpdate();
                }
                recordAuditLog(connection, actorId, "PURGE_USER", "USER", String.valueOf(targetId), null, null);
                connection.commit();
                return true;
            } catch (java.sql.SQLIntegrityConstraintViolationException e) {
                connection.rollback();
                throw new ConflictException("USER_HAS_RELATED_DATA",
                        "Không xoá vĩnh viễn được vì tài khoản còn dữ liệu liên quan (lớp học, điểm, nhật ký...). Hãy để trong thùng rác.");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
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