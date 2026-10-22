package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.SchoolClassDAO;
import com.ems.dao.ClassroomDAO;
import com.ems.dao.ScheduleDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.ConflictCheckResult;
import com.ems.dto.ConflictDetail;
import com.ems.dto.ConflictType;
import com.ems.dto.ScheduleRequest;
import com.ems.exception.ForbiddenAccessException;
import com.ems.exception.ScheduleConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.AuditLog;
import com.ems.model.ClassSession;
import com.ems.model.Classroom;
import com.ems.model.SchoolClass;
import com.ems.model.User;
import com.ems.service.ScheduleConflictService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho User Story S4-02:
 * [BE] Database + API kiểm tra xung đột thời khóa biểu
 * Bao quát đầy đủ 15 tiêu chí kiểm thử theo đặc tả nghiệp vụ.
 */
public class ScheduleConflictTest {

    private ScheduleConflictService service;
    private MockScheduleDAO mockScheduleDAO;
    private MockClassroomDAO mockClassroomDAO;
    private MockClassDAO mockClassDAO;
    private MockUserDAO mockUserDAO;

    private static final LocalDate TEST_DATE = LocalDate.of(2026, 11, 1);
    private static final Long TRAINING_MGR_ID = 2L;
    private static final List<String> TRAINING_MGR_ROLES = List.of(RoleConstant.TRAINING_MANAGER);
    private static final List<String> INSTRUCTOR_ROLES = List.of(RoleConstant.INSTRUCTOR);

    @BeforeEach
    void setUp() {
        mockScheduleDAO = new MockScheduleDAO();
        mockClassroomDAO = new MockClassroomDAO();
        mockClassDAO = new MockClassDAO();
        mockUserDAO = new MockUserDAO();

        service = new ScheduleConflictService(
                mockScheduleDAO,
                mockClassroomDAO,
                mockClassDAO,
                mockUserDAO,
                () -> createMockConnection(mockScheduleDAO)
        );

        // Khởi tạo phòng học mẫu
        mockClassroomDAO.save(new Classroom(1, "PH-101", "Phòng 101", 40, "PHYSICAL", "AVAILABLE"));
        mockClassroomDAO.save(new Classroom(2, "LAB-202", "Phòng máy 202", 35, "PHYSICAL", "AVAILABLE"));

        // Khởi tạo lớp học mẫu
        mockClassDAO.save(new SchoolClass(1L, "CNTT-K23-01", "Lớp Java 01", 1, "IN_PROGRESS"));
        mockClassDAO.save(new SchoolClass(2L, "CNTT-K23-02", "Lớp Web 02", 1, "IN_PROGRESS"));

        // Khởi tạo người dùng / giảng viên mẫu
        mockUserDAO.save(new User(3L, "NV-GV-01", "giangvien@edumanager.vn", "Trần Thị Giảng", RoleConstant.INSTRUCTOR));
        mockUserDAO.save(new User(4L, "NV-GV-02", "giangvien2@edumanager.vn", "Nguyễn Văn Thầy", RoleConstant.INSTRUCTOR));
    }

    // -------------------------------------------------------------------------
    // 1. Tạo lịch hợp lệ thành công
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 1: Tạo lịch hợp lệ thành công khi không có xung đột")
    void testCreateValidScheduleSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        ClassSession created = service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);

        assertNotNull(created, "Buổi học được tạo không được null");
        assertNotNull(created.getId(), "Phải sinh ID cho buổi học mới");
        assertEquals("SCHEDULED", created.getStatus());
        assertEquals(TEST_DATE, created.getSessionDate());
        assertEquals(LocalTime.of(8, 0), created.getStartTime());
        assertEquals(LocalTime.of(10, 0), created.getEndTime());
        assertEquals(1, mockScheduleDAO.auditLogs.size(), "Phải ghi nhận 1 audit log cho thao tác tạo lịch");
    }

    // -------------------------------------------------------------------------
    // 2. Trùng giảng viên cùng khung giờ bị chặn
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 2: Trùng giảng viên cùng khung giờ bị chặn lưu lịch (HTTP 409)")
    void testInstructorConflictBlocked() {
        // Đã có lịch của Giảng viên 3 ở Phòng 1 từ 09:00 đến 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Yêu cầu xếp lịch Giảng viên 3 ở Phòng 2 (khác phòng) từ 08:30 đến 10:00 (giao nhau 09:00 - 10:00)
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T08:30:00", "2026-11-01T10:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        ConflictCheckResult result = ex.getConflictResult();
        assertTrue(result.isHasConflict(), "Phải báo có xung đột");
        assertEquals(1, result.getConflicts().size(), "Phải có đúng 1 xung đột");
        assertEquals(ConflictType.INSTRUCTOR_CONFLICT.getCode(), result.getConflicts().get(0).getType());
        assertTrue(result.getConflicts().get(0).getMessage().contains("Trần Thị Giảng"));
    }

    // -------------------------------------------------------------------------
    // 3. Trùng phòng cùng khung giờ bị chặn
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 3: Trùng phòng cùng khung giờ bị chặn lưu lịch (HTTP 409)")
    void testRoomConflictBlocked() {
        // Đã có lịch ở Phòng 1 của Giảng viên 3 từ 09:00 đến 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Yêu cầu xếp lịch cho Giảng viên 4 (khác giảng viên) ở Phòng 1 từ 10:00 đến 12:00 (giao nhau 10:00 - 11:00)
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 1,
                "2026-11-01T10:00:00", "2026-11-01T12:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        ConflictCheckResult result = ex.getConflictResult();
        assertTrue(result.isHasConflict());
        assertEquals(1, result.getConflicts().size());
        assertEquals(ConflictType.ROOM_CONFLICT.getCode(), result.getConflicts().get(0).getType());
        assertTrue(result.getConflicts().get(0).getMessage().contains("PH-101"));
    }

    // -------------------------------------------------------------------------
    // 4. Trùng cả giảng viên và phòng trả đủ thông tin xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 4: Trùng cả giảng viên và phòng trả đủ cả 2 thông tin xung đột")
    void testBothInstructorAndRoomConflictReported() {
        // Buổi 1: Giảng viên 3, Phòng 1, 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Yêu cầu xếp cùng Giảng viên 3 VÀ cùng Phòng 1 từ 09:30 đến 11:30
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertTrue(result.isHasConflict());
        assertEquals(2, result.getConflicts().size(), "Phải trả về 2 xung đột: 1 cho GV, 1 cho Phòng");

        List<String> types = result.getConflicts().stream().map(ConflictDetail::getType).toList();
        assertTrue(types.contains(ConflictType.INSTRUCTOR_CONFLICT.getCode()));
        assertTrue(types.contains(ConflictType.ROOM_CONFLICT.getCode()));
    }

    // -------------------------------------------------------------------------
    // 5. Hai lịch nối tiếp nhau không bị coi là xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 5: Hai lịch nối tiếp nhau (buổi trước kết thúc đúng lúc buổi sau bắt đầu) KHÔNG xung đột")
    void testConsecutiveSchedulesNotConflicting() {
        // Buổi 1: 08:00 - 10:00 (Giảng viên 3, Phòng 1)
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(10, 0), "SCHEDULED");

        // Buổi sau: 10:00 - 12:00 (Cùng Giảng viên 3 hoặc cùng Phòng 1)
        ScheduleRequest nextReq = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T10:00:00", "2026-11-01T12:00:00");

        ConflictCheckResult nextResult = service.detectConflicts(nextReq, mockScheduleDAO.sessions);
        assertFalse(nextResult.isHasConflict(), "Nối tiếp sau 10:00 không được báo xung đột");

        // Buổi trước: 06:00 - 08:00 (Cùng Giảng viên 3 hoặc cùng Phòng 1)
        ScheduleRequest prevReq = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T06:00:00", "2026-11-01T08:00:00");

        ConflictCheckResult prevResult = service.detectConflicts(prevReq, mockScheduleDAO.sessions);
        assertFalse(prevResult.isHasConflict(), "Nối tiếp trước 08:00 không được báo xung đột");
    }

    // -------------------------------------------------------------------------
    // 6. Lịch giao nhau một phần bị phát hiện
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 6: Lịch giao nhau một phần (bắt đầu trước kết thúc giữa chừng, hoặc bắt đầu giữa chừng) bị phát hiện")
    void testPartialOverlapDetected() {
        // Buổi hiện tại: 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Trường hợp a: 08:00 - 09:30 (bắt đầu trước, kết thúc giữa chừng)
        ScheduleRequest reqA = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T08:00:00", "2026-11-01T09:30:00");
        assertTrue(service.detectConflicts(reqA, mockScheduleDAO.sessions).isHasConflict());

        // Trường hợp b: 10:30 - 12:00 (bắt đầu giữa chừng, kết thúc sau)
        ScheduleRequest reqB = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T10:30:00", "2026-11-01T12:00:00");
        assertTrue(service.detectConflicts(reqB, mockScheduleDAO.sessions).isHasConflict());
    }

    // -------------------------------------------------------------------------
    // 7. Lịch lồng hoàn toàn vào lịch khác bị phát hiện
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 7: Lịch lồng hoàn toàn bên trong hoặc bao trùm hoàn toàn lịch khác bị phát hiện")
    void testCompleteNestingDetected() {
        // Buổi 1: 08:00 - 12:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(8, 0), LocalTime.of(12, 0), "SCHEDULED");

        // Lồng bên trong: 09:00 - 11:00
        ScheduleRequest innerReq = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");
        assertTrue(service.detectConflicts(innerReq, mockScheduleDAO.sessions).isHasConflict());

        // Bao trùm bên ngoài: 07:00 - 13:00
        ScheduleRequest outerReq = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T07:00:00", "2026-11-01T13:00:00");
        assertTrue(service.detectConflicts(outerReq, mockScheduleDAO.sessions).isHasConflict());
    }

    // -------------------------------------------------------------------------
    // 8. Cập nhật lịch không so sánh với chính nó
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 8: Cập nhật lịch không so sánh với chính nó (loại trừ scheduleId)")
    void testUpdateDoesNotCompareWithItself() throws SQLException {
        // Buổi học ID 15: 09:00 - 11:00
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Cập nhật buổi học ID 15 dời giờ sang 09:00 - 11:30 (vẫn giảng viên 3, phòng 1)
        ScheduleRequest req = new ScheduleRequest(2L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T11:30:00");

        ClassSession updated = service.updateSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, 15L, req);

        assertNotNull(updated);
        assertEquals(LocalTime.of(11, 30), updated.getEndTime());
    }

    // -------------------------------------------------------------------------
    // 9. Cập nhật tạo ra xung đột với buổi khác bị từ chối
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 9: Cập nhật tạo ra xung đột với buổi học KHÁC bị từ chối")
    void testUpdateCausingConflictIsRejected() {
        // Buổi học 15: 09:00 - 11:00 (GV 3, Phòng 1)
        addExistingSession(15L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");
        // Buổi học 16: 14:00 - 16:00 (GV 3, Phòng 2)
        addExistingSession(16L, 1L, 3L, 2, LocalTime.of(14, 0), LocalTime.of(16, 0), "SCHEDULED");

        // Thử cập nhật Buổi 15 dời sang 14:30 - 16:30 -> Đụng Giảng viên 3 ở Buổi 16
        ScheduleRequest req = new ScheduleRequest(2L, 3L, 1,
                "2026-11-01T14:30:00", "2026-11-01T16:30:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.updateSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, 15L, req));

        assertTrue(ex.getConflictResult().isHasConflict());
        assertEquals(16L, ex.getConflictResult().getConflicts().get(0).getConflictingSchedule().get("scheduleId"));
    }

    // -------------------------------------------------------------------------
    // 10. Giảng viên khác nhau và phòng khác nhau không bị báo nhầm
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 10: Giảng viên khác và phòng khác trong cùng khung giờ không bị báo nhầm xung đột")
    void testDifferentInstructorAndRoomNotReported() {
        // Buổi 1: Giảng viên 3, Phòng 1, 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Buổi mới: Giảng viên 4, Phòng 2, 09:00 - 11:00 (khác cả GV lẫn phòng)
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Khác cả giảng viên lẫn phòng học không được báo xung đột");
    }

    // -------------------------------------------------------------------------
    // 11. Lịch đã hủy không bị tính là xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 11: Buổi học có trạng thái CANCELLED không bị tính là xung đột")
    void testCancelledScheduleExcludedFromConflict() {
        // Buổi 1: Giảng viên 3, Phòng 1, 09:00 - 11:00 nhưng trạng thái CANCELLED
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "CANCELLED");

        // Buổi mới: Giảng viên 3, Phòng 1, 09:00 - 11:00
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Buổi học đã hủy không được tính là chiếm dụng lịch");
    }

    // -------------------------------------------------------------------------
    // 12. Người không có quyền không thể ghi đè
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 12: Người không có vai trò Quản lý đào tạo (ví dụ Giảng viên/Học viên) không thể ghi đè xung đột (HTTP 403)")
    void testUnauthorizedUserCannotOverrideConflict() {
        // Đã có lịch 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Request cố tình truyền overrideConflict = true
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");
        req.setOverrideConflict(true);
        req.setOverrideReason("Tự ý ghi đè");

        // Giảng viên thử ghi đè -> Phải bị từ chối 403 Forbidden
        ForbiddenAccessException ex = assertThrows(ForbiddenAccessException.class, () ->
                service.createSchedule(3L, INSTRUCTOR_ROLES, req));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Training Manager") || ex.getMessage().contains("Quản lý đào tạo"));
    }

    // -------------------------------------------------------------------------
    // 13. Ghi đè hợp lệ phải có audit log
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 13: Quản lý đào tạo ghi đè hợp lệ kèm lý do được lưu thành công và ghi nhận Audit Log")
    void testAuthorizedOverrideRecordsAuditLog() throws SQLException {
        // Đã có lịch 09:00 - 11:00
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:30:00", "2026-11-01T11:30:00");
        req.setOverrideConflict(true);
        req.setOverrideReason("Giảng viên thỏa thuận dạy ghép bù theo phê duyệt của Ban giám đốc");

        ClassSession created = service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);

        assertNotNull(created, "Ghi đè hợp lệ phải lưu được buổi học");
        assertEquals(1, mockScheduleDAO.auditLogs.size(), "Phải ghi nhận 1 bản ghi audit log");
        AuditLog log = mockScheduleDAO.auditLogs.get(0);
        assertEquals("OVERRIDE_SCHEDULE_CONFLICT", log.getAction());
        assertEquals("SCHEDULE", log.getEntityType());
        assertEquals(String.valueOf(created.getId()), log.getEntityId());
        assertTrue(log.getNewValues().contains("Ban giám đốc"));
    }

    // -------------------------------------------------------------------------
    // 14. Kiểm tra hai request đồng thời tạo lịch trùng nhau (Concurrency / Race condition)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 14: Hai request đồng thời tạo lịch trùng nhau - chỉ 1 request thành công, request còn lại bị chặn xung đột")
    void testConcurrentRequestsRaceConditionHandled() throws Exception {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CyclicBarrier barrier = new CyclicBarrier(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final long classId = i + 1;
            tasks.add(() -> {
                barrier.await(); // Đảm bảo cả 2 luồng cùng xuất phát đồng thời
                ScheduleRequest req = new ScheduleRequest(classId, 3L, 1,
                        "2026-11-01T09:00:00", "2026-11-01T11:00:00");
                try {
                    service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req);
                    successCount.incrementAndGet();
                } catch (ScheduleConflictException e) {
                    conflictCount.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 buổi học được tạo thành công");
        assertEquals(1, conflictCount.get(), "Buổi học thứ hai bị phát hiện và chặn vì xung đột");
    }

    // -------------------------------------------------------------------------
    // 15. Lỗi lưu dữ liệu không để lại lịch ở trạng thái dở dang (Rollback)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 15: Lỗi khi xử lý lưu dữ liệu phải rollback, không để lại bản ghi dở dang")
    void testDataConsistencyRollbackOnError() {
        mockScheduleDAO.setSimulateDatabaseError(true);

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(SQLException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        assertEquals(0, mockScheduleDAO.sessions.size(), "Dữ liệu phải sạch, không có lịch dở dang sau rollback");
    }

    // -------------------------------------------------------------------------
    // Test bổ sung: Hai lịch cùng giảng viên và trùng hoàn toàn thời gian
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 2: Hai lịch cùng giảng viên và trùng hoàn toàn thời gian bị chặn")
    void testExactTimeInstructorConflict() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-01T09:00:00", "2026-11-01T11:00:00");

        ScheduleConflictException ex = assertThrows(ScheduleConflictException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));

        assertTrue(ex.getConflictResult().isHasConflict());
        assertEquals(ConflictType.INSTRUCTOR_CONFLICT.getCode(), ex.getConflictResult().getConflicts().get(0).getType());
    }

    // -------------------------------------------------------------------------
    // Tình huống 8: Cùng giảng viên nhưng khác ngày: không xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 8: Cùng giảng viên nhưng khác ngày -> không xung đột")
    void testSameInstructorDifferentDateNotConflicting() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Ngày 2026-11-02 cùng giờ 09:00 - 11:00 cùng giảng viên 3
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 2,
                "2026-11-02T09:00:00", "2026-11-02T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Khác ngày học không được tính là xung đột lịch");
    }

    // -------------------------------------------------------------------------
    // Tình huống 9: Cùng phòng nhưng khác ngày: không xung đột
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 9: Cùng phòng nhưng khác ngày -> không xung đột")
    void testSameRoomDifferentDateNotConflicting() {
        addExistingSession(10L, 2L, 3L, 1, LocalTime.of(9, 0), LocalTime.of(11, 0), "SCHEDULED");

        // Ngày 2026-11-02 cùng giờ 09:00 - 11:00 cùng phòng 1
        ScheduleRequest req = new ScheduleRequest(1L, 4L, 1,
                "2026-11-02T09:00:00", "2026-11-02T11:00:00");

        ConflictCheckResult result = service.detectConflicts(req, mockScheduleDAO.sessions);
        assertFalse(result.isHasConflict(), "Khác ngày học không được tính là xung đột phòng");
    }

    // -------------------------------------------------------------------------
    // Tình huống 12: Dữ liệu có thời gian kết thúc trước hoặc bằng thời gian bắt đầu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 12a: Thời gian kết thúc < thời gian bắt đầu -> từ chối (HTTP 400)")
    void testEndTimeBeforeStartTimeThrowsValidationException() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T10:00:00", "2026-11-01T08:00:00");

        ValidationException ex = assertThrows(ValidationException.class, req::validate);
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    @DisplayName("Tình huống 12b: Thời gian kết thúc = thời gian bắt đầu -> từ chối (HTTP 400)")
    void testEndTimeEqualsStartTimeThrowsValidationException() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 1,
                "2026-11-01T09:00:00", "2026-11-01T09:00:00");

        ValidationException ex = assertThrows(ValidationException.class, req::validate);
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }

    // -------------------------------------------------------------------------
    // Tình huống 13: Giảng viên hoặc phòng không tồn tại -> trả lỗi ResourceNotFoundException
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Tình huống 13a: Giảng viên không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentInstructorThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(1L, 9999L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    @Test
    @DisplayName("Tình huống 13b: Phòng học không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentRoomThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(1L, 3L, 9999,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    @Test
    @DisplayName("Tình huống 13c: Lớp học không tồn tại -> ném ResourceNotFoundException")
    void testNonExistentClassThrowsNotFound() {
        ScheduleRequest req = new ScheduleRequest(9999L, 3L, 1,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        assertThrows(com.ems.exception.ResourceNotFoundException.class, () ->
                service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, req));
    }

    private void addExistingSession(Long id, Long classId, Long instructorId, Integer roomId,
                                    LocalTime start, LocalTime end, String status) {
        ClassSession s = new ClassSession();
        s.setId(id);
        s.setClassId(classId);
        s.setClassCode("LỚP-TEST-" + classId);
        s.setClassName("Tên lớp " + classId);
        s.setInstructorId(instructorId);
        s.setInstructorName("Trần Thị Giảng");
        s.setClassroomId(roomId);
        s.setRoomCode("PH-10" + roomId);
        s.setSessionDate(TEST_DATE);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setStatus(status);
        mockScheduleDAO.sessions.add(s);
    }

    // =========================================================================
    // Mock DAOs cho môi trường Test độc lập, tốc độ cao, thread-safe
    // =========================================================================
    static class MockScheduleDAO extends ScheduleDAO {
        final List<ClassSession> sessions = new CopyOnWriteArrayList<>();
        final List<AuditLog> auditLogs = new CopyOnWriteArrayList<>();
        final AtomicLong idGen = new AtomicLong(100);
        private volatile boolean simulateDatabaseError = false;

        public void setSimulateDatabaseError(boolean val) {
            this.simulateDatabaseError = val;
        }

        @Override
        public synchronized List<ClassSession> findConflictingSessions(
                Connection connection, Long excludeScheduleId, Long instructorId,
                Integer roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
            List<ClassSession> result = new ArrayList<>();
            for (ClassSession s : sessions) {
                if (excludeScheduleId != null && excludeScheduleId.equals(s.getId())) continue;
                if ("CANCELLED".equalsIgnoreCase(s.getStatus())) continue;
                if (!date.equals(s.getSessionDate())) continue;

                boolean overlap = s.getStartTime().isBefore(endTime) && s.getEndTime().isAfter(startTime);
                if (overlap) {
                    if ((instructorId != null && instructorId.equals(s.getInstructorId()))
                            || (roomId != null && roomId.equals(s.getClassroomId()))) {
                        result.add(s);
                    }
                }
            }
            return result;
        }

        @Override
        public synchronized long createSchedule(Connection connection, ClassSession session) throws SQLException {
            if (simulateDatabaseError) {
                throw new SQLException("Simulated database failure during insert");
            }
            long id = idGen.incrementAndGet();
            session.setId(id);
            sessions.add(session);
            return id;
        }

        @Override
        public synchronized boolean updateSchedule(Connection connection, ClassSession session) {
            for (int i = 0; i < sessions.size(); i++) {
                if (sessions.get(i).getId().equals(session.getId())) {
                    sessions.set(i, session);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized ClassSession findById(Connection connection, long id) {
            return sessions.stream().filter(s -> s.getId().equals(id)).findFirst().orElse(null);
        }

        private final Set<Long> attendedSessionIds = ConcurrentHashMap.newKeySet();
        private final Map<Long, List<Long>> enrolledStudents = new ConcurrentHashMap<>();
        public final List<com.ems.model.Notification> sentNotifications = Collections.synchronizedList(new ArrayList<>());
        public final List<com.ems.model.ClassSessionChangeLog> changeLogs = Collections.synchronizedList(new ArrayList<>());

        public void markAttended(long sessionId) {
            attendedSessionIds.add(sessionId);
        }

        public void addEnrolledStudent(long classId, long studentUserId) {
            enrolledStudents.computeIfAbsent(classId, k -> new CopyOnWriteArrayList<>()).add(studentUserId);
        }

        @Override
        public boolean hasAttendance(Connection connection, long sessionId) {
            return attendedSessionIds.contains(sessionId);
        }

        @Override
        public boolean hasAttendance(long sessionId) {
            return attendedSessionIds.contains(sessionId);
        }

        @Override
        public List<Long> getEnrolledStudentUserIds(Connection connection, long classId) {
            return enrolledStudents.getOrDefault(classId, List.of());
        }

        @Override
        public List<Long> getEnrolledStudentUserIds(long classId) {
            return enrolledStudents.getOrDefault(classId, List.of());
        }

        @Override
        public int sendNotificationToStudents(Connection connection, List<Long> recipientUserIds, Long senderId,
                                              String title, String content, String type, String linkUrl) {
            if (recipientUserIds == null) return 0;
            for (Long rId : recipientUserIds) {
                sentNotifications.add(new com.ems.model.Notification(rId, senderId, title, content, type, linkUrl));
            }
            return recipientUserIds.size();
        }

        @Override
        public void recordChangeLog(Connection connection, long sessionId, String actionType,
                                    long performedBy, String reason, String oldValues, String newValues) {
            changeLogs.add(new com.ems.model.ClassSessionChangeLog(sessionId, actionType, performedBy, reason, oldValues, newValues));
        }

        @Override
        public synchronized boolean cancelSchedule(Connection connection, long id, String cancelReason) {
            for (ClassSession s : sessions) {
                if (s.getId().equals(id)) {
                    s.setStatus("CANCELLED");
                    s.setCancelReason(cancelReason);
                    return true;
                }
            }
            return false;
        }

        @Override
        public synchronized void recordAuditLog(Connection connection, Long userId, String action,
                                                String entityType, String entityId, String oldVal, String newVal) {
            AuditLog log = new AuditLog(userId, action, entityType, entityId, oldVal, newVal);
            auditLogs.add(log);
        }

        private final java.util.concurrent.locks.ReentrantLock roomInstructorLock = new java.util.concurrent.locks.ReentrantLock();

        @Override
        public void lockInstructorAndRoom(Connection connection, Long instructorId, Integer roomId) {
            roomInstructorLock.lock();
        }

        public void releaseLock() {
            if (roomInstructorLock.isHeldByCurrentThread()) {
                roomInstructorLock.unlock();
            }
        }
    }

    static class MockClassroomDAO extends ClassroomDAO {
        private final Map<Integer, Classroom> map = new ConcurrentHashMap<>();

        public void save(Classroom c) { map.put(c.getId(), c); }

        @Override
        public Classroom findById(Connection conn, int id) { return map.get(id); }
    }

    static class MockClassDAO extends SchoolClassDAO {
        private final Map<Long, SchoolClass> map = new ConcurrentHashMap<>();

        public void save(SchoolClass c) { map.put(c.getId(), c); }

        @Override
        public SchoolClass findById(Connection conn, long id) { return map.get(id); }
    }

    static class MockUserDAO extends UserDAO {
        private final Map<Long, User> map = new ConcurrentHashMap<>();

        public void save(User u) { map.put(u.getId(), u); }

        @Override
        public User findById(long id) { return map.get(id); }
    }

    static Connection createMockConnection(MockScheduleDAO dao) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        dao.releaseLock();
                    }
                    if ("isClosed".equals(name)) return false;
                    return null;
                }
        );
    }
}
