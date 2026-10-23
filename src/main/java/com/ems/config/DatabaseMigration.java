package com.ems.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
        S4AttendanceMigration.run(conn);
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

            // 3. Tạo bảng subjects (S2-05)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    subject_code VARCHAR(50) NOT NULL UNIQUE,
                    subject_name VARCHAR(150) NOT NULL,
                    total_sessions INT NOT NULL DEFAULT 15,
                    credit_weight DECIMAL(4, 2) NOT NULL DEFAULT 1.00,
                    min_pass_score DECIMAL(4, 2) NOT NULL DEFAULT 5.00,
                    learning_outcomes TEXT NULL,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 4. Tạo bảng program_subjects (S2-06)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS program_subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    program_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    order_index INT NOT NULL DEFAULT 1,
                    prerequisite_subject_id INT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 5. Tạo bảng subject_lessons (S2-07)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subject_lessons (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    subject_id INT NOT NULL,
                    session_order INT NOT NULL,
                    topic VARCHAR(255) NOT NULL,
                    objectives TEXT NULL
                )
            """);

            // 6. Tạo bảng class_subjects (hỗ trợ kiểm tra ràng buộc lớp học)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_subjects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    subject_id INT NOT NULL,
                    status VARCHAR(20) DEFAULT 'PLANNING'
                )
            """);

            LOGGER.info("Đã kiểm tra và đảm bảo cấu trúc bảng programs, classes, subjects, program_subjects, subject_lessons thành công.");
        }
    }
}
