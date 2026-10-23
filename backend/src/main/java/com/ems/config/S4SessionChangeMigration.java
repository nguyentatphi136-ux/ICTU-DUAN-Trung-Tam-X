package com.ems.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bảng, cột S4-02 (kiểm tra trùng lịch) và S4-03 (sửa, dời, huỷ buổi) cần, tạo khi ứng dụng khởi động nên không
 * cần chạy tay file SQL. Tách khỏi DatabaseMigration để các nhánh S4 gộp không xung đột; không dùng khoá ngoại
 * để không phụ thuộc thứ tự chạy với AppStartupListener.
 */
@WebListener
public class S4SessionChangeMigration implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(S4SessionChangeMigration.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection conn = DBConnection.getConnection()) {
            run(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không tạo được bảng S4-02, S4-03: " + e.getMessage());
        }
    }

    public static void run(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_session_change_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id BIGINT NOT NULL,
                    action_type VARCHAR(20) NOT NULL,
                    performed_by BIGINT NOT NULL,
                    reason VARCHAR(255) NULL,
                    old_values LONGTEXT NULL,
                    new_values LONGTEXT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    recipient_id BIGINT NOT NULL,
                    sender_id BIGINT NULL,
                    title VARCHAR(255) NOT NULL,
                    content TEXT NOT NULL,
                    notification_type VARCHAR(20) DEFAULT 'SYSTEM',
                    link_url VARCHAR(255) NULL,
                    is_read BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            // Buổi tạo tay (S4-02) có thể chưa gắn môn; schema.sql và DatabaseMigration của S3 để NOT NULL.
            if (isNotNull(conn, "class_sessions", "class_subject_id")) {
                stmt.execute("ALTER TABLE class_sessions MODIFY COLUMN class_subject_id BIGINT NULL");
            }
        }
    }

    private static boolean isNotNull(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next() && rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
        }
    }
}
