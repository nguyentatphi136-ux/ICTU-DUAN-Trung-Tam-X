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

    private boolean isValidBcryptHash(String hash) {
        if (hash == null || !hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")) return false;
        return true;
    }
}
