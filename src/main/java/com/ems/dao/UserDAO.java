package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.User;
import org.mindrot.jbcrypt.BCrypt;
import com.ems.security.LoginAttemptPolicy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * Mục 12 & 13: JDBC & CRUD, JDBC Querying
 * Data Access Object (DAO) xử lý truy vấn dữ liệu bảng users và roles từ MySQL
 */
public class UserDAO {
    private static final String DUMMY_PASSWORD_HASH = BCrypt.hashpw(
            "constant-time-login-placeholder",
            BCrypt.gensalt(10)
    );

    /**
     * Xác thực thông tin đăng nhập bằng Email và Password
     */
    public User authenticate(String email, String password) throws SQLException {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedEmail.isEmpty() || password == null || password.isEmpty()) {
            BCrypt.checkpw(password == null ? "" : password, DUMMY_PASSWORD_HASH);
            return null;
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long userId;
                String userCode;
                String storedEmail;
                String passwordHash;
                String fullName;
                String phone;
                String status;
                int failures;
                Instant lockedUntil;
                String sql = "SELECT id, user_code, email, password_hash, full_name, phone, status, "
                        + "failed_login_attempts, locked_until FROM users WHERE email = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, normalizedEmail);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.commit();
                            BCrypt.checkpw(password, DUMMY_PASSWORD_HASH);
                            return null;
                        }
                        userId = result.getLong("id");
                        userCode = result.getString("user_code");
                        storedEmail = result.getString("email");
                        passwordHash = result.getString("password_hash");
                        fullName = result.getString("full_name");
                        phone = result.getString("phone");
                        status = result.getString("status");
                        failures = result.getInt("failed_login_attempts");
                        Timestamp lockTime = result.getTimestamp("locked_until");
                        lockedUntil = lockTime == null ? null : lockTime.toInstant();
                    }
                }

                Instant now = Instant.now();
                LoginAttemptPolicy.State lockState = new LoginAttemptPolicy.State(failures, lockedUntil);
                if (!"ACTIVE".equals(status) || LoginAttemptPolicy.isLocked(lockState, now)) {
                    BCrypt.checkpw(password, DUMMY_PASSWORD_HASH);
                    connection.commit();
                    return null;
                }

                boolean passwordMatches = isValidBcryptHash(passwordHash)
                        && BCrypt.checkpw(password, passwordHash);
                if (!passwordMatches) {
                    LoginAttemptPolicy.State next = LoginAttemptPolicy.recordFailure(lockState, now);
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE users SET failed_login_attempts = ?, locked_until = ? WHERE id = ?")) {
                        statement.setInt(1, next.failures());
                        statement.setTimestamp(2, next.lockedUntil() == null ? null : Timestamp.from(next.lockedUntil()));
                        statement.setLong(3, userId);
                        statement.executeUpdate();
                    }
                    connection.commit();
                    return null;
                }

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?")) {
                    statement.setLong(1, userId);
                    statement.executeUpdate();
                }

                List<String> roles = new ArrayList<>();
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                                + "WHERE ur.user_id = ? ORDER BY CASE r.role_code WHEN 'ADMIN' THEN 0 ELSE 1 END, r.id")) {
                    statement.setLong(1, userId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) roles.add(result.getString("role_code"));
                    }
                }
                if (roles.isEmpty()) {
                    connection.commit();
                    return null;
                }

                User user = new User();
                user.setId(userId);
                user.setUserCode(userCode);
                user.setEmail(storedEmail);
                user.setFullName(fullName);
                user.setPhone(phone);
                user.setStatus(status);
                user.setRoles(roles);
                user.setPrimaryRole(roles.get(0));
                connection.commit();
                return user;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    /**
     * Chức năng S1-04: Đổi mật khẩu khi đang đăng nhập
     * Ràng buộc:
     * - Mật khẩu hiện tại phải khớp BCrypt hash
     * - Mật khẩu mới tối thiểu 8 ký tự, có cả chữ và số
     * - Mật khẩu xác nhận phải khớp mật khẩu mới
     * - Mật khẩu mới không được trùng với mật khẩu cũ
     * - Mã hóa BCrypt trước khi cập nhật
     */
    public boolean changePassword(long userId, String currentPassword, String newPassword, String confirmPassword)
            throws SQLException {
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Mật khẩu mới phải có tối thiểu 8 ký tự.");
        }
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải bao gồm cả chữ cái và chữ số.");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Xác nhận mật khẩu mới không trùng khớp.");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String storedHash = null;
                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT password_hash FROM users WHERE id = ? FOR UPDATE")) {
                    stmt.setLong(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            storedHash = rs.getString("password_hash");
                        }
                    }
                }

                if (storedHash == null) {
                    connection.rollback();
                    throw new IllegalStateException("Không tìm thấy thông tin tài khoản.");
                }

                if (!isValidBcryptHash(storedHash) || !BCrypt.checkpw(currentPassword, storedHash)) {
                    connection.rollback();
                    throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
                }

                String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
                try (PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?")) {
                    updateStmt.setString(1, newHash);
                    updateStmt.setLong(2, userId);
                    updateStmt.executeUpdate();
                }

                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    public static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");

    /**
     * Kiểm tra định dạng số điện thoại Việt Nam (Sprint 2 S2-02 AC3)
     * - 10 chữ số, bắt đầu bằng các đầu số di động: 03, 05, 07, 08, 09 (hoặc +843, +845, +847, +848, +849)
     */
    public static boolean isValidVietnamPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        String normalized = phone.trim().replaceAll("[\\s.-]", "");
        return VN_PHONE_PATTERN.matcher(normalized).matches();
    }

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng (Sprint 2 S2-02)
     */
    public Map<String, Object> getUserProfile(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String sql = "SELECT id, user_code, email, full_name, phone, date_of_birth, gender, address, avatar_url, status, created_at "
                    + "FROM users WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return null;
                    return mapUserProfile(connection, rs);
                }
            }
        }
    }

    /**
     * Lấy thông tin hồ sơ theo Email (hỗ trợ kiểm thử và session)
     */
    public Map<String, Object> getUserProfileByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) return null;
        try (Connection connection = DBConnection.getConnection()) {
            String sql = "SELECT id, user_code, email, full_name, phone, date_of_birth, gender, address, avatar_url, status, created_at "
                    + "FROM users WHERE email = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, email.trim().toLowerCase(java.util.Locale.ROOT));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) return null;
                    return mapUserProfile(connection, rs);
                }
            }
        }
    }

    private Map<String, Object> mapUserProfile(Connection connection, ResultSet rs) throws SQLException {
        long userId = rs.getLong("id");
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", userId);
        profile.put("userCode", rs.getString("user_code"));
        profile.put("email", rs.getString("email"));
        profile.put("fullName", rs.getString("full_name"));
        profile.put("phone", rs.getString("phone") != null ? rs.getString("phone") : "");
        profile.put("dateOfBirth", rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toString() : "");
        profile.put("gender", rs.getString("gender") != null ? rs.getString("gender") : "OTHER");
        profile.put("address", rs.getString("address") != null ? rs.getString("address") : "");
        profile.put("avatarUrl", rs.getString("avatar_url") != null ? rs.getString("avatar_url") : "");
        profile.put("status", rs.getString("status"));
        profile.put("createdAt", rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "");

        // Lấy danh sách vai trò
        List<String> roles = new ArrayList<>();
        String roleSql = "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?";
        try (PreparedStatement roleStmt = connection.prepareStatement(roleSql)) {
            roleStmt.setLong(1, userId);
            try (ResultSet roleRs = roleStmt.executeQuery()) {
                while (roleRs.next()) {
                    roles.add(roleRs.getString("role_code"));
                }
            }
        }
        profile.put("roles", roles);
        profile.put("primaryRole", roles.isEmpty() ? "STUDENT" : roles.get(0));
        return profile;
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân (Sprint 2 S2-02)
     * - Sửa họ tên, số điện thoại, ngày sinh, địa chỉ, giới tính
     * - Tuyệt đối không cho phép đổi email và vai trò
     * - Bắt buộc kiểm tra định dạng số điện thoại Việt Nam
     */
    public Map<String, Object> updateUserProfile(long userId, String fullName, String phone,
                                                String dateOfBirth, String gender, String address) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }

        String normalizedPhone = null;
        if (phone != null && !phone.trim().isEmpty()) {
            normalizedPhone = phone.trim().replaceAll("[\\s.-]", "");
            if (!isValidVietnamPhone(normalizedPhone)) {
                throw new IllegalArgumentException("Số điện thoại không đúng định dạng di động Việt Nam (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09).");
            }
        }

        String normalizedGender = "OTHER";
        if (gender != null && !gender.trim().isEmpty()) {
            String g = gender.trim().toUpperCase(java.util.Locale.ROOT);
            if ("MALE".equals(g) || "FEMALE".equals(g) || "OTHER".equals(g)) {
                normalizedGender = g;
            }
        }

        java.sql.Date dobDate = null;
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                dobDate = java.sql.Date.valueOf(dateOfBirth.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Định dạng ngày sinh không hợp lệ (yêu cầu YYYY-MM-DD).");
            }
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String oldValuesJson = null;
                try (PreparedStatement selectStmt = connection.prepareStatement(
                        "SELECT full_name, phone, date_of_birth, gender, address FROM users WHERE id = ? FOR UPDATE")) {
                    selectStmt.setLong(1, userId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            connection.commit();
                            throw new IllegalStateException("Không tìm thấy người dùng.");
                        }
                        oldValuesJson = String.format("{\"full_name\":\"%s\",\"phone\":\"%s\",\"gender\":\"%s\"}",
                                rs.getString("full_name"), rs.getString("phone"), rs.getString("gender"));
                    }
                }

                String updateSql = "UPDATE users SET full_name = ?, phone = ?, date_of_birth = ?, gender = ?, address = ?, updated_at = NOW() "
                        + "WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setString(1, fullName.trim());
                    updateStmt.setString(2, normalizedPhone);
                    updateStmt.setDate(3, dobDate);
                    updateStmt.setString(4, normalizedGender);
                    updateStmt.setString(5, address != null ? address.trim() : null);
                    updateStmt.setLong(6, userId);
                    updateStmt.executeUpdate();
                }

                String newValuesJson = String.format("{\"full_name\":\"%s\",\"phone\":\"%s\",\"gender\":\"%s\"}",
                        fullName.trim(), normalizedPhone, normalizedGender);

                try (PreparedStatement auditStmt = connection.prepareStatement(
                        "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values, created_at) "
                                + "VALUES (?, 'UPDATE_PROFILE', 'USER', ?, ?, ?, NOW())")) {
                    auditStmt.setLong(1, userId);
                    auditStmt.setString(2, String.valueOf(userId));
                    auditStmt.setString(3, oldValuesJson);
                    auditStmt.setString(4, newValuesJson);
                    auditStmt.executeUpdate();
                }

                connection.commit();
                return getUserProfile(userId);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private boolean isValidBcryptHash(String hash) {
        if (hash == null || !hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) return false;
        return true;
    }
}
