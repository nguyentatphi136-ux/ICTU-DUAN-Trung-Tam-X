package com.ems.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiện ích thực thi Migration tự động cho CSDL
 * Phục vụ User Story S2-04: Quản lý danh mục chương trình đào tạo
 */
public class DatabaseMigration {
    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());

    public static void runMigration() {
        try (Connection conn = DBConnection.getConnection()) {
            runMigration(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không thể kết nối CSDL để chạy migration: " + e.getMessage());
        }
    }

    public static void runMigration(Connection conn) throws SQLException {
        ensureSchema(conn);
    }

    /**
     * Tạo bảng programs và classes nếu chưa tồn tại
     */
    public static void ensureSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 1. Tạo bảng programs
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS programs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    program_code VARCHAR(50) NOT NULL UNIQUE,
                    program_name VARCHAR(150) NOT NULL,
                    description TEXT NULL,
                    duration INT NOT NULL DEFAULT 60,
                    standard_tuition DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 2. Tạo bảng classes
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS classes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_code VARCHAR(50) NOT NULL UNIQUE,
                    class_name VARCHAR(150) NOT NULL,
                    program_id INT NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NULL,
                    max_capacity INT NOT NULL DEFAULT 30,
                    default_classroom_id INT NULL,
                    study_mode VARCHAR(20) DEFAULT 'OFFLINE',
                    status VARCHAR(20) DEFAULT 'PLANNING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 3. Bảng của S2-05 đến S2-10: môn học, lộ trình, buổi học, lead, lịch sử phân công.
            // Cùng cột với database/schema.sql nhưng không khai báo khoá ngoại để chạy được trên CSDL đang dở dang.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    subject_code VARCHAR(50) NOT NULL UNIQUE,
                    subject_name VARCHAR(150) NOT NULL,
                    total_sessions INT NOT NULL DEFAULT 15
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS program_subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    program_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    order_index INT NOT NULL DEFAULT 1
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS program_subject_prereqs (
                    program_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    prereq_subject_id INT NOT NULL,
                    PRIMARY KEY (program_id, subject_id, prereq_subject_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subject_lessons (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    subject_id INT NOT NULL,
                    session_order INT NOT NULL,
                    topic VARCHAR(255) NOT NULL,
                    objectives TEXT NULL,
                    CONSTRAINT uk_subject_session UNIQUE (subject_id, session_order)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS leads (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    full_name VARCHAR(100) NOT NULL,
                    phone VARCHAR(20) NOT NULL,
                    email VARCHAR(150) NULL,
                    source VARCHAR(50) DEFAULT 'WEBSITE',
                    interested_program_id INT NULL,
                    assigned_counselor_id BIGINT NULL,
                    status VARCHAR(30) DEFAULT 'NEW',
                    reject_reason VARCHAR(255) NULL,
                    notes TEXT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lead_assignments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    lead_id BIGINT NOT NULL,
                    from_counselor_id BIGINT NULL,
                    to_counselor_id BIGINT NOT NULL,
                    assigned_by BIGINT NULL,
                    note VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            LOGGER.info("Đã kiểm tra và đảm bảo cấu trúc bảng programs và classes thành công.");
        }

        ensureColumn(conn, "subjects", "credit_weight", "DECIMAL(4, 2) NOT NULL DEFAULT 1.00");
        ensureColumn(conn, "subjects", "learning_outcomes", "TEXT NULL");
        ensureColumn(conn, "subjects", "status", "VARCHAR(20) DEFAULT 'ACTIVE'");
        seedLeadAssignPermission(conn);

        // schema.sql cũ đặt tên cột là duration_months, còn DAO và migration S2-04 dùng duration.
        if (hasColumn(conn, "programs", "duration_months") && !hasColumn(conn, "programs", "duration")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE programs RENAME COLUMN duration_months TO duration");
            }
        }

        // Xoá mềm: bản ghi bị xoá chỉ được gán deleted_at, nằm trong thùng rác cho tới khi khôi phục hoặc xoá vĩnh viễn.
        for (String table : new String[] {"users", "programs", "subjects", "leads"}) {
            ensureColumn(conn, table, "deleted_at", "DATETIME NULL");
            ensureColumn(conn, table, "deleted_by", "BIGINT NULL");
        }
    }

    /**
     * S2-10: quyền LEAD_ASSIGN (xem mọi lead, phân công, xoá) cho Quản trị và Quản lý đào tạo; Quản lý đào tạo được thêm
     * LEAD_MANAGE để mở danh sách lead. Chỉ gán ở lần đầu tạo quyền, để quản trị thu hồi ở S1-05 thì khởi động lại không gán lại.
     */
    static void seedLeadAssignPermission(Connection conn) throws SQLException {
        if (!hasTable(conn, "permissions") || !hasTable(conn, "roles") || !hasTable(conn, "role_permissions")) return;
        try (java.sql.PreparedStatement check = conn.prepareStatement("SELECT 1 FROM permissions WHERE permission_code = 'LEAD_ASSIGN'");
             ResultSet rs = check.executeQuery()) {
            if (rs.next()) return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO permissions (permission_code, permission_name, module, description) "
                    + "VALUES ('LEAD_ASSIGN', 'Phân công lead', 'ADMISSIONS', 'Xem mọi lead, phân công tư vấn viên và xoá lead')");
        }
        String grant = "INSERT INTO role_permissions (role_id, permission_id) "
                + "SELECT r.id, p.id FROM roles r, permissions p WHERE r.role_code = ? AND p.permission_code = ? "
                + "AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id)";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(grant)) {
            for (String[] g : new String[][] {{"ADMIN", "LEAD_ASSIGN"}, {"TRAINING_MANAGER", "LEAD_ASSIGN"}, {"TRAINING_MANAGER", "LEAD_MANAGE"}}) {
                stmt.setString(1, g[0]);
                stmt.setString(2, g[1]);
                stmt.executeUpdate();
            }
        }
    }

    /** Thêm cột nếu bảng có mà cột chưa có. Bảng chưa tồn tại thì bỏ qua. */
    static void ensureColumn(Connection conn, String table, String column, String definition) throws SQLException {
        if (!hasTable(conn, table) || hasColumn(conn, table, column)) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    static boolean hasTable(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        for (String name : new String[] {table, table.toUpperCase(Locale.ROOT)}) {
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, name, new String[] {"TABLE"})) {
                if (rs.next()) return true;
            }
        }
        return false;
    }

    static boolean hasColumn(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        for (String t : new String[] {table, table.toUpperCase(Locale.ROOT)}) {
            for (String c : new String[] {column, column.toUpperCase(Locale.ROOT)}) {
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, t, c)) {
                    if (rs.next()) return true;
                }
            }
        }
        return false;
    }
}
