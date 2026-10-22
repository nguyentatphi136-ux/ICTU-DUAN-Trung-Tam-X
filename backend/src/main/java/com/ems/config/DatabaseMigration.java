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

            // 3. Tạo bảng students (nếu chưa có)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    student_code VARCHAR(50) NOT NULL UNIQUE,
                    entry_level VARCHAR(100) NULL,
                    enrollment_status VARCHAR(50) DEFAULT 'STUDYING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 4. Tạo bảng enrollments (nếu chưa có)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    enrollment_date DATE NOT NULL,
                    status VARCHAR(50) DEFAULT 'ENROLLED',
                    notes VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 5. Bảng kế hoạch học phí sinh viên (Student Tuition Plans)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS student_tuition_plans (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    enrollment_id BIGINT NULL,
                    student_id BIGINT NOT NULL,
                    original_tuition DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    discount_type VARCHAR(20) DEFAULT 'NONE',
                    discount_value DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    discount_reason VARCHAR(255) NULL,
                    discount_approved_by BIGINT NULL,
                    final_payable_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    debt_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(50) DEFAULT 'UNPAID',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 6. Bảng các đợt thanh toán học phí (Tuition Installment Bills)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tuition_installment_bills (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    tuition_plan_id BIGINT NOT NULL,
                    installment_order INT NOT NULL,
                    due_date DATE NOT NULL,
                    expected_amount DECIMAL(12, 2) NOT NULL,
                    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(50) DEFAULT 'UNPAID',
                    last_reminder_sent_at TIMESTAMP NULL
                )
            """);

            // 7. Bảng biên lai thu học phí (Payment Receipts)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_receipts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    receipt_code VARCHAR(50) NOT NULL UNIQUE,
                    installment_bill_id BIGINT NOT NULL,
                    amount_paid DECIMAL(12, 2) NOT NULL,
                    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
                    payment_date DATE NOT NULL,
                    reference_code VARCHAR(100) NULL,
                    notes VARCHAR(255) NULL,
                    collected_by BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 8. Bảng bút toán điều chỉnh thu học phí (Payment Adjustments - S7-03)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_adjustments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    adjustment_code VARCHAR(50) NOT NULL UNIQUE,
                    payment_receipt_id BIGINT NOT NULL,
                    adjustment_type VARCHAR(50) NOT NULL,
                    delta_amount DECIMAL(12, 2) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    adjusted_by BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            LOGGER.info("Đã kiểm tra và đảm bảo cấu trúc bảng học phí, biên lai và điều chỉnh thành công.");
        }

        // schema.sql cũ đặt tên cột là duration_months, còn DAO và migration S2-04 dùng duration.
        if (hasColumn(conn, "programs", "duration_months") && !hasColumn(conn, "programs", "duration")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE programs RENAME COLUMN duration_months TO duration");
            }
        }

        ensureColumn(conn, "payment_receipts", "reference_code", "VARCHAR(100) NULL");

        // Xoá mềm: bản ghi bị xoá chỉ được gán deleted_at, nằm trong thùng rác cho tới khi khôi phục hoặc xoá vĩnh viễn.
        for (String table : new String[] {"users", "programs", "subjects", "leads"}) {
            ensureColumn(conn, table, "deleted_at", "DATETIME NULL");
            ensureColumn(conn, table, "deleted_by", "BIGINT NULL");
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
