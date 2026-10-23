package com.ems.test;

import com.ems.dao.UserDAO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Họ tên người dùng lưu IN HOA, không dấu. */
class UserNameTest {
    @Test
    void normalizeName() {
        assertEquals("TRAN THI GIANG", UserDAO.normalizeName("Trần Thị Giảng"));
        assertEquals("DO DUC DUNG", UserDAO.normalizeName("  đỗ   Đức  Dũng "));
        assertEquals("NGUYEN VAN AN", UserDAO.normalizeName("NGUYEN VAN AN"));
        assertNull(UserDAO.normalizeName(null));
    }
}
