package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.controller.ScheduleServlet;
import com.ems.model.*;
import com.ems.service.ClassStore;
import com.ems.service.HolidayStore;
import com.ems.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động tính năng Sinh thời khoá biểu tự động theo mẫu lặp (User Story S4-01)
 * Jira Issue: IDTTX-247 / Subtask IDTTX-263 [BE]
 * Chạy trên H2 với lớp, môn, giảng viên, giáo trình, phòng học của S3 (xem S4TestData).
 *
 * Tiêu chí nghiệm thu (Acceptance Criteria):
 *   1. Khai báo mẫu: các thứ trong tuần, ca học, ngày bắt đầu, số buổi
 *   2. Hệ thống sinh đủ buổi và tự động bỏ qua các ngày nghỉ lễ đã khai báo
 *   3. Xem trước danh sách buổi trước khi xác nhận lưu
 *   4. Mỗi buổi được gắn với một bài học trong môn (theo thứ tự môn và lấy đúng GV theo phân công)
 *   5. Phân quyền: Chỉ Quản lý đào tạo hoặc Admin mới được phép thao tác
 *
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ScheduleGenerationTest {

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() throws Exception {
        S4TestData.reset();
        scheduleService = new ScheduleService();
    }

    private SchedulePattern k16(int count) {
        return new SchedulePattern("S4T-K16", Arrays.asList(1, 3, 5), "18:00", "20:30", "2026-11-02", count, "S4T-LAB1");
    }

    @Test
    @DisplayName("S4-01 AC1: Sinh đúng và đủ số buổi cho một lớp theo mẫu lặp Thứ 2 - 4 - 6")
    void testGenerateExactSessions() {
        ScheduleGenerationResult result = scheduleService.generateSchedule(k16(30));

        assertEquals(30, result.getSessions().size(), "Phải sinh đủ chính xác 30 buổi học");
        assertEquals("S4T-K16", result.getKlass().getCode());
        for (int i = 0; i < result.getSessions().size(); i++) {
            PlannedSession s = result.getSessions().get(i);
            int dow = LocalDate.parse(s.getDate()).getDayOfWeek().getValue() % 7;
            assertTrue(dow == 1 || dow == 3 || dow == 5, "Buổi học ngày " + s.getDate() + " phải rơi vào Thứ 2, 4 hoặc 6");
            assertEquals("18:00", s.getStart());
            assertEquals("20:30", s.getEnd());
            assertEquals("S4T-LAB1", s.getRoom());
            assertEquals(i + 1, s.getNo());
        }
    }

    @Test
    @DisplayName("S4-01 AC2: Tự động bỏ qua các ngày nghỉ lễ đã khai báo (20/11) và không bị hụt số buổi")
    void testAutoSkipDeclaredHolidays() {
        ScheduleGenerationResult result = scheduleService.generateSchedule(k16(20));

        assertTrue(result.getSkipped().stream().anyMatch(d -> "2026-11-20".equals(d.getDate())),
                "Ngày 20/11/2026 phải được tự động bỏ qua do nghỉ lễ");
        assertFalse(result.getSessions().stream().anyMatch(s -> "2026-11-20".equals(s.getDate())),
                "Không được xếp lịch học vào ngày nghỉ lễ 20/11/2026");
        assertEquals(20, result.getSessions().size(), "Số buổi học thực tế phải đủ số lượng yêu cầu");
    }

    @Test
    @DisplayName("S4-01 AC3: Gắn từng buổi với bài học theo đúng thứ tự môn của lớp và giáo trình (subject_lessons)")
    void testSequentialLessonMappingWithSyllabus() {
        List<PlannedSession> sessions = scheduleService.generateSchedule(k16(30)).getSessions();

        for (int i = 0; i < 4; i++) {
            assertEquals("S4T-GIT", sessions.get(i).getSubject(), "Buổi " + (i + 1) + " phải là môn Git");
            assertEquals(i + 1, sessions.get(i).getLesson());
        }
        assertEquals("Làm quen Git & GitHub", sessions.get(0).getTopic());
        assertEquals("Xử lý xung đột", sessions.get(3).getTopic());

        assertEquals("S4T-WEB1", sessions.get(4).getSubject());
        assertEquals(1, sessions.get(4).getLesson());
        assertEquals("Cấu trúc tài liệu HTML5", sessions.get(4).getTopic());
        assertEquals("Bài học số 2", sessions.get(5).getTopic(), "Buổi chưa có trong giáo trình dùng tên mặc định");

        assertEquals("S4T-WEB1", sessions.get(15).getSubject());
        assertEquals(12, sessions.get(15).getLesson());
        assertEquals("S4T-WEB2", sessions.get(16).getSubject());
        assertEquals(1, sessions.get(16).getLesson());
    }

    @Test
    @DisplayName("S4-01 AC4: Tự động gán đúng giảng viên phân công theo từng môn của lớp")
    void testTeacherAssignmentBySubject() {
        List<PlannedSession> sessions = scheduleService.generateSchedule(k16(30)).getSessions();

        for (int i = 0; i < 4; i++) {
            assertEquals("LE HOANG NAM", sessions.get(i).getTeacher());
        }
        for (int i = 4; i < 30; i++) {
            assertEquals("NGO THI THU", sessions.get(i).getTeacher());
        }
        Klass klass = ClassStore.findByCode("S4T-K16");
        assertEquals(List.of("PHAM GIA HUY"), klass.getSubjects().get(0).getTas());
    }

    @Test
    @DisplayName("S4-01 AC5: Tính khung dự kiến (Buổi đầu, Buổi cuối, Số buổi/tuần, Số ngày nghỉ lễ bị bỏ qua)")
    void testEstimatedOverviewCalculation() {
        ScheduleGenerationResult result = scheduleService.generateSchedule(k16(30));

        assertEquals("2026-11-02", result.getFirstDate());
        assertNotNull(result.getLastDate());
        assertEquals(3, result.getSessionsPerWeek());
        assertEquals(10, result.getPlainWeeks(), "30 buổi / 3 buổi/tuần = 10 tuần lý thuyết");
        assertTrue(result.getActualWeeks() >= 10);
        assertFalse(result.getSkipped().isEmpty(), "Khoá học đi qua 20/11 nên phải bỏ qua ngày nghỉ lễ");
    }

    @Test
    @DisplayName("S4-01 AC6: Thêm ngày nghỉ lễ mới (lưu CSDL) và hệ thống tự động nhận diện khi sinh lịch")
    void testAddNewHolidayDynamically() {
        HolidayStore.addHoliday("S4T Ngày hội CNTT", "2026-11-04", "2026-11-04");

        assertTrue(HolidayStore.getAllHolidays().stream().anyMatch(h -> "S4T Ngày hội CNTT".equals(h.getName())));
        ScheduleGenerationResult result = scheduleService.generateSchedule(k16(10));
        assertTrue(result.getSkipped().stream().anyMatch(d -> "2026-11-04".equals(d.getDate())));
        assertFalse(result.getSessions().stream().anyMatch(s -> "2026-11-04".equals(s.getDate())));
        assertThrows(IllegalArgumentException.class, () -> HolidayStore.addHoliday("S4T Sai", "2026-11-05", "2026-11-04"));
    }

    @Test
    @DisplayName("S4-01 AC7: Lưu thời khoá biểu vào class_sessions, đúng giảng viên, phòng, bài học; lưu lại thì thay lịch cũ")
    void testSaveScheduleSuccessfully() throws Exception {
        SchedulePattern pattern = k16(30);
        assertFalse(scheduleService.hasSchedule("S4T-K16"));

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);
        scheduleService.saveSchedule("S4T-K16", result.getSessions(), pattern);
        scheduleService.saveSchedule("S4T-K16", result.getSessions(), pattern);

        List<PlannedSession> saved = scheduleService.getSessionsByClass("S4T-K16");
        assertEquals(30, saved.size(), "Lưu lại lần hai phải thay lịch cũ, không nhân đôi");
        assertEquals("S4T-K16-1", saved.get(0).getId());
        assertEquals("Làm quen Git & GitHub", saved.get(0).getTopic());
        assertEquals("NGO THI THU", saved.get(29).getTeacher());
        assertEquals("S4T-LAB1", saved.get(0).getRoom());

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT class_subject_id, instructor_id, classroom_id, subject_lesson_id, session_number "
                             + "FROM class_sessions WHERE class_id = 9101 ORDER BY session_date LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(9101, rs.getLong("class_subject_id"));
                assertEquals(9101, rs.getLong("instructor_id"));
                assertEquals(9102, rs.getLong("classroom_id"));
                assertNotEquals(0, rs.getLong("subject_lesson_id"));
                assertEquals(1, rs.getInt("session_number"));
            }
        }
        assertEquals("Tối 2-4-6", ClassStore.findByCode("S4T-K16").getSlot());
    }

    @Test
    @DisplayName("S4-01 AC7: Không lưu khi môn chưa có giảng viên, phòng không tồn tại hoặc lớp đã có buổi học xong")
    void testSaveScheduleRejected() throws Exception {
        SchedulePattern noTeacher = new SchedulePattern("S4T-NOGV", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 4, "S4T-P401");
        ScheduleGenerationResult r1 = scheduleService.generateSchedule(noTeacher);
        assertEquals("Chưa phân công", r1.getSessions().get(0).getTeacher(), "Xem trước vẫn được, ghi rõ chưa phân công");
        assertThrows(IllegalArgumentException.class, () -> scheduleService.saveSchedule("S4T-NOGV", r1.getSessions(), noTeacher));

        SchedulePattern badRoom = new SchedulePattern("S4T-K16", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 4, "KHONG-CO");
        ScheduleGenerationResult r2 = scheduleService.generateSchedule(badRoom);
        assertThrows(IllegalArgumentException.class, () -> scheduleService.saveSchedule("S4T-K16", r2.getSessions(), badRoom));
        assertFalse(scheduleService.hasSchedule("S4T-K16"), "Lưu lỗi thì không được ghi buổi nào");

        SchedulePattern ok = k16(4);
        ScheduleGenerationResult r3 = scheduleService.generateSchedule(ok);
        scheduleService.saveSchedule("S4T-K16", r3.getSessions(), ok);
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("UPDATE class_sessions SET status = 'COMPLETED' WHERE class_id = 9101 AND session_number = 1");
        }
        assertThrows(IllegalArgumentException.class, () -> scheduleService.saveSchedule("S4T-K16", r3.getSessions(), ok));
        assertEquals(4, scheduleService.getSessionsByClass("S4T-K16").size());
    }

    @Test
    @DisplayName("S4-01 AC8: Kiểm tra tính hợp lệ dữ liệu đầu vào (Validation)")
    void testPatternValidation() {
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateSchedule(
                new SchedulePattern("S4T-K16", Collections.emptyList(), "18:00", "20:30", "2026-11-02", 30, "S4T-P401")));
        IllegalArgumentException timeError = assertThrows(IllegalArgumentException.class, () -> scheduleService.generateSchedule(
                new SchedulePattern("S4T-K16", Arrays.asList(1, 3), "20:30", "18:00", "2026-11-02", 30, "S4T-P401")));
        assertTrue(timeError.getMessage().contains("phải sau giờ bắt đầu"));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateSchedule(
                new SchedulePattern("S4T-K16", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 0, "S4T-P401")));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateSchedule(
                new SchedulePattern("UNKNOWN-CLASS", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 30, "S4T-P401")));
        assertThrows(IllegalArgumentException.class, () -> scheduleService.generateSchedule(
                new SchedulePattern("S4T-J01", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 30, "S4T-P401")),
                "Lớp chưa có môn học thì không sinh lịch");
    }

    @Test
    @DisplayName("S4-01 AC9: Danh sách lớp, phòng lấy từ CSDL; chỉ Quản lý đào tạo và Admin được sinh lịch")
    void testClassListAndAuthorization() {
        List<String> codes = ClassStore.getAllClasses().stream().map(Klass::getCode).toList();
        assertTrue(codes.contains("S4T-K16"));
        assertFalse(codes.contains("S4T-OLD"), "Lớp đã kết thúc không còn để xếp lịch");
        assertTrue(ClassStore.getAvailableRooms().contains("S4T-LAB1"));
        assertFalse(ClassStore.getAvailableRooms().contains("S4T-P999"), "Phòng đang bảo trì không được chọn");

        // Vai trò trong phiên là mã ở bảng roles
        assertTrue(ScheduleServlet.canManageSchedule(new User(1L, "QLDT", "a@x.vn", "ACTIVE", List.of("TRAINING_MANAGER"))));
        assertTrue(ScheduleServlet.canManageSchedule(new User(2L, "Admin", "b@x.vn", "ACTIVE", List.of("ADMIN"))));
        assertFalse(ScheduleServlet.canManageSchedule(new User(3L, "GV", "c@x.vn", "ACTIVE", List.of("INSTRUCTOR"))));
        assertFalse(ScheduleServlet.canManageSchedule(new User(4L, "HV", "d@x.vn", "ACTIVE", List.of("STUDENT"))));
        assertFalse(ScheduleServlet.canManageSchedule(null));
    }
}
