package com.ems.test;

import com.ems.config.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tiện ích khởi tạo CSDL H2 in-memory (MySQL mode) cho việc kiểm thử tự động
 */
public class TestDatabaseHelper {

    private static boolean initialized = false;

    public static synchronized void initTestDatabase() {
        System.setProperty("DB_URL", "jdbc:h2:mem:ems_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        System.setProperty("DB_USER", "sa");
        System.setProperty("DB_PASSWORD", "");

        if (!initialized) {
            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 1. Bảng programs
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

                // 2. Bảng classes
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

                // 3. Bảng roles & permissions cho RBAC
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS roles (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        role_code VARCHAR(50) NOT NULL UNIQUE,
                        role_name VARCHAR(100) NOT NULL,
                        description VARCHAR(255) NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS permissions (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        permission_code VARCHAR(100) NOT NULL UNIQUE,
                        permission_name VARCHAR(150) NOT NULL,
                        module VARCHAR(50) NOT NULL,
                        description VARCHAR(255) NULL
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS role_permissions (
                        role_id INT NOT NULL,
                        permission_id INT NOT NULL,
                        PRIMARY KEY (role_id, permission_id)
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_code VARCHAR(50) NOT NULL UNIQUE,
                        email VARCHAR(150) NOT NULL UNIQUE,
                        password_hash VARCHAR(255) NOT NULL,
                        full_name VARCHAR(100) NOT NULL,
                        phone VARCHAR(20) NULL,
                        status VARCHAR(20) DEFAULT 'ACTIVE'
                    )
                """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS user_roles (
                        user_id BIGINT NOT NULL,
                        role_id INT NOT NULL,
                        PRIMARY KEY (user_id, role_id)
                    )
                """);

                initialized = true;
            } catch (SQLException e) {
                throw new RuntimeException("Lỗi khởi tạo H2 test database", e);
            }
        }
    }

    public static void clearData() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM classes");
            stmt.execute("DELETE FROM programs");
            stmt.execute("DELETE FROM role_permissions");
            stmt.execute("DELETE FROM user_roles");
            stmt.execute("DELETE FROM permissions");
            stmt.execute("DELETE FROM roles");
            stmt.execute("DELETE FROM users");
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi dọn dẹp dữ liệu test", e);
        }
    }
}
