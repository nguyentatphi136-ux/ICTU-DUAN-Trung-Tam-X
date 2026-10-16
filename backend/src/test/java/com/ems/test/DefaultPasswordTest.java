package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.config.DatabaseMigration;
import com.ems.dao.UserDAO;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/** Khởi động backend đặt mọi tài khoản về Tms@2026; đổi mật khẩu không được quay lại mật khẩu mặc định. */
class DefaultPasswordTest {

    @Test
    void startupResetsPasswordsAndBlocksReusingDefault() throws Exception {
        TestDatabaseHelper.initTestDatabase();
        TestDatabaseHelper.clearData();
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO roles (id, role_code, role_name) VALUES (1, 'ADMIN', 'Quản trị')");
            s.execute("INSERT INTO users (id, user_code, email, password_hash, full_name, failed_login_attempts) "
                    + "VALUES (1, 'U1', 'a@x.vn', 'hash-cu', 'A', 4)");
            s.execute("INSERT INTO user_roles (user_id, role_id) VALUES (1, 1)");
        }

        DatabaseMigration.runMigration();

        UserDAO dao = new UserDAO();
        assertNotNull(dao.authenticate("a@x.vn", UserDAO.DEFAULT_PASSWORD));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> dao.changePassword(1, UserDAO.DEFAULT_PASSWORD + "x1", UserDAO.DEFAULT_PASSWORD, UserDAO.DEFAULT_PASSWORD));
        assertTrue(e.getMessage().contains("mặc định"));
        assertTrue(dao.changePassword(1, UserDAO.DEFAULT_PASSWORD, "MatKhauMoi9", "MatKhauMoi9"));
        assertNull(dao.authenticate("a@x.vn", UserDAO.DEFAULT_PASSWORD));
        assertNotNull(dao.authenticate("a@x.vn", "MatKhauMoi9"));
    }
}
