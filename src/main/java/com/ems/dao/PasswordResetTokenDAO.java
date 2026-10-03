package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.PasswordResetToken;
import com.ems.model.User;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Date;

/**
 * Mục 12 & 13: JDBC & CRUD, Transaction
 * DAO xử lý truy vấn bảng password_reset_tokens và cập nhật mật khẩu bảng users
 * Phục vụ: IDTTX-37, IDTTX-41, IDTTX-42
 */
public class PasswordResetTokenDAO {

    /**
     * Tìm người dùng theo Email (để phục vụ quên mật khẩu)
     */
    public User findUserByEmail(String email) {
        String sql = "SELECT id, user_code, email, full_name, status FROM users WHERE email = ? AND status = 'ACTIVE'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUserCode(rs.getString("user_code"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm kiếm user theo email: " + e.getMessage());
        }
        return null;
    }

    /**
     * Lưu Token đặt lại mật khẩu mới vào cơ sở dữ liệu (IDTTX-41)
     */
    public boolean saveToken(Long userId, String token, Timestamp expiresAt) {
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setString(2, hashToken(token));
            ps.setTimestamp(3, expiresAt);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi lưu password reset token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Tìm Token theo chuỗi token_hash (IDTTX-42)
     */
    public PasswordResetToken findByToken(String token) {
        String sql = "SELECT id, user_id, token_hash, expires_at, used_at, created_at " +
                     "FROM password_reset_tokens WHERE token_hash = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hashToken(token));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PasswordResetToken t = new PasswordResetToken();
                    t.setId(rs.getLong("id"));
                    t.setUserId(rs.getLong("user_id"));
                    t.setTokenHash(rs.getString("token_hash"));
                    t.setExpiresAt(rs.getTimestamp("expires_at"));
                    t.setUsedAt(rs.getTimestamp("used_at"));
                    t.setCreatedAt(rs.getTimestamp("created_at"));
                    return t;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm token: " + e.getMessage());
        }
        return null;
    }

    /**
     * Thực hiện đổi mật khẩu và đánh dấu token đã sử dụng trong 1 Transaction (IDTTX-42)
     * Đảm bảo tính toàn vẹn ACID (Mục 13: Transaction)
     */
    public boolean resetPasswordWithTransaction(Long userId, String token, String hashedPassword) {
        String updatePassSql = "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?";
        String findTokenSql = "SELECT id, user_id FROM password_reset_tokens "
            + "WHERE token_hash = ? AND expires_at > NOW() AND used_at IS NULL FOR UPDATE";
        String markTokenSql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE id = ? AND used_at IS NULL";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            long tokenId;
            long tokenUserId;
            try (PreparedStatement psToken = conn.prepareStatement(findTokenSql)) {
                psToken.setString(1, hashToken(token));
                try (ResultSet rs = psToken.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    tokenId = rs.getLong("id");
                    tokenUserId = rs.getLong("user_id");
                }
            }
            if (tokenUserId != userId) {
                conn.rollback();
                return false;
            }

            // 1. Cập nhật mật khẩu mới
            try (PreparedStatement psUser = conn.prepareStatement(updatePassSql)) {
                psUser.setString(1, hashedPassword);
                psUser.setLong(2, userId);
                if (psUser.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Đánh dấu token đã sử dụng (AC: chỉ dùng được 1 lần)
            try (PreparedStatement psToken = conn.prepareStatement(markTokenSql)) {
                psToken.setLong(1, tokenId);
                if (psToken.executeUpdate() != 1) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit(); // Commit Transaction thành công
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi transaction reset password: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback nếu có lỗi
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể bảo vệ token khôi phục mật khẩu.", exception);
        }
    }
}
