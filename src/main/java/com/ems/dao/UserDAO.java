package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mục 12 & 13: JDBC & CRUD, JDBC Querying
 * Data Access Object (DAO) xử lý truy vấn dữ liệu bảng users và roles từ MySQL
 */
public class UserDAO {

    /**
     * Xác thực thông tin đăng nhập bằng Email và Password
     */
    public User authenticate(String email, String password) {
        String sql = "SELECT u.id, u.user_code, u.email, u.password_hash, u.full_name, u.phone, u.status, r.role_code " +
                     "FROM users u " +
                     "LEFT JOIN user_roles ur ON u.id = ur.user_id " +
                     "LEFT JOIN roles r ON ur.role_id = r.id " +
                     "WHERE u.email = ? AND u.status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // Kiểm tra mật khẩu (hỗ trợ cả mật khẩu hash BCrypt và mật khẩu plain text '123456' khi test)
                    boolean passwordMatches = false;
                    if (storedHash != null && storedHash.startsWith("$2a$")) {
                        try {
                            passwordMatches = BCrypt.checkpw(password, storedHash);
                        } catch (Exception ignored) {
                            passwordMatches = password.equals(storedHash);
                        }
                    } else {
                        passwordMatches = password.equals(storedHash);
                    }

                    if (passwordMatches) {
                        User user = new User();
                        user.setId(rs.getLong("id"));
                        user.setUserCode(rs.getString("user_code"));
                        user.setEmail(rs.getString("email"));
                        user.setFullName(rs.getString("full_name"));
                        user.setPhone(rs.getString("phone"));
                        user.setStatus(rs.getString("status"));
                        user.setPrimaryRole(rs.getString("role_code"));

                        List<String> roles = new ArrayList<>();
                        if (user.getPrimaryRole() != null) {
                            roles.add(user.getPrimaryRole());
                        }
                        // Lấy tiếp các role khác nếu có
                        while (rs.next()) {
                            String additionalRole = rs.getString("role_code");
                            if (additionalRole != null && !roles.contains(additionalRole)) {
                                roles.add(additionalRole);
                            }
                        }
                        user.setRoles(roles);
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi xác thực người dùng trong UserDAO: " + e.getMessage());
        }
        return null;
    }
}
