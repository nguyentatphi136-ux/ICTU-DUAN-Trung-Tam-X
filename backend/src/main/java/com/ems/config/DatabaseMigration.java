package com.ems.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
        applyRolePermissions(conn);
        renameSeedEmails(conn);
        // S1-08: họ tên viết như người nhập, quê quán, cờ bắt đổi mật khẩu tạm ở lần đăng nhập đầu.
        ensureColumn(conn, "users", "display_name", "VARCHAR(150) NULL");
        ensureColumn(conn, "users", "hometown", "VARCHAR(255) NULL");
        ensureColumn(conn, "users", "must_change_password", "BOOLEAN NOT NULL DEFAULT FALSE");
        normalizeUserNames(conn);

        // schema.sql cũ đặt tên cột là duration_months, còn DAO và migration S2-04 dùng duration.
        if (hasColumn(conn, "programs", "duration_months") && !hasColumn(conn, "programs", "duration")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE programs RENAME COLUMN duration_months TO duration");
            }
        }
        // Bảng lead_assignments tạo từ migration S2-10/S2-11 cũ dùng previous_counselor_id, new_counselor_id, assigned_at;
        // LeadDAO và schema.sql dùng from_counselor_id, to_counselor_id, created_at (phân công và lịch sử chuyển giao S2-10).
        for (String[] r : new String[][] {{"previous_counselor_id", "from_counselor_id"}, {"new_counselor_id", "to_counselor_id"},
                {"assigned_at", "created_at"}}) {
            if (hasColumn(conn, "lead_assignments", r[0]) && !hasColumn(conn, "lead_assignments", r[1])) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("ALTER TABLE lead_assignments RENAME COLUMN " + r[0] + " TO " + r[1]);
                }
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

    private static final List<String> BASE = List.of("MENU_VIEW", "DASHBOARD_VIEW", "PROFILE_VIEW");

    /**
     * Quyền từng vai trò theo Product Backlog: sheet 2 "User Roles" (ma trận module x vai trò) và sheet 4 (vai trò của
     * từng story). Quản trị chỉ quản lý tài khoản (S1-05, S1-08, S1-09, S1-10, S2-01) và xoá vĩnh viễn (nhận xét mentor S2).
     * Quản lý đào tạo chỉ xem lead (R) nhưng phân công được (S2-10). Phải khớp database/schema.sql.
     */
    static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", with("USER_READ", "USER_CREATE", "USER_ROLE_ASSIGN", "ROLE_PERMISSION_READ", "ROLE_PERMISSION_UPDATE", "TRASH_PURGE"),
            "TRAINING_MANAGER", with("PROGRAM_MANAGE", "PROGRAM_READ", "STUDENT_READ", "STUDENT_WRITE", "CLASS_MANAGE", "CLASS_READ", "SCHEDULE_READ",
                    "LEAD_READ", "LEAD_ASSIGN", "ATTENDANCE_MANAGE", "ASSIGNMENT_READ", "GRADE_MANAGE", "GRADE_READ", "FINANCE_READ",
                    "REPORT_VIEW", "USER_READ"),
            "ADMISSIONS", with("LEAD_MANAGE", "PROGRAM_READ", "STUDENT_READ", "STUDENT_WRITE", "CLASS_READ", "SCHEDULE_READ", "FINANCE_READ", "REPORT_VIEW"),
            "INSTRUCTOR", with("PROGRAM_READ", "STUDENT_READ", "CLASS_READ", "SCHEDULE_READ", "ATTENDANCE_MANAGE", "ASSIGNMENT_MANAGE",
                    "GRADE_MANAGE", "GRADE_READ", "REPORT_VIEW"),
            "TA", with("PROGRAM_READ", "STUDENT_READ", "CLASS_READ", "SCHEDULE_READ", "ATTENDANCE_MANAGE", "ASSIGNMENT_MANAGE", "GRADE_READ"),
            "ACCOUNTANT", with("LEAD_READ", "STUDENT_READ", "FINANCE_MANAGE", "FINANCE_READ", "REPORT_VIEW"),
            "STUDENT", with("PROGRAM_READ", "SCHEDULE_READ", "ASSIGNMENT_READ", "GRADE_READ", "FINANCE_READ"),
            "GUEST", List.of("CONSULTATION_CREATE"));

    /** Quyền thêm sau schema.sql đầu tiên: CSDL cũ chưa có thì tạo. { mã, tên, module, mô tả } */
    private static final String[][] NEW_PERMISSIONS = {
            {"LEAD_ASSIGN", "Phân công lead", "ADMISSIONS", "Xem mọi lead, phân công tư vấn viên và xoá lead"},
            {"LEAD_READ", "Xem lead", "ADMISSIONS", "Xem danh sách lead, phễu và nhật ký chăm sóc"},
            {"PROGRAM_READ", "Xem chương trình", "PROGRAM", "Xem chương trình, môn học, lộ trình"},
            {"STUDENT_READ", "Xem hồ sơ học viên", "CLASS", "Xem danh sách và hồ sơ học viên"},
            {"CLASS_READ", "Xem lớp học", "CLASS", "Xem lớp, ghi danh, người dạy và phòng học"},
            {"STUDENT_WRITE", "Sửa hồ sơ học viên", "CLASS", "Sửa thông tin cá nhân, trình độ đầu vào, ghi chú của học viên"},
            {"TRASH_PURGE", "Xoá vĩnh viễn", "SYSTEM", "Xem thùng rác các module và xoá vĩnh viễn"}};

    private static List<String> with(String... extra) {
        List<String> all = new ArrayList<>(BASE);
        all.addAll(List.of(extra));
        return all;
    }

    /**
     * CSDL tạo từ schema.sql cũ (chưa có CLASS_READ): gán lại quyền mọi vai trò đúng ROLE_PERMISSIONS, một lần.
     * CSDL đã cập nhật: chỉ tạo quyền mới trong NEW_PERMISSIONS và gán cho các vai trò có quyền đó trong ROLE_PERMISSIONS.
     * Quản trị chỉnh ở S1-05 thì khởi động lại không bị ghi đè.
     */
    static void applyRolePermissions(Connection conn) throws SQLException {
        if (!hasTable(conn, "permissions") || !hasTable(conn, "roles") || !hasTable(conn, "role_permissions")) return;
        Set<String> existing = new HashSet<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT permission_code FROM permissions")) {
            while (rs.next()) existing.add(rs.getString(1));
        }
        boolean oldDatabase = !existing.contains("CLASS_READ");
        Set<String> added = new HashSet<>();
        try (PreparedStatement add = conn.prepareStatement(
                "INSERT INTO permissions (permission_code, permission_name, module, description) VALUES (?, ?, ?, ?)")) {
            for (String[] p : NEW_PERMISSIONS) {
                if (existing.contains(p[0])) continue;
                for (int i = 0; i < 4; i++) add.setString(i + 1, p[i]);
                add.executeUpdate();
                added.add(p[0]);
            }
        }
        if (!oldDatabase) {
            try (PreparedStatement grant = conn.prepareStatement("INSERT INTO role_permissions (role_id, permission_id) "
                    + "SELECT r.id, p.id FROM roles r, permissions p WHERE r.role_code = ? AND p.permission_code = ?")) {
                for (Map.Entry<String, List<String>> role : ROLE_PERMISSIONS.entrySet()) {
                    for (String permission : role.getValue()) {
                        if (!added.contains(permission)) continue;
                        grant.setString(1, role.getKey());
                        grant.setString(2, permission);
                        grant.executeUpdate();
                    }
                }
            }
            if (!added.isEmpty()) LOGGER.info("Đã thêm quyền mới " + added + " cho các vai trò theo Product Backlog.");
            return;
        }
        try (PreparedStatement clear = conn.prepareStatement(
                     "DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE role_code = ?)");
             PreparedStatement grant = conn.prepareStatement("INSERT INTO role_permissions (role_id, permission_id) "
                     + "SELECT r.id, p.id FROM roles r, permissions p WHERE r.role_code = ? AND p.permission_code = ?")) {
            for (Map.Entry<String, List<String>> role : ROLE_PERMISSIONS.entrySet()) {
                clear.setString(1, role.getKey());
                clear.executeUpdate();
                grant.setString(1, role.getKey());
                for (String permission : role.getValue()) {
                    grant.setString(2, permission);
                    grant.executeUpdate();
                }
            }
        }
        LOGGER.info("Đã gán lại quyền các vai trò theo Product Backlog (sheet 2, 4).");
    }

    /** Họ tên người dùng lưu IN HOA, không dấu (UserDAO.normalizeName); đổi các tên đã lưu từ trước. */
    static void normalizeUserNames(Connection conn) throws SQLException {
        if (!hasTable(conn, "users")) return;
        // Giữ họ tên gốc (có dấu) ở display_name trước khi đổi full_name sang in hoa không dấu.
        if (hasColumn(conn, "users", "display_name")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE users SET display_name = full_name WHERE display_name IS NULL");
            }
        }
        Map<Long, String> changed = new java.util.LinkedHashMap<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, full_name FROM users")) {
            while (rs.next()) {
                String name = rs.getString("full_name");
                String upper = com.ems.dao.UserDAO.normalizeName(name);
                if (name != null && !name.equals(upper)) changed.put(rs.getLong("id"), upper);
            }
        }
        if (changed.isEmpty()) return;
        try (PreparedStatement stmt = conn.prepareStatement("UPDATE users SET full_name = ? WHERE id = ?")) {
            for (Map.Entry<Long, String> e : changed.entrySet()) {
                stmt.setString(1, e.getValue());
                stmt.setLong(2, e.getKey());
                stmt.executeUpdate();
            }
        }
        LOGGER.info("Đã đổi họ tên " + changed.size() + " người dùng sang chữ in hoa không dấu.");
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
