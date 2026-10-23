package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.config.S4TimetableMigration;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Dữ liệu H2 cho test S4-01, S4-06. Bảng tạo giống DatabaseMigration của S3 để chạy chung với test S3;
 * mọi dòng dùng id 9101-9199 và mã S4T-... để không đụng dữ liệu test khác, reset() xoá và chèn lại.
 *
 * Lớp S4T-K16 (chuẩn bị, 30 buổi): S4T-GIT 4 buổi (LE HOANG NAM), S4T-WEB1 12 buổi (NGO THI THU),
 * S4T-WEB2 14 buổi (NGO THI THU). Lớp S4T-K15 đang học có 3 học viên; S4T-K17 cùng chương trình, còn 1 chỗ;
 * S4T-NOGV có môn chưa phân công giảng viên.
 */
final class S4TestData {
    private S4TestData() {
    }

    static void reset() throws Exception {
        TestDatabaseHelper.initTestDatabase();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            createTables(st);
            S4TimetableMigration.run(conn);
            String[] cleanup = {
                    "DELETE FROM attendance WHERE session_id BETWEEN 9201 AND 9299",
                    "DELETE FROM absence_requests WHERE session_id BETWEEN 9201 AND 9299",
                    "DELETE FROM audit_logs WHERE entity_type = 'ATTENDANCE' AND entity_id LIKE '92__'",
                    "DELETE FROM class_sessions WHERE class_id BETWEEN 9101 AND 9199",
                    "DELETE FROM enrollments WHERE class_id BETWEEN 9101 AND 9199",
                    "DELETE FROM class_subject_assistants WHERE class_subject_id BETWEEN 9101 AND 9199",
                    "DELETE FROM class_subjects WHERE class_id BETWEEN 9101 AND 9199",
                    "DELETE FROM classes WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM subject_lessons WHERE subject_id BETWEEN 9101 AND 9199",
                    "DELETE FROM subjects WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM classrooms WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM students WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM users WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM programs WHERE id BETWEEN 9101 AND 9199",
                    "DELETE FROM student_status_logs WHERE student_code LIKE 'S4T-%'",
                    "DELETE FROM holidays WHERE name LIKE 'S4T %'",
            };
            for (String sql : cleanup) st.execute(sql);

            st.execute("INSERT INTO programs (id, program_code, program_name) VALUES "
                    + "(9101, 'S4T-WEB', 'Lập trình web Full-stack'), (9102, 'S4T-JAVA', 'Lập trình Java')");
            st.execute("INSERT INTO classrooms (id, room_code, room_name, capacity, status) VALUES "
                    + "(9101, 'S4T-P401', 'Phòng 401', 30, 'AVAILABLE'), (9102, 'S4T-LAB1', 'Lab 1', 25, 'AVAILABLE'), "
                    + "(9103, 'S4T-P999', 'Phòng bảo trì', 30, 'MAINTENANCE')");
            st.execute("INSERT INTO users (id, user_code, email, password_hash, full_name, phone) VALUES "
                    + "(9101, 'S4T-GV1', 's4t.gv1@test.vn', 'x', 'LE HOANG NAM', NULL), "
                    + "(9102, 'S4T-GV2', 's4t.gv2@test.vn', 'x', 'NGO THI THU', NULL), "
                    + "(9103, 'S4T-TG1', 's4t.tg1@test.vn', 'x', 'PHAM GIA HUY', NULL), "
                    + "(9111, 'S4T-HV1', 's4t.hv1@test.vn', 'x', 'NGUYEN MINH ANH', '0912345678'), "
                    + "(9112, 'S4T-HV2', 's4t.hv2@test.vn', 'x', 'HOANG YEN NHI', '0934118205'), "
                    + "(9113, 'S4T-HV3', 's4t.hv3@test.vn', 'x', 'TRAN DUC HUY', '0904612775')");
            st.execute("INSERT INTO subjects (id, subject_code, subject_name, total_sessions) VALUES "
                    + "(9101, 'S4T-GIT', 'Git và làm việc nhóm', 4), (9102, 'S4T-WEB1', 'HTML & CSS nền tảng', 12), "
                    + "(9103, 'S4T-WEB2', 'JavaScript cơ bản', 14)");
            st.execute("INSERT INTO subject_lessons (subject_id, session_order, topic) VALUES "
                    + "(9101, 1, 'Làm quen Git & GitHub'), (9101, 2, 'Nhánh và gộp nhánh'), "
                    + "(9101, 3, 'Pull Request & Code Review'), (9101, 4, 'Xử lý xung đột'), "
                    + "(9102, 1, 'Cấu trúc tài liệu HTML5')");
            st.execute("INSERT INTO classes (id, class_code, class_name, program_id, start_date, max_capacity, "
                    + "default_classroom_id, study_mode, status) VALUES "
                    + "(9101, 'S4T-K16', 'Web Full-stack K16', 9101, '2026-11-02', 30, 9101, 'HYBRID', 'PLANNING'), "
                    + "(9102, 'S4T-K15', 'Web Full-stack K15', 9101, '2026-08-12', 30, 9101, 'OFFLINE', 'IN_PROGRESS'), "
                    + "(9103, 'S4T-K17', 'Web Full-stack K17', 9101, '2026-11-01', 1, 9102, 'OFFLINE', 'PLANNING'), "
                    + "(9104, 'S4T-NOGV', 'Lớp thiếu giảng viên', 9101, '2026-11-02', 30, 9101, 'OFFLINE', 'PLANNING'), "
                    + "(9105, 'S4T-J01', 'Java 01', 9102, '2026-11-01', 30, 9102, 'OFFLINE', 'PLANNING'), "
                    + "(9106, 'S4T-OLD', 'Lớp đã kết thúc', 9101, '2025-01-01', 30, 9101, 'OFFLINE', 'FINISHED')");
            st.execute("INSERT INTO class_subjects (id, class_id, subject_id, primary_instructor_id, order_index) VALUES "
                    + "(9101, 9101, 9101, 9101, 1), (9102, 9101, 9102, 9102, 2), (9103, 9101, 9103, 9102, 3), "
                    + "(9104, 9102, 9101, 9101, 1), (9105, 9104, 9101, NULL, 1)");
            st.execute("INSERT INTO class_subject_assistants (class_subject_id, ta_user_id) VALUES (9101, 9103)");
            st.execute("INSERT INTO students (id, user_id, student_code, enrollment_status) VALUES "
                    + "(9111, 9111, 'S4T-HV1', 'STUDYING'), (9112, 9112, 'S4T-HV2', 'STUDYING'), "
                    + "(9113, 9113, 'S4T-HV3', 'STUDYING')");
            st.execute("INSERT INTO enrollments (class_id, student_id, enrollment_date, status) VALUES "
                    + "(9102, 9111, '2026-08-12', 'ENROLLED'), (9102, 9112, '2026-08-12', 'ENROLLED'), "
                    + "(9102, 9113, '2026-08-12', 'ENROLLED')");
        }
    }

    private static void createTables(Statement st) throws Exception {
        st.execute("""
            CREATE TABLE IF NOT EXISTS attendance (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                session_id BIGINT NOT NULL,
                student_id BIGINT NOT NULL,
                status VARCHAR(20) DEFAULT 'PRESENT',
                notes VARCHAR(255) NULL,
                marked_by BIGINT NOT NULL,
                marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_by BIGINT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT uk_session_student UNIQUE (session_id, student_id)
            )
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS absence_requests (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                student_id BIGINT NOT NULL,
                session_id BIGINT NOT NULL,
                reason TEXT NOT NULL,
                proof_file_url VARCHAR(255) NULL,
                status VARCHAR(20) DEFAULT 'PENDING',
                reviewed_by BIGINT NULL,
                review_note VARCHAR(255) NULL,
                reviewed_at TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS subject_lessons (
                id INT AUTO_INCREMENT PRIMARY KEY,
                subject_id INT NOT NULL,
                session_order INT NOT NULL,
                topic VARCHAR(255) NOT NULL,
                objectives TEXT NULL,
                CONSTRAINT uk_subject_session UNIQUE (subject_id, session_order)
            )
        """);
        st.execute("""
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
        st.execute("""
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
        st.execute("""
            CREATE TABLE IF NOT EXISTS class_subjects (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                class_id BIGINT NOT NULL,
                subject_id INT NOT NULL,
                primary_instructor_id BIGINT NULL,
                status VARCHAR(20) DEFAULT 'NOT_STARTED',
                CONSTRAINT uk_class_subject UNIQUE (class_id, subject_id)
            )
        """);
        st.execute("""
            CREATE TABLE IF NOT EXISTS class_subject_assistants (
                class_subject_id BIGINT NOT NULL,
                ta_user_id BIGINT NOT NULL,
                PRIMARY KEY (class_subject_id, ta_user_id)
            )
        """);
        st.execute("""
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
        st.execute("""
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
    }
}
