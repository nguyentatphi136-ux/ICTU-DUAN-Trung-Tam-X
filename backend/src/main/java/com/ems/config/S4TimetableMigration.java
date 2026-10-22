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
 * Bảng của S4-01 (ngày nghỉ lễ) và S4-06 (nhật ký bảo lưu, chuyển lớp, thôi học), tạo khi ứng dụng khởi động
 * nên không cần chạy tay file SQL. Tách khỏi DatabaseMigration để các nhánh S4 gộp không xung đột.
 * Không dùng khoá ngoại để không phụ thuộc thứ tự chạy với AppStartupListener.
 */
@WebListener
public class S4TimetableMigration implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(S4TimetableMigration.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection conn = DBConnection.getConnection()) {
            run(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không tạo được bảng S4-01, S4-06: " + e.getMessage());
        }
    }

    public static void run(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS holidays (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    from_date DATE NOT NULL,
                    to_date DATE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS student_status_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    student_code VARCHAR(50) NOT NULL,
                    action_type VARCHAR(20) NOT NULL,
                    from_class VARCHAR(50) NULL,
                    to_class VARCHAR(50) NULL,
                    effective_date DATE NOT NULL,
                    reason VARCHAR(255) NOT NULL,
                    notes TEXT NULL,
                    actor_name VARCHAR(100) NOT NULL,
                    actor_id BIGINT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            // Lịch nghỉ lễ mặc định chỉ chèn khi bảng còn trống, quản lý đào tạo thêm tiếp trên màn hình S4-01.
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM holidays")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("""
                        INSERT INTO holidays (name, from_date, to_date) VALUES
                        ('Ngày Nhà giáo Việt Nam', '2026-11-20', '2026-11-20'),
                        ('Tết Dương lịch', '2027-01-01', '2027-01-01'),
                        ('Nghỉ Tết Nguyên đán', '2027-02-05', '2027-02-14'),
                        ('Giỗ Tổ Hùng Vương', '2027-04-26', '2027-04-26'),
                        ('Giải phóng miền Nam, Quốc tế Lao động', '2027-04-30', '2027-05-01')
                    """);
                }
            }
            // S4-01 xếp bài theo thứ tự môn của lớp; S3 cũng thêm cột này, chạy lại không sao.
            if (tableExists(conn, "class_subjects") && !columnExists(conn, "class_subjects", "order_index")) {
                stmt.execute("ALTER TABLE class_subjects ADD COLUMN order_index INT NOT NULL DEFAULT 1");
            }
        }
    }

    static boolean tableExists(Connection conn, String table) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, table, null)) {
            return rs.next();
        }
    }

    static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
