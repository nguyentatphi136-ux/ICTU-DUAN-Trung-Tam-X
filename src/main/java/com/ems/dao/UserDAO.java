package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.exception.ConflictException;
import com.ems.model.User;
import com.google.gson.Gson;
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
                        + "failed_login_attempts, locked_until FROM users WHERE email = ? AND deleted_at IS NULL FOR UPDATE";
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

    public User findById(long userId) throws SQLException {
        String sql = "SELECT id, user_code, email, full_name, phone, status FROM users WHERE id = ? AND deleted_at IS NULL";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return null;
                User user = new User();
                user.setId(result.getLong("id"));
                user.setUserCode(result.getString("user_code"));
                user.setEmail(result.getString("email"));
                user.setFullName(result.getString("full_name"));
                user.setPhone(result.getString("phone"));
                user.setStatus(result.getString("status"));
                List<String> roles = new ArrayList<>();
                try (PreparedStatement roleStmt = connection.prepareStatement(
                        "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                                + "WHERE ur.user_id = ? ORDER BY CASE r.role_code WHEN 'ADMIN' THEN 0 ELSE 1 END, r.id")) {
                    roleStmt.setLong(1, userId);
                    try (ResultSet roleRs = roleStmt.executeQuery()) {
                        while (roleRs.next()) roles.add(roleRs.getString("role_code"));
                    }
                }
                user.setRoles(roles);
                if (!roles.isEmpty()) user.setPrimaryRole(roles.get(0));
                return user;
            }
        }
    }

    public static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)[0-9]{8}$");
    private static final Gson GSON = new Gson();

    /**
     * Kiểm tra định dạng số điện thoại Việt Nam (S2-02 AC3): 10 chữ số, đầu số 03, 05, 07, 08, 09 (hoặc +84).
     * Số trống được coi là hợp lệ vì trường không bắt buộc.
     */
    public static boolean isValidVietnamPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        return VN_PHONE_PATTERN.matcher(normalizePhone(phone)).matches();
    }

    public static String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("[\\s.-]", "");
    }

    /**
     * Tìm tài khoản khác đang dùng số điện thoại này (bỏ qua tài khoản excludeId và tài khoản đã vào thùng rác).
     * Trả về họ tên người đang dùng, null nếu số chưa ai dùng.
     */
    public static String findPhoneOwner(Connection connection, String phone, long excludeId) throws SQLException {
        String normalized = normalizePhone(phone);
        if (normalized.isEmpty()) return null;
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT full_name FROM users WHERE phone = ? AND id <> ? AND deleted_at IS NULL")) {
            stmt.setString(1, normalized);
            stmt.setLong(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("full_name") : null;
            }
        }
    }

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng (S2-02)
     */
    public Map<String, Object> getUserProfile(long userId) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            String sql = "SELECT id, user_code, email, full_name, phone, date_of_birth, gender, address, avatar_url, status, created_at "
                    + "FROM users WHERE id = ? AND deleted_at IS NULL";
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
     * Lấy thông tin hồ sơ theo email (dùng trong kiểm thử).
     */
    public Map<String, Object> getUserProfileByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) return null;
        try (Connection connection = DBConnection.getConnection()) {
            String sql = "SELECT id, user_code, email, full_name, phone, date_of_birth, gender, address, avatar_url, status, created_at "
                    + "FROM users WHERE email = ? AND deleted_at IS NULL";
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

        List<String> roles = new ArrayList<>();
        try (PreparedStatement roleStmt = connection.prepareStatement(
                "SELECT r.role_code FROM user_roles ur JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ?")) {
            roleStmt.setLong(1, userId);
            try (ResultSet roleRs = roleStmt.executeQuery()) {
                while (roleRs.next()) roles.add(roleRs.getString("role_code"));
            }
        }
        profile.put("roles", roles);
        profile.put("primaryRole", roles.isEmpty() ? "STUDENT" : roles.get(0));
        return profile;
    }

    /**
     * Cập nhật hồ sơ cá nhân (S2-02): họ tên, số điện thoại, ngày sinh, giới tính, địa chỉ.
     * Không đổi email và vai trò. Số điện thoại phải đúng định dạng và chưa được tài khoản khác dùng.
     *
     * @throws IllegalArgumentException dữ liệu không hợp lệ
     * @throws ConflictException        PHONE_DUPLICATE khi số điện thoại đã được dùng
     */
    public Map<String, Object> updateUserProfile(long userId, String fullName, String phone,
                                                String dateOfBirth, String gender, String address) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        String normalizedPhone = normalizePhone(phone);
        if (!isValidVietnamPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng di động Việt Nam (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09).");
        }
        String normalizedGender = "OTHER";
        if (gender != null) {
            String g = gender.trim().toUpperCase(java.util.Locale.ROOT);
            if ("MALE".equals(g) || "FEMALE".equals(g)) normalizedGender = g;
        }
        java.sql.Date dobDate = null;
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            try {
                dobDate = java.sql.Date.valueOf(dateOfBirth.trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Định dạng ngày sinh không hợp lệ (yêu cầu YYYY-MM-DD).");
            }
            if (dobDate.toLocalDate().isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("Ngày sinh không được ở tương lai.");
            }
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Map<String, Object> oldValues = new LinkedHashMap<>();
                try (PreparedStatement selectStmt = connection.prepareStatement(
                        "SELECT full_name, phone, gender, address FROM users WHERE id = ? AND deleted_at IS NULL FOR UPDATE")) {
                    selectStmt.setLong(1, userId);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalStateException("Không tìm thấy người dùng.");
                        }
                        oldValues.put("full_name", rs.getString("full_name"));
                        oldValues.put("phone", rs.getString("phone"));
                        oldValues.put("gender", rs.getString("gender"));
                        oldValues.put("address", rs.getString("address"));
                    }
                }

                String owner = findPhoneOwner(connection, normalizedPhone, userId);
                if (owner != null) {
                    throw new ConflictException("PHONE_DUPLICATE", "Số điện thoại đã được dùng cho tài khoản khác.");
                }

                try (PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE users SET full_name = ?, phone = ?, date_of_birth = ?, gender = ?, address = ?, updated_at = NOW() WHERE id = ?")) {
                    updateStmt.setString(1, fullName.trim());
                    updateStmt.setString(2, normalizedPhone.isEmpty() ? null : normalizedPhone);
                    updateStmt.setDate(3, dobDate);
                    updateStmt.setString(4, normalizedGender);
                    updateStmt.setString(5, address == null || address.isBlank() ? null : address.trim());
                    updateStmt.setLong(6, userId);
                    updateStmt.executeUpdate();
                }

                Map<String, Object> newValues = new LinkedHashMap<>();
                newValues.put("full_name", fullName.trim());
                newValues.put("phone", normalizedPhone);
                newValues.put("gender", normalizedGender);
                newValues.put("address", address == null ? null : address.trim());
                try (PreparedStatement auditStmt = connection.prepareStatement(
                        "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values) "
                                + "VALUES (?, 'UPDATE_PROFILE', 'USER', ?, ?, ?)")) {
                    auditStmt.setLong(1, userId);
                    auditStmt.setString(2, String.valueOf(userId));
                    auditStmt.setString(3, GSON.toJson(oldValues));
                    auditStmt.setString(4, GSON.toJson(newValues));
                    auditStmt.executeUpdate();
                }

                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
        return getUserProfile(userId);
    }

    private boolean isValidBcryptHash(String hash) {
        if (hash == null || !hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) return false;
        return true;
    }
}
