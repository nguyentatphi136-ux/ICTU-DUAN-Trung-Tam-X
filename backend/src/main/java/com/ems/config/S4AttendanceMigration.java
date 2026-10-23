package com.ems.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Migration tự động tạo bảng điểm danh (attendance) và nhật ký sửa điểm danh (attendance_audit_logs)
 * phục vụ chức năng S4-07 (Jira EP-05).
 */
@WebListener
public class S4AttendanceMigration implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(S4AttendanceMigration.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection conn = DBConnection.getConnection()) {
            run(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không tạo được bảng điểm danh S4-07: " + e.getMessage());
        }
    }

    public static void run(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT',
                    notes VARCHAR(255) NULL,
                    marked_by BIGINT NULL,
                    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_by BIGINT NULL,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_session_student UNIQUE (session_id, student_id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance_audit_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    student_code VARCHAR(50) NULL,
                    student_name VARCHAR(100) NULL,
                    actor_id BIGINT NULL,
                    actor_name VARCHAR(100) NOT NULL,
                    actor_role VARCHAR(50) NOT NULL,
                    old_status VARCHAR(50) NOT NULL,
                    new_status VARCHAR(50) NOT NULL,
                    reason VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }
    }
}
