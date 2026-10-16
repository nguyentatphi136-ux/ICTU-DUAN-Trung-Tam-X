package com.ems.config;

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
            // Chỉ máy nào tự bật trong backend/.env mới đặt lại mật khẩu; mặc định tắt để CSDL dùng chung không bị đổi.
            if ("true".equalsIgnoreCase(DBConnection.setting("RESET_PASSWORDS_ON_START", "false"))) {
                resetPasswords(conn);
            }
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

            // 4. Bảng của S3: nhật ký chăm sóc lead, học viên, phòng học, môn và phân công của lớp, ghi danh, buổi học.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lead_interactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    lead_id BIGINT NOT NULL,
                    counselor_id BIGINT NOT NULL,
                    interaction_type VARCHAR(20) NOT NULL,
                    content TEXT NOT NULL,
                    callback_scheduled_at DATETIME NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS students (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL UNIQUE,
                    student_code VARCHAR(50) NOT NULL UNIQUE,
                    entry_level VARCHAR(100) NULL,
                    enrollment_status VARCHAR(20) DEFAULT 'STUDYING',
                    notes TEXT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS classrooms (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    room_code VARCHAR(50) NOT NULL UNIQUE,
                    room_name VARCHAR(100) NOT NULL,
                    capacity INT NULL,
                    room_type VARCHAR(20) DEFAULT 'PHYSICAL',
                    meeting_url VARCHAR(255) NULL,
                    status VARCHAR(20) DEFAULT 'AVAILABLE'
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_subjects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    subject_id INT NOT NULL,
                    primary_instructor_id BIGINT NULL,
                    status VARCHAR(20) DEFAULT 'NOT_STARTED',
                    CONSTRAINT uk_class_subject UNIQUE (class_id, subject_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_subject_assistants (
                    class_subject_id BIGINT NOT NULL,
                    ta_user_id BIGINT NOT NULL,
                    PRIMARY KEY (class_subject_id, ta_user_id)
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    student_id BIGINT NOT NULL,
                    enrollment_date DATE NOT NULL,
                    status VARCHAR(20) DEFAULT 'ENROLLED',
                    notes VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_class_student UNIQUE (class_id, student_id)
                )
            """);
            // Lịch từng buổi thuộc S4; S3-07 chỉ đếm buổi đã học để tính tiến độ lớp.
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    class_subject_id BIGINT NOT NULL,
                    subject_lesson_id INT NULL,
                    session_number INT NOT NULL,
                    session_date DATE NOT NULL,
                    start_time TIME NOT NULL,
                    end_time TIME NOT NULL,
                    classroom_id INT NOT NULL,
                    instructor_id BIGINT NOT NULL,
                    status VARCHAR(20) DEFAULT 'SCHEDULED',
                    cancel_reason VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            LOGGER.info("Đã kiểm tra và đảm bảo cấu trúc bảng programs và classes thành công.");
        }

        // S3: cột bổ sung so với schema.sql cũ.
        ensureColumn(conn, "leads", "converted_student_id", "BIGINT NULL");
        ensureColumn(conn, "leads", "callback_date", "DATE NULL");
        ensureColumn(conn, "students", "program_id", "INT NULL");
        ensureColumn(conn, "students", "lead_id", "BIGINT NULL");
        ensureColumn(conn, "students", "contact_email", "VARCHAR(150) NULL");
        ensureColumn(conn, "students", "enrolled_at", "DATE NULL");
        ensureColumn(conn, "classrooms", "location", "VARCHAR(255) NULL");
        ensureColumn(conn, "class_subjects", "order_index", "INT NOT NULL DEFAULT 1");
        ensureColumn(conn, "enrollments", "enrolled_by", "BIGINT NULL");
        // Môn vừa sao chép khi mở lớp chưa có giảng viên; phòng trực tuyến có thể không giới hạn chỗ;
        // nhật ký chăm sóc ghi cả đổi giai đoạn và chuyển học viên nên loại tương tác không còn là ENUM bốn giá trị.
        relaxColumn(conn, "class_subjects", "primary_instructor_id", "BIGINT NULL");
        relaxColumn(conn, "classrooms", "capacity", "INT NULL");
        relaxColumn(conn, "lead_interactions", "interaction_type", "VARCHAR(20) NOT NULL");

        ensureColumn(conn, "subjects", "credit_weight", "DECIMAL(4, 2) NOT NULL DEFAULT 1.00");
        ensureColumn(conn, "subjects", "learning_outcomes", "TEXT NULL");
        ensureColumn(conn, "subjects", "status", "VARCHAR(20) DEFAULT 'ACTIVE'");
        seedLeadAssignPermission(conn);
        renameSeedEmails(conn);

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
     * Khi RESET_PASSWORDS_ON_START=true (backend/.env), mỗi lần backend khởi động mọi tài khoản về lại mật khẩu chung
     * UserDAO.DEFAULT_PASSWORD và mở khoá.
     * Đăng nhập bằng mật khẩu này thì bị buộc đổi mật khẩu (LoginApiServlet, SessionAuthFilter).
     * Chỉ gọi lúc khởi động, không gọi trong runMigration(conn) để kiểm thử giữ nguyên mật khẩu tự tạo.
     */
    static void resetPasswords(Connection conn) throws SQLException {
        if (!hasTable(conn, "users")) return;
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(
                "UPDATE users SET password_hash = ?, failed_login_attempts = 0, locked_until = NULL")) {
            stmt.setString(1, org.mindrot.jbcrypt.BCrypt.hashpw(com.ems.dao.UserDAO.DEFAULT_PASSWORD, org.mindrot.jbcrypt.BCrypt.gensalt(10)));
            LOGGER.info("Đã đặt lại mật khẩu " + stmt.executeUpdate() + " tài khoản về mật khẩu mặc định.");
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

    /** CSDL tạo từ schema.sql cũ có tài khoản mẫu @edumanager.vn; đổi sang email theo mã như frontend (A25200212001@tms.vn...). */
    static void renameSeedEmails(Connection conn) throws SQLException {
        if (!hasTable(conn, "users")) return;
        String[][] renames = {
                {"admin@edumanager.vn", "A25200212001@tms.vn"},
                {"quanlydaotao@edumanager.vn", "TM25200212002@tms.vn"},
                {"giangvien@edumanager.vn", "IST25200212045@tms.vn"},
                {"trogiang@edumanager.vn", "TA25200212007@tms.vn"},
                {"tuvan@edumanager.vn", "AMS25200212012@tms.vn"},
                {"ketoan@edumanager.vn", "ACT25200212003@tms.vn"},
                {"hocvien@edumanager.vn", "S25200212101@tms.vn"},
        };
        try (java.sql.PreparedStatement stmt = conn.prepareStatement("UPDATE users SET email = ? WHERE email = ?")) {
            for (String[] r : renames) {
                stmt.setString(1, r[1]);
                stmt.setString(2, r[0]);
                try {
                    stmt.executeUpdate();
                } catch (java.sql.SQLIntegrityConstraintViolationException duplicate) {
                    // Email mới đã có người dùng thì giữ nguyên tài khoản cũ.
                }
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

    /** Đổi định nghĩa cột khi cột đang NOT NULL mà definition cho phép NULL, hoặc cột đang là ENUM. */
    static void relaxColumn(Connection conn, String table, String column, String definition) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        boolean needed = false;
        for (String t : new String[] {table, table.toUpperCase(Locale.ROOT)}) {
            for (String c : new String[] {column, column.toUpperCase(Locale.ROOT)}) {
                try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, t, c)) {
                    if (rs.next()) {
                        needed = "ENUM".equalsIgnoreCase(rs.getString("TYPE_NAME"))
                                || "NO".equals(rs.getString("IS_NULLABLE")) && !definition.contains("NOT NULL");
                    }
                }
            }
        }
        if (!needed) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " " + definition);
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
