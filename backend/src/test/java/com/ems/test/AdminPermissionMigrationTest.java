package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.config.DatabaseMigration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** CSDL cũ (ADMIN có mọi quyền): khởi động gán lại quyền mọi vai trò theo Product Backlog, chỉ một lần. */
class AdminPermissionMigrationTest {

    @Test
    void oldDatabaseGetsRolePermissionsFromBacklogOnce() throws Exception {
        TestDatabaseHelper.initTestDatabase();
        TestDatabaseHelper.clearData();
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO roles (id, role_code, role_name) VALUES (1, 'ADMIN', 'Quản trị'), (2, 'TRAINING_MANAGER', 'QLĐT'), "
                    + "(3, 'ACCOUNTANT', 'Kế toán')");
            s.execute("INSERT INTO permissions (id, permission_code, permission_name, module) VALUES "
                    + "(1, 'USER_READ', 'a', 'USER'), (2, 'PROGRAM_MANAGE', 'b', 'PROGRAM'), (3, 'LEAD_MANAGE', 'c', 'ADMISSIONS')");
            s.execute("INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 1), (1, 2), (1, 3), (2, 2), (2, 3)");

            DatabaseMigration.runMigration(c);
            Set<String> admin = perms(s, "ADMIN");
            assertTrue(admin.containsAll(Set.of("USER_READ", "TRASH_PURGE")), admin.toString());
            assertFalse(admin.contains("PROGRAM_MANAGE") || admin.contains("LEAD_MANAGE"), "Quản trị chỉ quản lý tài khoản: " + admin);
            Set<String> manager = perms(s, "TRAINING_MANAGER");
            assertTrue(manager.containsAll(Set.of("PROGRAM_MANAGE", "LEAD_READ")), manager.toString());
            assertFalse(manager.contains("LEAD_MANAGE"), "Quản lý đào tạo chỉ xem lead (sheet 2)");
            assertTrue(perms(s, "ACCOUNTANT").contains("LEAD_READ"));

            // Quản trị tự gán lại ở S1-05 thì lần khởi động sau không ghi đè.
            s.execute("INSERT INTO role_permissions (role_id, permission_id) VALUES (1, 2)");
            DatabaseMigration.runMigration(c);
            assertTrue(perms(s, "ADMIN").contains("PROGRAM_MANAGE"));
        }
    }

    private static Set<String> perms(Statement s, String role) throws Exception {
        Set<String> out = new HashSet<>();
        try (ResultSet rs = s.executeQuery("SELECT p.permission_code FROM role_permissions rp JOIN roles r ON r.id = rp.role_id "
                + "JOIN permissions p ON p.id = rp.permission_id WHERE r.role_code = '" + role + "'")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }
}
