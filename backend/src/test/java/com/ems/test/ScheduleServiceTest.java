package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dto.ClassSessionResponse;
import com.ems.service.ClassSessionService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động cho ClassSessionService (EP-04 – S4-04)
 * Sử dụng H2 in-memory (MySQL mode) theo mẫu TestDatabaseHelper
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ScheduleServiceTest {

    private static ClassSessionService service;

    @BeforeAll
    static void setupDatabase() throws Exception {
        // Khởi tạo H2 cùng các bảng cơ bản đã có
        TestDatabaseHelper.initTestDatabase();

        // Bổ sung các bảng cần thiết cho EP-04
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS classrooms (
                    id        INT AUTO_INCREMENT PRIMARY KEY,
                    room_code VARCHAR(50)  NOT NULL UNIQUE,
                    room_name VARCHAR(100) NOT NULL,
                    capacity  INT          NOT NULL DEFAULT 30,
                    room_type VARCHAR(20)  DEFAULT 'PHYSICAL',
                    status    VARCHAR(20)  DEFAULT 'AVAILABLE'
                )
            """);

            // Đảm bảo bảng classes có đủ cột
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS classes (
                    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_code           VARCHAR(50)  NOT NULL UNIQUE,
                    class_name           VARCHAR(150) NOT NULL,
                    program_id           INT NOT NULL DEFAULT 1,
                    start_date           DATE NOT NULL,
                    end_date             DATE NULL,
                    max_capacity         INT  NOT NULL DEFAULT 30,
                    default_classroom_id INT  NULL,
                    study_mode           VARCHAR(20) DEFAULT 'OFFLINE',
                    status               VARCHAR(20) DEFAULT 'PLANNING',
                    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_subjects (
                    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id              BIGINT NOT NULL,
                    subject_id            INT    NOT NULL,
                    primary_instructor_id BIGINT NOT NULL,
                    status                VARCHAR(20) DEFAULT 'NOT_STARTED'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS class_sessions (
                    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id          BIGINT NOT NULL,
                    class_subject_id  BIGINT NOT NULL,
                    subject_lesson_id INT    NULL,
                    session_number    INT    NOT NULL,
                    session_date      DATE   NOT NULL,
                    start_time        TIME   NOT NULL,
                    end_time          TIME   NOT NULL,
                    classroom_id      INT    NOT NULL,
                    instructor_id     BIGINT NOT NULL,
                    status            VARCHAR(20) DEFAULT 'SCHEDULED',
                    cancel_reason     VARCHAR(255) NULL,
                    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
        }

        // Seed dữ liệu mẫu
        seedData();

        service = new ClassSessionService();
    }

    @BeforeEach
    void clearSessions() throws Exception {
        // Không clear toàn bộ – dùng dữ liệu seed cố định
    }

    // =========================================================================
    // Seed dữ liệu: 2 lớp, 2 phòng, 2 giảng viên, nhiều buổi học
    // =========================================================================
    private static void seedData() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Giảng viên
            stmt.execute("""
                MERGE INTO users (id, user_code, email, password_hash, full_name, status)
                KEY(id)
                VALUES (101, 'GV001', 'gv001@test.com', 'hash', 'Nguyễn Văn A', 'ACTIVE'),
                       (102, 'GV002', 'gv002@test.com', 'hash', 'Trần Thị B',   'ACTIVE')
            """);

            // Phòng học
            stmt.execute("""
                MERGE INTO classrooms (id, room_code, room_name)
                KEY(id)
                VALUES (1, 'A101', 'Phòng A101'), (2, 'B201', 'Phòng B201')
            """);

            // Môn học
            stmt.execute("""
                MERGE INTO subjects (id, subject_code, subject_name, total_sessions)
                KEY(id)
                VALUES (1, 'JAVA', 'Lập trình Java', 15),
                       (2, 'WEB',  'Lập trình Web',  15)
            """);

            // Lớp học
            stmt.execute("""
                MERGE INTO classes (id, class_code, class_name, program_id, start_date)
                KEY(id)
                VALUES (1, 'JAVA-K1', 'Lớp Java K1', 1, '2026-10-01'),
                       (2, 'WEB-K1',  'Lớp Web K1',  1, '2026-10-01')
            """);

            // Phân công môn / lớp (test S3 dùng chung H2 có thể đã chèn cặp lớp-môn này với id khác)
            stmt.execute("DELETE FROM class_subjects WHERE class_id IN (1, 2)");
            stmt.execute("""
                MERGE INTO class_subjects (id, class_id, subject_id, primary_instructor_id)
                KEY(id)
                VALUES (1, 1, 1, 101), (2, 2, 2, 102)
            """);

            // Buổi học trong tuần 2026-10-19 ~ 2026-10-25
            stmt.execute("DELETE FROM class_sessions");
            stmt.execute("""
                INSERT INTO class_sessions
                    (id, class_id, class_subject_id, session_number, session_date,
                     start_time, end_time, classroom_id, instructor_id, status)
                VALUES
                -- Lịch bình thường
                (1,  1, 1, 1, '2026-10-20', '08:00:00', '10:00:00', 1, 101, 'SCHEDULED'),
                (2,  1, 1, 2, '2026-10-22', '08:00:00', '10:00:00', 1, 101, 'SCHEDULED'),
                (3,  2, 2, 1, '2026-10-21', '13:00:00', '15:00:00', 2, 102, 'SCHEDULED'),
                -- Tuần 2026-10-26 ~ 2026-11-01
                (4,  1, 1, 3, '2026-10-27', '08:00:00', '10:00:00', 1, 101, 'SCHEDULED'),
                -- Tháng 2026-11
                (5,  1, 1, 4, '2026-11-03', '08:00:00', '10:00:00', 1, 101, 'SCHEDULED'),
                -- Trùng phòng (session 6 và 7 cùng phòng A101, cùng ngày, giờ giao nhau)
                (6,  2, 2, 2, '2026-10-20', '09:00:00', '11:00:00', 1, 102, 'SCHEDULED'),
                -- Trùng giảng viên (session 7 và 8 cùng GV001, cùng ngày, giờ giao nhau)
                (7,  2, 2, 3, '2026-10-22', '09:00:00', '11:00:00', 2, 101, 'SCHEDULED'),
                -- Trùng lớp (session 8 và 9 cùng lớp 1, cùng ngày, giờ giao nhau)
                (8,  1, 1, 5, '2026-10-27', '09:00:00', '11:00:00', 2, 102, 'SCHEDULED'),
                -- Tiếp nối: kết thúc 10:00, bắt đầu 10:00 => KHÔNG trùng
                (9,  1, 1, 6, '2026-10-28', '08:00:00', '10:00:00', 1, 101, 'SCHEDULED'),
                (10, 1, 1, 7, '2026-10-28', '10:00:00', '12:00:00', 1, 101, 'SCHEDULED')
            """);
        }
    }

    // =========================================================================
    // TC-01: Lấy lịch tuần có dữ liệu
    // =========================================================================
    @Test @Order(1)
    void testWeeklySchedule_withData() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", null, null, null);
        assertFalse(sessions.isEmpty(), "Tuần 2026-10-20 phải có buổi học");
        sessions.forEach(s -> {
            assertTrue(s.getSessionDate().compareTo("2026-10-19") >= 0, "session_date >= 2026-10-19");
            assertTrue(s.getSessionDate().compareTo("2026-10-25") <= 0, "session_date <= 2026-10-25");
        });
    }

    // =========================================================================
    // TC-02: Lấy lịch tuần không có dữ liệu
    // =========================================================================
    @Test @Order(2)
    void testWeeklySchedule_empty() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2025-01-05", null, null, null);
        assertTrue(sessions.isEmpty(), "Tuần 2025-01-05 không có buổi học");
    }

    // =========================================================================
    // TC-03: Lấy lịch tháng có dữ liệu
    // =========================================================================
    @Test @Order(3)
    void testMonthlySchedule_withData() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getMonthlySchedule(2026, 10, null, null, null);
        assertFalse(sessions.isEmpty(), "Tháng 10/2026 phải có buổi học");
        sessions.forEach(s -> assertTrue(s.getSessionDate().startsWith("2026-10"),
                "Tháng phải là 2026-10"));
    }

    // =========================================================================
    // TC-04: Lọc theo lớp
    // =========================================================================
    @Test @Order(4)
    void testFilterByClass() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", 1L, null, null);
        sessions.forEach(s -> assertEquals(1L, s.getClassId(), "Phải là lớp ID=1"));
    }

    // =========================================================================
    // TC-05: Lọc theo giảng viên
    // =========================================================================
    @Test @Order(5)
    void testFilterByLecturer() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", null, 101L, null);
        sessions.forEach(s -> assertEquals(101L, s.getInstructorId(), "Phải là GV ID=101"));
    }

    // =========================================================================
    // TC-06: Lọc theo phòng học
    // =========================================================================
    @Test @Order(6)
    void testFilterByRoom() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", null, null, 1);
        sessions.forEach(s -> assertEquals(1, s.getClassroomId(), "Phải là phòng ID=1"));
    }

    // =========================================================================
    // TC-07: Kết hợp nhiều bộ lọc
    // =========================================================================
    @Test @Order(7)
    void testMultiFilter() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", 1L, 101L, 1);
        sessions.forEach(s -> {
            assertEquals(1L,  s.getClassId(),      "classId=1");
            assertEquals(101L, s.getInstructorId(), "lecturerId=101");
            assertEquals(1,   s.getClassroomId(),  "roomId=1");
        });
    }

    // =========================================================================
    // TC-08: Xem chi tiết buổi học tồn tại
    // =========================================================================
    @Test @Order(8)
    void testGetSessionById_found() throws Exception {
        ClassSessionResponse session = service.getSessionById(1L);
        assertNotNull(session);
        assertEquals(1L, session.getId());
        assertEquals("2026-10-20", session.getSessionDate());
    }

    // =========================================================================
    // TC-09: Xem chi tiết buổi học không tồn tại -> 404
    // =========================================================================
    @Test @Order(9)
    void testGetSessionById_notFound() {
        var ex = assertThrows(com.ems.exception.ResourceNotFoundException.class,
                () -> service.getSessionById(9999L));
        assertEquals(404, ex.getStatusCode());
    }

    // =========================================================================
    // TC-10: Ngày sai định dạng -> ValidationException
    // =========================================================================
    @Test @Order(10)
    void testInvalidDateFormat() {
        var ex = assertThrows(com.ems.exception.ValidationException.class,
                () -> service.getWeeklySchedule("20-10-2026", null, null, null));
        assertEquals(400, ex.getStatusCode());
    }

    // =========================================================================
    // TC-11: startDate > endDate -> ValidationException
    // =========================================================================
    @Test @Order(11)
    void testInvalidDateRange() {
        var ex = assertThrows(com.ems.exception.ValidationException.class,
                () -> service.getScheduleByRange("2026-10-25", "2026-10-20", null, null, null));
        assertEquals(400, ex.getStatusCode());
    }

    // =========================================================================
    // TC-12: Phát hiện trùng phòng (session 1 & 6: phòng A101, 2026-10-20, 08-10 giao 09-11)
    // =========================================================================
    @Test @Order(12)
    void testConflict_room() throws Exception {
        List<Map<String, Object>> conflicts =
                service.detectConflicts("2026-10-20", "2026-10-20");
        boolean hasRoomConflict = conflicts.stream()
                .anyMatch(c -> "ROOM".equals(c.get("conflictType")));
        assertTrue(hasRoomConflict, "Phải phát hiện xung đột phòng ngày 2026-10-20");
    }

    // =========================================================================
    // TC-13: Phát hiện trùng giảng viên (session 2 & 7: GV101, 2026-10-22, 08-10 giao 09-11)
    // =========================================================================
    @Test @Order(13)
    void testConflict_instructor() throws Exception {
        List<Map<String, Object>> conflicts =
                service.detectConflicts("2026-10-22", "2026-10-22");
        boolean hasInstructorConflict = conflicts.stream()
                .anyMatch(c -> "INSTRUCTOR".equals(c.get("conflictType")));
        assertTrue(hasInstructorConflict, "Phải phát hiện xung đột giảng viên ngày 2026-10-22");
    }

    // =========================================================================
    // TC-14: Phát hiện trùng lớp (session 4 & 8: lớp 1, 2026-10-27, 08-10 giao 09-11)
    // =========================================================================
    @Test @Order(14)
    void testConflict_class() throws Exception {
        List<Map<String, Object>> conflicts =
                service.detectConflicts("2026-10-27", "2026-10-27");
        boolean hasClassConflict = conflicts.stream()
                .anyMatch(c -> "CLASS".equals(c.get("conflictType")));
        assertTrue(hasClassConflict, "Phải phát hiện xung đột lớp ngày 2026-10-27");
    }

    // =========================================================================
    // TC-15: Hai buổi tiếp nối (10:00-12:00 sau 08:00-10:00) KHÔNG bị coi là trùng
    // =========================================================================
    @Test @Order(15)
    void testNoConflict_adjacentSessions() throws Exception {
        // session 9 (08-10) và session 10 (10-12) cùng phòng 1, cùng GV 101, 2026-10-28
        // Điều kiện giao: startA < endB AND endA > startB
        // 08:00 < 12:00 AND 10:00 > 10:00 → FALSE (10:00 > 10:00 = false)
        List<Map<String, Object>> conflicts =
                service.detectConflicts("2026-10-28", "2026-10-28");
        // Không được có cặp nào chứa cả session 9 & 10
        boolean adjacentFlaggedAsConflict = conflicts.stream().anyMatch(c -> {
            long a = (long) c.get("sessionAId");
            long b = (long) c.get("sessionBId");
            return (a == 9 && b == 10) || (a == 10 && b == 9);
        });
        assertFalse(adjacentFlaggedAsConflict,
                "Buổi học tiếp nối nhau KHÔNG được coi là xung đột");
    }

    // =========================================================================
    // TC-16: hasConflict flag được đánh dấu đúng trong danh sách lịch
    // =========================================================================
    @Test @Order(16)
    void testHasConflictFlag() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getWeeklySchedule("2026-10-20", null, null, null);
        // session 1 (id=1) và session 6 (id=6) bị trùng phòng → hasConflict = true
        sessions.stream()
                .filter(s -> s.getId() == 1L || s.getId() == 6L)
                .forEach(s -> assertTrue(s.isHasConflict(),
                        "Session " + s.getId() + " phải có hasConflict=true"));
    }

    // =========================================================================
    // TC-17: Tháng không hợp lệ -> ValidationException
    // =========================================================================
    @Test @Order(17)
    void testInvalidMonth() {
        var ex = assertThrows(com.ems.exception.ValidationException.class,
                () -> service.getMonthlySchedule(2026, 13, null, null, null));
        assertEquals(400, ex.getStatusCode());
    }

    // =========================================================================
    // TC-18: Tháng 11/2026 chỉ trả về session trong tháng 11
    // =========================================================================
    @Test @Order(18)
    void testMonthlySchedule_november() throws Exception {
        List<ClassSessionResponse> sessions =
                service.getMonthlySchedule(2026, 11, null, null, null);
        sessions.forEach(s -> assertTrue(s.getSessionDate().startsWith("2026-11"),
                "Tháng phải là 2026-11"));
    }
}
