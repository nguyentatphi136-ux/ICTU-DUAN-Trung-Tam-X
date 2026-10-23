package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dao.AdminDAO;
import com.ems.dao.UserDAO;
import com.ems.model.User;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** S1-08: mật khẩu tạm <tên đăng nhập>@<ngày sinh ddMMyy>, phải đổi ở lần đăng nhập đầu; họ tên gốc giữ ở displayName. */
class AccountTempPasswordTest {

    @Test
    void newAccountGetsTempPasswordFromBirthDateAndMustChangeIt() throws Exception {
        assertEquals("A25200212070@170605", AdminDAO.tempPasswordFor("A25200212070@tms.vn", "2005-06-17"));
        assertTrue(AdminDAO.tempPasswordFor("x@tms.vn", null).startsWith("Edu@"), "Chưa có ngày sinh thì mật khẩu ngẫu nhiên");

        TestDatabaseHelper.initTestDatabase();
        TestDatabaseHelper.clearData();
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO roles (id, role_code, role_name) VALUES (1, 'STUDENT', 'Học viên')");
        }
        Map<String, Object> created = new AdminDAO().createUser(0, null, "A25200212070@tms.vn", "Chu Hải Đăng", "0911222333",
                List.of("STUDENT"), null, "2005-06-17", "Thái Bình", "Số 1 Cầu Giấy, Hà Nội");
        assertEquals("A25200212070@170605", created.get("tempPassword"));
        assertEquals("CHU HAI DANG", created.get("fullName"));

        UserDAO users = new UserDAO();
        Map<String, Object> detail = users.getUserProfile((Long) created.get("id"));
        assertEquals("Chu Hải Đăng", detail.get("displayName"));
        assertEquals("Thái Bình", detail.get("hometown"));
        assertEquals("2005-06-17", detail.get("dateOfBirth"));

        User first = users.authenticate("a25200212070@tms.vn", "A25200212070@170605");
        assertNotNull(first, "Đăng nhập không phân biệt hoa thường");
        assertTrue(first.isMustChangePassword(), "Mật khẩu tạm phải đổi ở lần đăng nhập đầu");
        assertTrue(users.changePassword(first.getId(), "A25200212070@170605", "MatKhauMoi9", "MatKhauMoi9"));
        assertFalse(users.authenticate("A25200212070@tms.vn", "MatKhauMoi9").isMustChangePassword());
    }
}
