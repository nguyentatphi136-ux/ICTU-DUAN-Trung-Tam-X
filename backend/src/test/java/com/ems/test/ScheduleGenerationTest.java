package com.ems.test;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.model.*;
import com.ems.service.ClassStore;
import com.ems.service.HolidayStore;
import com.ems.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động tính năng Sinh thời khoá biểu tự động theo mẫu lặp (User Story S4-01)
 * Jira Issue: IDTTX-247 / Subtask IDTTX-263 [BE]
 * 
 * Tiêu chí nghiệm thu (Acceptance Criteria):
 *   1. Khai báo mẫu: các thứ trong tuần, ca học, ngày bắt đầu, số buổi (sinh đủ 60 buổi cho một lớp)
 *   2. Hệ thống sinh đủ buổi và tự động bỏ qua các ngày nghỉ lễ đã khai báo
 *   3. Xem trước danh sách buổi trước khi xác nhận lưu
 *   4. Mỗi buổi được gắn với một bài học trong môn (theo thứ tự môn và lấy đúng GV theo phân công)
 *   5. Phân quyền: Chỉ Quản lý đào tạo (TrainingManager) hoặc Admin mới được phép thao tác
 * 
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ScheduleGenerationTest {

    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        HolidayStore.resetToDefault();
        ClassStore.resetToDefault();
        scheduleService = new ScheduleService();
    }

    @Test
    @DisplayName("S4-01 AC1: Sinh đúng và đủ 60 buổi cho một lớp theo mẫu lặp Thứ 2 - 4 - 6")
    void testGenerateExact60Sessions() {
        // Mẫu lặp: Thứ 2, 4, 6 (1, 3, 5), Tối 18:00 - 20:30, bắt đầu từ 2026-11-02, đủ 60 buổi
        SchedulePattern pattern = new SchedulePattern(
                "JAVA-K15",
                Arrays.asList(1, 3, 5),
                "18:00",
                "20:30",
                "2026-11-02",
                60,
                "Lab 1"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        assertNotNull(result);
        assertEquals(60, result.getSessions().size(), "Phải sinh đủ chính xác 60 buổi học");
        assertEquals("JAVA-K15", result.getKlass().getCode());

        // Kiểm tra tất cả các buổi đều rơi vào Thứ 2, Thứ 4 hoặc Thứ 6
        for (PlannedSession s : result.getSessions()) {
            LocalDate d = LocalDate.parse(s.getDate());
            int dow = d.getDayOfWeek().getValue() % 7;
            assertTrue(dow == 1 || dow == 3 || dow == 5, "Buổi học ngày " + s.getDate() + " phải rơi vào Thứ 2, 4 hoặc 6");
            assertEquals("18:00", s.getStart());
            assertEquals("20:30", s.getEnd());
            assertEquals("Lab 1", s.getRoom());
        }

        // Kiểm tra số thứ tự buổi tăng dần từ 1 đến 60
        for (int i = 0; i < 60; i++) {
            assertEquals(i + 1, result.getSessions().get(i).getNo());
        }
    }

    @Test
    @DisplayName("S4-01 AC2: Hệ thống tự động bỏ qua các ngày nghỉ lễ đã khai báo (20/11, Tết...) và không bị hụt số buổi")
    void testAutoSkipDeclaredHolidays() {
        // Ngày 2026-11-20 rơi vào Thứ Sáu (dow = 5) - Trùng ngày Nhà giáo Việt Nam
        SchedulePattern pattern = new SchedulePattern(
                "WEB-K16",
                Arrays.asList(1, 3, 5), // T2, T4, T6
                "18:00",
                "20:30",
                "2026-11-16",
                20,
                "Meet WEB"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        // 1. Phải có ngày 2026-11-20 trong danh sách bị bỏ qua
        boolean holidaySkipped = result.getSkipped().stream()
                .anyMatch(sk -> "2026-11-20".equals(sk.getDate()) && sk.getReason().contains("Nhà giáo Việt Nam"));
        assertTrue(holidaySkipped, "Ngày 20/11/2026 phải được tự động bỏ qua do nghỉ lễ");

        // 2. Tuyệt đối không có buổi học chính thức nào rơi vào ngày 2026-11-20
        boolean sessionOnHoliday = result.getSessions().stream()
                .anyMatch(s -> "2026-11-20".equals(s.getDate()));
        assertFalse(sessionOnHoliday, "Không được xếp lịch học vào ngày nghỉ lễ 20/11/2026");

        // 3. Tổng số buổi học được sinh vẫn phải đủ 20 buổi (không bị thiếu)
        assertEquals(20, result.getSessions().size(), "Số buổi học thực tế phải đủ số lượng yêu cầu");
    }

    @Test
    @DisplayName("S4-01 AC3: Gắn từng buổi với bài học theo đúng thứ tự môn và giáo trình chuẩn (Syllabus)")
    void testSequentialLessonMappingWithSyllabus() {
        // Lớp WEB-K16 có các môn theo thứ tự:
        // GIT-01 (4 buổi), WEB-01 (12 buổi), WEB-02 (14 buổi)...
        SchedulePattern pattern = new SchedulePattern(
                "WEB-K16",
                Arrays.asList(1, 3, 5),
                "18:00",
                "20:30",
                "2026-11-02",
                30,
                "Meet WEB"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);
        List<PlannedSession> sessions = result.getSessions();

        // 4 buổi đầu tiên phải thuộc môn GIT-01, từ Bài 1 đến Bài 4
        for (int i = 0; i < 4; i++) {
            PlannedSession s = sessions.get(i);
            assertEquals("GIT-01", s.getSubject(), "Buổi " + (i + 1) + " phải là môn GIT-01");
            assertEquals(i + 1, s.getLesson(), "Buổi " + (i + 1) + " phải là Bài " + (i + 1));
            assertNotNull(s.getTopic());
            assertFalse(s.getTopic().isBlank());
        }
        assertEquals("Làm quen Git & GitHub", sessions.get(0).getTopic());

        // Buổi số 5 bắt đầu môn WEB-01 (Bài 1)
        assertEquals("WEB-01", sessions.get(4).getSubject());
        assertEquals(1, sessions.get(4).getLesson());
        assertEquals("Cấu trúc tài liệu HTML5", sessions.get(4).getTopic());

        // Buổi số 16 kết thúc môn WEB-01 (Bài 12)
        assertEquals("WEB-01", sessions.get(15).getSubject());
        assertEquals(12, sessions.get(15).getLesson());

        // Buổi số 17 chuyển sang môn tiếp theo WEB-02 (Bài 1)
        assertEquals("WEB-02", sessions.get(16).getSubject());
        assertEquals(1, sessions.get(16).getLesson());
    }

    @Test
    @DisplayName("S4-01 AC4: Tự động gán đúng giảng viên phân công theo từng môn của lớp")
    void testTeacherAssignmentBySubject() {
        // Lớp WEB-K16 phân công:
        // GIT-01 -> LE HOANG NAM
        // WEB-01 -> NGO THI THU
        // WEB-03 -> DANG MINH SON
        SchedulePattern pattern = new SchedulePattern(
                "WEB-K16",
                Arrays.asList(1, 3, 5),
                "18:00",
                "20:30",
                "2026-11-02",
                30,
                "Meet WEB"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        // Các buổi của GIT-01 phải có GV LE HOANG NAM
        for (int i = 0; i < 4; i++) {
            assertEquals("LE HOANG NAM", result.getSessions().get(i).getTeacher());
        }

        // Các buổi của WEB-01 phải có GV NGO THI THU
        for (int i = 4; i < 16; i++) {
            assertEquals("NGO THI THU", result.getSessions().get(i).getTeacher());
        }
    }

    @Test
    @DisplayName("S4-01 AC5: Tính toán chính xác khung dự kiến (Buổi đầu, Buổi cuối, Số buổi/tuần, Số ngày nghỉ lễ bị bỏ qua)")
    void testEstimatedOverviewCalculation() {
        SchedulePattern pattern = new SchedulePattern(
                "JAVA-K15",
                Arrays.asList(1, 3, 5), // 3 buổi / tuần
                "18:00",
                "20:30",
                "2026-11-02", // Thứ 2
                60,
                "Lab 1"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        assertEquals("2026-11-02", result.getFirstDate());
        assertNotNull(result.getLastDate());
        assertEquals(3, result.getSessionsPerWeek());
        assertEquals(20, result.getPlainWeeks(), "60 buổi / 3 buổi/tuần = 20 tuần lý thuyết");
        assertTrue(result.getActualWeeks() >= 20, "Số tuần thực tế phải kéo dài hơn hoặc bằng số tuần lý thuyết do có các ngày nghỉ lễ");
        assertTrue(result.getSkipped().size() > 0, "Khoá 60 buổi kéo dài qua Tết nên chắc chắn bỏ qua các ngày nghỉ lễ");
    }

    @Test
    @DisplayName("S4-01 AC6: Thêm ngày nghỉ lễ mới thành công và hệ thống tự động nhận diện khi sinh lịch")
    void testAddNewHolidayDynamically() {
        // Thêm ngày nghỉ lễ bất thường của trường vào ngày 2026-11-04 (Thứ 4)
        HolidayStore.addHoliday("Ngày hội CNTT ICTU", "2026-11-04", "2026-11-04");

        SchedulePattern pattern = new SchedulePattern(
                "JAVA-K15",
                Arrays.asList(1, 3, 5),
                "18:00",
                "20:30",
                "2026-11-02",
                10,
                "Lab 1"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);

        // Ngày 2026-11-04 phải bị bỏ qua
        boolean skipped = result.getSkipped().stream()
                .anyMatch(sk -> "2026-11-04".equals(sk.getDate()) && sk.getReason().contains("ICTU"));
        assertTrue(skipped, "Ngày nghỉ lễ mới thêm phải được tự động bỏ qua khi sinh lịch");

        // Không có buổi học nào vào ngày 2026-11-04
        assertFalse(result.getSessions().stream().anyMatch(s -> "2026-11-04".equals(s.getDate())));
    }

    @Test
    @DisplayName("S4-01 AC7: Xác nhận lưu thời khoá biểu thành công vào hệ thống và cập nhật trạng thái lớp")
    void testSaveScheduleSuccessfully() {
        SchedulePattern pattern = new SchedulePattern(
                "WEB-K16",
                Arrays.asList(1, 3, 5),
                "18:00",
                "20:30",
                "2026-11-02",
                60,
                "Meet WEB"
        );

        ScheduleGenerationResult result = scheduleService.generateSchedule(pattern);
        assertFalse(scheduleService.hasSchedule("WEB-K16"));

        // Xác nhận lưu
        scheduleService.saveSchedule("WEB-K16", result.getSessions(), pattern);

        assertTrue(scheduleService.hasSchedule("WEB-K16"));
        List<PlannedSession> saved = scheduleService.getSessionsByClass("WEB-K16");
        assertEquals(60, saved.size());
        assertEquals("WEB-K16-1", saved.get(0).getId());

        // Kiểm tra thông tin ca học của lớp đã được cập nhật
        Klass k = ClassStore.findByCode("WEB-K16");
        assertNotNull(k);
        assertTrue(k.getSlot().contains("Tối 2-4-6"));
        assertEquals("Meet WEB", k.getRoom());
    }

    @Test
    @DisplayName("S4-01 AC8: Kiểm tra tính hợp lệ dữ liệu đầu vào (Validation)")
    void testPatternValidation() {
        // 1. Không chọn thứ trong tuần
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleService.generateSchedule(new SchedulePattern("WEB-K16", Collections.emptyList(), "18:00", "20:30", "2026-11-02", 60, "P.401"));
        });

        // 2. Giờ kết thúc trước giờ bắt đầu
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleService.generateSchedule(new SchedulePattern("WEB-K16", Arrays.asList(1, 3), "20:30", "18:00", "2026-11-02", 60, "P.401"));
        });

        // 3. Số buổi không hợp lệ (<= 0 hoặc > 300)
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleService.generateSchedule(new SchedulePattern("WEB-K16", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 0, "P.401"));
        });

        // 4. Lớp học không tồn tại
        assertThrows(IllegalArgumentException.class, () -> {
            scheduleService.generateSchedule(new SchedulePattern("UNKNOWN-CLASS", Arrays.asList(1, 3), "18:00", "20:30", "2026-11-02", 60, "P.401"));
        });
    }

    @Test
    @DisplayName("S4-01 AC9: Phân quyền - Chỉ Quản lý đào tạo (TrainingManager) và Admin được phép quản lý lớp và xếp thời khoá biểu")
    void testAuthorizationPermissions() {
        // Quản lý đào tạo: Được phép quản lý lớp (CLASS_MANAGE)
        assertTrue(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.TRAINING_MANAGER),
                PermissionConstant.CLASS_MANAGE
        ));

        // Quản trị viên: Được phép quản lý lớp (CLASS_MANAGE)
        assertTrue(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.ADMIN),
                PermissionConstant.CLASS_MANAGE
        ));

        // Học viên, Khách: Tuyệt đối không có quyền quản lý lớp (CLASS_MANAGE)
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.STUDENT),
                PermissionConstant.CLASS_MANAGE
        ));
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.GUEST),
                PermissionConstant.CLASS_MANAGE
        ));
    }
}
