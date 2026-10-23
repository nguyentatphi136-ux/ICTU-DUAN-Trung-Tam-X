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
 * Migration tự động khởi tạo bảng Biên lai thanh toán (payment_receipts)
 * và Bút toán điều chỉnh (payment_adjustments) phục vụ nghiệp vụ S7-03 (Jira EP-08).
 */
@WebListener
public class S7PaymentMigration implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(S7PaymentMigration.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection conn = DBConnection.getConnection()) {
            run(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không tạo được bảng thanh toán S7-03: " + e.getMessage());
        }
    }

    public static void run(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_receipts (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    receipt_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã biên lai: BL-2026-000001',
                    student_code VARCHAR(50) NOT NULL,
                    installment_order INT NOT NULL,
                    amount_paid DECIMAL(12, 2) NOT NULL,
                    payment_method VARCHAR(30) NOT NULL DEFAULT 'CASH',
                    payment_date DATE NOT NULL,
                    payment_time VARCHAR(10) NOT NULL,
                    collected_by_name VARCHAR(100) NOT NULL,
                    reference_code VARCHAR(100) NULL,
                    notes VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_pr_student (student_code),
                    INDEX idx_pr_date (payment_date)
                ) ENGINE=InnoDB;
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_adjustments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    adjustment_code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã bút toán: ĐC-2026-000001',
                    receipt_code VARCHAR(50) NOT NULL,
                    kind VARCHAR(50) NOT NULL COMMENT 'Giảm khoản thu, Tăng khoản thu, Huỷ toàn bộ',
                    delta_amount DECIMAL(12, 2) NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    adjusted_by_name VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_pa_receipt (receipt_code)
                ) ENGINE=InnoDB;
            """);
        }
    }
}
