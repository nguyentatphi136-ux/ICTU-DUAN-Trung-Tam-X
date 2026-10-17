package com.ems.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tiện ích khởi tạo CSDL In-Memory H2 (chế độ tương thích MySQL) phục vụ Integration Test
 */
public class TestDbHelper {

    private static final String H2_URL = "jdbc:h2:mem:ems_test_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
    }

    public static void initDatabase() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP ALL OBJECTS");

            // 1. Programs table
            stmt.execute("""
                CREATE TABLE programs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    program_code VARCHAR(50) NOT NULL UNIQUE,
                    program_name VARCHAR(150) NOT NULL,
                    description TEXT NULL,
                    duration_months INT NOT NULL DEFAULT 6,
                    standard_tuition DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // 2. Subjects table
            stmt.execute("""
                CREATE TABLE subjects (
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

            // 3. Program Subjects table
            stmt.execute("""
                CREATE TABLE program_subjects (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    program_id INT NOT NULL,
                    subject_id INT NOT NULL,
                    order_index INT NOT NULL DEFAULT 1,
                    prerequisite_subject_id INT NULL,
                    CONSTRAINT uk_program_subject UNIQUE (program_id, subject_id)
                )
            """);

            // 4. Classes table
            stmt.execute("""
                CREATE TABLE classes (
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

            // 5. Class Subjects table
            stmt.execute("""
                CREATE TABLE class_subjects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    subject_id INT NOT NULL,
                    order_index INT NOT NULL DEFAULT 1,
                    primary_instructor_id BIGINT NULL,
                    status VARCHAR(20) DEFAULT 'NOT_STARTED',
                    CONSTRAINT uk_class_subject UNIQUE (class_id, subject_id)
                )
            """);

            // Seed sample programs & subjects
            // Program 1: Có 3 môn học (CS101, DB101, JAVA101)
            stmt.execute("INSERT INTO programs (id, program_code, program_name, status) VALUES (1, 'CNTT', 'Lập trình viên Quốc tế', 'ACTIVE')");
            // Program 2: Chương trình chưa có môn học nào (để test nghiệp vụ)
            stmt.execute("INSERT INTO programs (id, program_code, program_name, status) VALUES (2, 'EMPTY_PROG', 'Chương trình Trống', 'ACTIVE')");
            // Program 3: Chương trình đã ngưng hoạt động
            stmt.execute("INSERT INTO programs (id, program_code, program_name, status) VALUES (3, 'INACTIVE_PROG', 'Chương trình Tạm dừng', 'INACTIVE')");

            stmt.execute("INSERT INTO subjects (id, subject_code, subject_name, status) VALUES (1, 'CS101', 'Nhập môn lập trình', 'ACTIVE')");
            stmt.execute("INSERT INTO subjects (id, subject_code, subject_name, status) VALUES (2, 'DB101', 'Cơ sở dữ liệu MySQL', 'ACTIVE')");
            stmt.execute("INSERT INTO subjects (id, subject_code, subject_name, status) VALUES (3, 'JAVA101', 'Lập trình Java Web', 'ACTIVE')");

            stmt.execute("INSERT INTO program_subjects (program_id, subject_id, order_index) VALUES (1, 1, 1)");
            stmt.execute("INSERT INTO program_subjects (program_id, subject_id, order_index) VALUES (1, 2, 2)");
            stmt.execute("INSERT INTO program_subjects (program_id, subject_id, order_index) VALUES (1, 3, 3)");
        }
    }
}
