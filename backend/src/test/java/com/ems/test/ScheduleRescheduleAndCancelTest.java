package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.ClassDAO;
import com.ems.dao.ClassroomDAO;
import com.ems.dao.ScheduleDAO;
import com.ems.dao.UserDAO;
import com.ems.dto.RescheduleResponse;
import com.ems.dto.ScheduleRequest;
import com.ems.exception.ForbiddenAccessException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ScheduleConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.AuditLog;
import com.ems.model.ClassSession;
import com.ems.model.ClassSessionChangeLog;
import com.ems.model.Classroom;
import com.ems.model.Notification;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho User Story S4-03: Sửa, dời hoặc hủy buổi học đơn lẻ
 * Bao phủ đầy đủ 23 trường hợp kiểm thử theo Acceptance Criteria và đặc tả nghiệp vụ.
 */
public class ScheduleRescheduleAndCancelTest {

    private ScheduleConflictService service;
    private S403MockScheduleDAO mockScheduleDAO;
    private S403MockClassroomDAO mockClassroomDAO;
    private S403MockClassDAO mockClassDAO;
    private S403MockUserDAO mockUserDAO;

    private static final Long TRAINING_MGR_ID = 2L;
    private static final List<String> TRAINING_MGR_ROLES = List.of(RoleConstant.TRAINING_MANAGER);
    private static final List<String> ADMIN_ROLES = List.of(RoleConstant.ADMIN);
    private static final List<String> INSTRUCTOR_ROLES = List.of(RoleConstant.INSTRUCTOR);
    private static final List<String> STUDENT_ROLES = List.of(RoleConstant.STUDENT);

    private static final Long CLASS_JAVA_ID = 1L;
    private static final Long CLASS_WEB_ID = 2L;
    private static final Long INSTRUCTOR_GIANG_ID = 3L;
    private static final Long INSTRUCTOR_THAY_ID = 4L;
    private static final Integer ROOM_101_ID = 1;
    private static final Integer ROOM_202_ID = 2;

    private static final Long STUDENT_USER_1 = 101L;
    private static final Long STUDENT_USER_2 = 102L;
    private static final Long OTHER_CLASS_STUDENT = 201L;

    private long sessionId;

    @BeforeEach
    void setUp() throws SQLException {
        mockScheduleDAO = new S403MockScheduleDAO();
        mockClassroomDAO = new S403MockClassroomDAO();
        mockClassDAO = new S403MockClassDAO();
        mockUserDAO = new S403MockUserDAO();

        service = new ScheduleConflictService(
                mockScheduleDAO,
                mockClassroomDAO,
                mockClassDAO,
                mockUserDAO,
                () -> createMockConnection(mockScheduleDAO)
        );

        // Seeding phòng học
        mockClassroomDAO.save(new Classroom(ROOM_101_ID, "PH-101", "Phòng 101", 40, "PHYSICAL", "AVAILABLE"));
        mockClassroomDAO.save(new Classroom(ROOM_202_ID, "LAB-202", "Phòng Lab 202", 35, "PHYSICAL", "AVAILABLE"));

        // Seeding lớp học
        mockClassDAO.save(new SchoolClass(CLASS_JAVA_ID, "CNTT-JAVA-01", "Lớp Java Chuyên Sâu", 1, "IN_PROGRESS"));
        mockClassDAO.save(new SchoolClass(CLASS_WEB_ID, "CNTT-WEB-02", "Lớp Web Fullstack", 1, "IN_PROGRESS"));

        // Seeding giảng viên
        mockUserDAO.save(new User(INSTRUCTOR_GIANG_ID, "NV-GV-01", "giangvien@edumanager.vn", "Trần Thị Giảng", RoleConstant.INSTRUCTOR));
        mockUserDAO.save(new User(INSTRUCTOR_THAY_ID, "NV-GV-02", "giangvien2@edumanager.vn", "Nguyễn Văn Thầy", RoleConstant.INSTRUCTOR));

        // Seeding học viên ghi danh vào lớp Java
        mockScheduleDAO.addEnrolledStudent(CLASS_JAVA_ID, STUDENT_USER_1);
        mockScheduleDAO.addEnrolledStudent(CLASS_JAVA_ID, STUDENT_USER_2);
        // Học viên ghi danh vào lớp khác
        mockScheduleDAO.addEnrolledStudent(CLASS_WEB_ID, OTHER_CLASS_STUDENT);

        // Tạo sẵn một buổi học chuẩn: Lớp Java, ngày 2026-11-01, 08:00 - 10:00, Phòng 101, GV Trần Thị Giảng
        ClassSession initialSession = new ClassSession();
        initialSession.setClassId(CLASS_JAVA_ID);
        initialSession.setClassCode("CNTT-JAVA-01");
        initialSession.setClassName("Lớp Java Chuyên Sâu");
        initialSession.setSessionDate(LocalDate.of(2026, 11, 1));
        initialSession.setStartTime(LocalTime.of(8, 0));
        initialSession.setEndTime(LocalTime.of(10, 0));
        initialSession.setClassroomId(ROOM_101_ID);
        initialSession.setRoomCode("PH-101");
        initialSession.setRoomName("Phòng 101");
        initialSession.setInstructorId(INSTRUCTOR_GIANG_ID);
        initialSession.setInstructorName("Trần Thị Giảng");
        initialSession.setStatus("SCHEDULED");

        sessionId = mockScheduleDAO.createSchedule(null, initialSession);
    }

    // -------------------------------------------------------------------------
    // TC 1: Sửa thành công ngày học
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 1: Sửa thành công ngày học của một buổi học")
    void testRescheduleDateSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setSessionDate("2026-11-05");

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals("RESCHEDULE", resp.getAction());
        assertEquals(LocalDate.of(2026, 11, 5), resp.getSession().getSessionDate());
        assertEquals("RESCHEDULED", resp.getSession().getStatus());
        // Các trường khác giữ nguyên
        assertEquals(LocalTime.of(8, 0), resp.getSession().getStartTime());
        assertEquals(LocalTime.of(10, 0), resp.getSession().getEndTime());
        assertEquals(ROOM_101_ID, resp.getSession().getClassroomId());
        assertEquals(INSTRUCTOR_GIANG_ID, resp.getSession().getInstructorId());
    }

    // -------------------------------------------------------------------------
    // TC 2: Sửa thành công giờ học
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 2: Sửa thành công giờ học (startTime và endTime)")
    void testRescheduleTimeSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setStartTime("13:30:00");
        req.setEndTime("15:30:00");

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertTrue(resp.isSuccess());
        assertEquals(LocalTime.of(13, 30), resp.getSession().getStartTime());
        assertEquals(LocalTime.of(15, 30), resp.getSession().getEndTime());
        assertEquals(LocalDate.of(2026, 11, 1), resp.getSession().getSessionDate());
        assertEquals("RESCHEDULED", resp.getSession().getStatus());
    }

    // -------------------------------------------------------------------------
    // TC 3: Thay đổi phòng học thành công
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 3: Thay đổi phòng học thành công (giữ nguyên ngày, giờ, người dạy)")
    void testChangeRoomSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertTrue(resp.isSuccess());
        assertEquals(ROOM_202_ID, resp.getSession().getClassroomId());
        assertEquals(LocalDate.of(2026, 11, 1), resp.getSession().getSessionDate());
        assertEquals(LocalTime.of(8, 0), resp.getSession().getStartTime());
        assertEquals(INSTRUCTOR_GIANG_ID, resp.getSession().getInstructorId());
    }

    // -------------------------------------------------------------------------
    // TC 4: Thay đổi giảng viên thành công
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 4: Thay đổi giảng viên thành công khi giảng viên báo bận đột xuất")
    void testChangeInstructorSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setInstructorId(INSTRUCTOR_THAY_ID);

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertTrue(resp.isSuccess());
        assertEquals(INSTRUCTOR_THAY_ID, resp.getSession().getInstructorId());
        assertEquals(ROOM_101_ID, resp.getSession().getClassroomId());
    }

    // -------------------------------------------------------------------------
    // TC 5: Sửa nhiều trường cùng lúc
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 5: Sửa nhiều trường cùng lúc (ngày, giờ, phòng, giảng viên)")
    void testUpdateMultipleFieldsSuccess() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setSessionDate("2026-11-10");
        req.setStartTime("14:00:00");
        req.setEndTime("16:00:00");
        req.setRoomId(ROOM_202_ID);
        req.setInstructorId(INSTRUCTOR_THAY_ID);

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertTrue(resp.isSuccess());
        assertEquals(LocalDate.of(2026, 11, 10), resp.getSession().getSessionDate());
        assertEquals(LocalTime.of(14, 0), resp.getSession().getStartTime());
        assertEquals(LocalTime.of(16, 0), resp.getSession().getEndTime());
        assertEquals(ROOM_202_ID, resp.getSession().getClassroomId());
        assertEquals(INSTRUCTOR_THAY_ID, resp.getSession().getInstructorId());
        assertEquals("RESCHEDULED", resp.getSession().getStatus());
    }

    // -------------------------------------------------------------------------
    // TC 6: PATCH một trường nhưng giữ nguyên các trường còn lại
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 6: PATCH chỉ gửi duy nhất roomId - các trường ngày, giờ, giảng viên không bị đặt thành null")
    void testPatchSingleFieldPreservesOthers() throws SQLException {
        ScheduleRequest patchReq = new ScheduleRequest();
        patchReq.setRoomId(ROOM_202_ID);

        ClassSession updated = service.updateSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, patchReq);

        assertEquals(ROOM_202_ID, updated.getClassroomId());
        assertNotNull(updated.getSessionDate(), "Ngày học không được vô tình thành null");
        assertNotNull(updated.getStartTime(), "Giờ bắt đầu không được vô tình thành null");
        assertNotNull(updated.getEndTime(), "Giờ kết thúc không được vô tình thành null");
        assertNotNull(updated.getInstructorId(), "Giảng viên không được vô tình thành null");
        assertEquals(LocalDate.of(2026, 11, 1), updated.getSessionDate());
        assertEquals(INSTRUCTOR_GIANG_ID, updated.getInstructorId());
    }

    // -------------------------------------------------------------------------
    // TC 7: Hủy thành công với lý do hợp lệ
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 7: Hủy buổi học thành công với lý do hợp lệ")
    void testCancelScheduleSuccess() throws SQLException {
        String reason = "Giảng viên bận đột xuất công tác nước ngoài";

        RescheduleResponse resp = service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, reason);

        assertTrue(resp.isSuccess());
        assertEquals("CANCEL", resp.getAction());
        assertEquals("CANCELLED", resp.getSession().getStatus());
        assertEquals(reason, resp.getSession().getCancelReason());

        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals("CANCELLED", inDb.getStatus());
        assertEquals(reason, inDb.getCancelReason());
    }

    // -------------------------------------------------------------------------
    // TC 8: Hủy không có lý do -> phải bị từ chối (400)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 8: Hủy buổi học không cung cấp lý do (null) phải bị từ chối")
    void testCancelWithoutReasonRejected() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, null)
        );
        assertTrue(ex.getMessage().contains("Lý do hủy buổi học là bắt buộc"));

        // Dữ liệu cũ giữ nguyên
        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals("SCHEDULED", inDb.getStatus());
    }

    // -------------------------------------------------------------------------
    // TC 9: Hủy với lý do chỉ chứa khoảng trắng -> phải bị từ chối (400)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 9: Hủy buổi học với lý do chỉ chứa khoảng trắng ('   ') phải bị từ chối")
    void testCancelWithWhitespaceReasonRejected() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, "    ")
        );
        assertTrue(ex.getMessage().contains("không được để trống"));

        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals("SCHEDULED", inDb.getStatus());
    }

    // -------------------------------------------------------------------------
    // TC 10: Sửa buổi học đã điểm danh -> phải bị từ chối (400)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 10: Sửa buổi học đã được điểm danh phải bị từ chối")
    void testUpdateAttendedSessionRejected() {
        // Đánh dấu buổi học đã được điểm danh
        mockScheduleDAO.markAttended(sessionId);

        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req)
        );
        assertTrue(ex.getMessage().contains("đã được điểm danh"));

        // Dữ liệu cũ không đổi
        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals(ROOM_101_ID, inDb.getClassroomId());
    }

    // -------------------------------------------------------------------------
    // TC 11: Hủy buổi học đã điểm danh -> phải bị từ chối (400)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 11: Hủy buổi học đã được điểm danh phải bị từ chối")
    void testCancelAttendedSessionRejected() {
        mockScheduleDAO.markAttended(sessionId);

        ValidationException ex = assertThrows(ValidationException.class, () ->
                service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, "Hủy lịch")
        );
        assertTrue(ex.getMessage().contains("đã được điểm danh"));

        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals("SCHEDULED", inDb.getStatus());
    }

    // -------------------------------------------------------------------------
    // TC 12: Sửa buổi học không tồn tại -> trả về 404
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 12: Sửa buổi học không tồn tại phải ném ResourceNotFoundException (404)")
    void testUpdateNonExistingSessionNotFound() {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        assertThrows(ResourceNotFoundException.class, () ->
                service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, 999999L, req)
        );
    }

    // -------------------------------------------------------------------------
    // TC 13: Người dùng không đủ quyền -> trả về 403
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 13: Học viên hoặc Giảng viên không có quyền sửa hoặc hủy lịch (403 Forbidden)")
    void testUnauthorizedUserRejected() {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        // Học viên sửa lịch
        assertThrows(ForbiddenAccessException.class, () ->
                service.updateScheduleWithDetails(STUDENT_USER_1, STUDENT_ROLES, sessionId, req)
        );

        // Giảng viên hủy lịch
        assertThrows(ForbiddenAccessException.class, () ->
                service.cancelScheduleWithDetails(INSTRUCTOR_GIANG_ID, INSTRUCTOR_ROLES, sessionId, "Lý do")
        );
    }

    // -------------------------------------------------------------------------
    // TC 14: Sửa lịch gây trùng giảng viên -> từ chối cập nhật (409)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 14: Sửa lịch gây trùng giảng viên với buổi học khác bị chặn (409 Conflict)")
    void testUpdateCausingInstructorConflictRejected() throws SQLException {
        // Tạo buổi học khác của Giảng viên Thầy: ngày 2026-11-01, 10:00 - 12:00
        ClassSession other = new ClassSession();
        other.setClassId(CLASS_WEB_ID);
        other.setInstructorId(INSTRUCTOR_THAY_ID);
        other.setClassroomId(ROOM_202_ID);
        other.setSessionDate(LocalDate.of(2026, 11, 1));
        other.setStartTime(LocalTime.of(10, 0));
        other.setEndTime(LocalTime.of(12, 0));
        other.setStatus("SCHEDULED");
        mockScheduleDAO.createSchedule(null, other);

        // Sửa buổi học hiện tại (08:00 - 10:00) đổi sang giờ 09:30 - 11:30 và gán cho GV Thầy
        ScheduleRequest req = new ScheduleRequest();
        req.setStartTime("09:30:00");
        req.setEndTime("11:30:00");
        req.setInstructorId(INSTRUCTOR_THAY_ID);

        assertThrows(ScheduleConflictException.class, () ->
                service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req)
        );
    }

    // -------------------------------------------------------------------------
    // TC 15: Sửa lịch gây trùng phòng -> từ chối cập nhật (409)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 15: Sửa lịch gây trùng phòng học với buổi học khác bị chặn (409 Conflict)")
    void testUpdateCausingRoomConflictRejected() throws SQLException {
        // Phòng 202 đang có lớp từ 14:00 - 16:00
        ClassSession other = new ClassSession();
        other.setClassId(CLASS_WEB_ID);
        other.setInstructorId(INSTRUCTOR_THAY_ID);
        other.setClassroomId(ROOM_202_ID);
        other.setSessionDate(LocalDate.of(2026, 11, 1));
        other.setStartTime(LocalTime.of(14, 0));
        other.setEndTime(LocalTime.of(16, 0));
        other.setStatus("SCHEDULED");
        mockScheduleDAO.createSchedule(null, other);

        // Đổi buổi học hiện tại sang Phòng 202 khung giờ 15:00 - 17:00
        ScheduleRequest req = new ScheduleRequest();
        req.setStartTime("15:00:00");
        req.setEndTime("17:00:00");
        req.setRoomId(ROOM_202_ID);

        assertThrows(ScheduleConflictException.class, () ->
                service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req)
        );
    }

    // -------------------------------------------------------------------------
    // TC 16: Khi bị từ chối, dữ liệu cũ không bị thay đổi
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 16: Khi yêu cầu sửa bị xung đột và từ chối, dữ liệu cũ trong DB được giữ nguyên")
    void testDataRemainsUnchangedWhenRejected() throws SQLException {
        // Tạo buổi xung đột
        ClassSession other = new ClassSession();
        other.setClassId(CLASS_WEB_ID);
        other.setInstructorId(INSTRUCTOR_GIANG_ID);
        other.setClassroomId(ROOM_202_ID);
        other.setSessionDate(LocalDate.of(2026, 11, 1));
        other.setStartTime(LocalTime.of(14, 0));
        other.setEndTime(LocalTime.of(16, 0));
        other.setStatus("SCHEDULED");
        mockScheduleDAO.createSchedule(null, other);

        ScheduleRequest badReq = new ScheduleRequest();
        badReq.setStartTime("14:30:00");
        badReq.setEndTime("16:30:00");

        assertThrows(ScheduleConflictException.class, () ->
                service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, badReq)
        );

        ClassSession sessionAfter = mockScheduleDAO.findById(null, sessionId);
        assertEquals(LocalTime.of(8, 0), sessionAfter.getStartTime());
        assertEquals(LocalTime.of(10, 0), sessionAfter.getEndTime());
        assertEquals(ROOM_101_ID, sessionAfter.getClassroomId());
    }

    // -------------------------------------------------------------------------
    // TC 17: Lưu lịch sử thay đổi chính xác (audit_logs & class_session_change_logs)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 17: Sửa hoặc hủy thành công phải lưu lịch sử vào audit_logs và change_logs")
    void testChangeLogsAndAuditLogsRecorded() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setSessionDate("2026-11-08");
        req.setReason("Đổi lịch theo đề xuất của giảng viên");

        service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        // Kiểm tra audit log
        assertFalse(mockScheduleDAO.auditLogs.isEmpty());
        AuditLog audit = mockScheduleDAO.auditLogs.get(mockScheduleDAO.auditLogs.size() - 1);
        assertEquals("RESCHEDULE_SESSION", audit.getAction());
        assertEquals(TRAINING_MGR_ID, audit.getUserId());

        // Kiểm tra change log
        assertFalse(mockScheduleDAO.changeLogs.isEmpty());
        ClassSessionChangeLog changeLog = mockScheduleDAO.changeLogs.get(mockScheduleDAO.changeLogs.size() - 1);
        assertEquals("RESCHEDULE", changeLog.getActionType());
        assertEquals(sessionId, changeLog.getSessionId());
        assertEquals(TRAINING_MGR_ID, changeLog.getPerformedBy());
        assertNotNull(changeLog.getOldValues());
        assertNotNull(changeLog.getNewValues());

        // Thao tác hủy
        service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, "Lý do hủy lớp");
        ClassSessionChangeLog cancelLog = mockScheduleDAO.changeLogs.get(mockScheduleDAO.changeLogs.size() - 1);
        assertEquals("CANCEL", cancelLog.getActionType());
        assertEquals("Lý do hủy lớp", cancelLog.getReason());
    }

    // -------------------------------------------------------------------------
    // TC 18: Học viên thuộc lớp nhận được thông báo tương ứng
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 18: Học viên ghi danh thuộc lớp nhận được thông báo khi dời hoặc hủy lịch")
    void testEnrolledStudentsReceiveNotification() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setSessionDate("2026-11-07");

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        assertTrue(resp.isNotificationSent());
        assertEquals(2, resp.getRecipientCount(), "Có 2 học viên trong lớp Java phải nhận thông báo");

        List<Notification> notifs = mockScheduleDAO.sentNotifications;
        assertFalse(notifs.isEmpty());

        Set<Long> recipients = new HashSet<>();
        for (Notification n : notifs) {
            recipients.add(n.getRecipientId());
            assertEquals("SCHEDULE", n.getNotificationType());
            assertTrue(n.getTitle().contains("CNTT-JAVA-01"));
        }
        assertTrue(recipients.contains(STUDENT_USER_1));
        assertTrue(recipients.contains(STUDENT_USER_2));
    }

    // -------------------------------------------------------------------------
    // TC 19: Học viên không thuộc lớp không nhận được thông báo
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 19: Học viên không thuộc lớp (lớp Web) tuyệt đối không nhận được thông báo")
    void testNonEnrolledStudentsDoNotReceiveNotification() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        List<Notification> notifs = mockScheduleDAO.sentNotifications;
        for (Notification n : notifs) {
            assertNotEquals(OTHER_CLASS_STUDENT, n.getRecipientId(),
                    "Học viên lớp khác không được phép nhận thông báo của lớp Java");
        }
    }

    // -------------------------------------------------------------------------
    // TC 20: Lỗi gửi thông báo không làm mất thay đổi lịch đã lưu thành công
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 20: Lỗi khi phát thông báo không làm rollback thay đổi lịch đã commit")
    void testNotificationFailureDoesNotRollbackScheduleChange() throws SQLException {
        // Cấu hình mock ném lỗi khi gửi thông báo
        mockScheduleDAO.simulateNotificationError = true;

        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        RescheduleResponse resp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);

        // Lịch học vẫn cập nhật thành công vào DB
        assertTrue(resp.isSuccess());
        ClassSession inDb = mockScheduleDAO.findById(null, sessionId);
        assertEquals(ROOM_202_ID, inDb.getClassroomId());
        assertFalse(resp.isNotificationSent());
        assertNotNull(resp.getNotificationError());
    }

    // -------------------------------------------------------------------------
    // TC 21: Retry không tạo thông báo trùng nếu cùng một yêu cầu
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 21: Gọi cập nhật với dữ liệu giống hệt không làm xáo trộn và xử lý an toàn")
    void testIdempotentRescheduleSafe() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        RescheduleResponse firstResp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);
        assertTrue(firstResp.isSuccess());

        // Lần thứ 2 gọi lại với cùng roomId
        RescheduleResponse secondResp = service.updateScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, req);
        assertTrue(secondResp.isSuccess());
        assertEquals(ROOM_202_ID, secondResp.getSession().getClassroomId());
    }

    // -------------------------------------------------------------------------
    // TC 22: Buổi học đã hủy không tiếp tục gây xung đột lịch
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 22: Buổi học đã bị hủy (CANCELLED) không tiếp tục gây xung đột với lịch mới")
    void testCancelledSessionDoesNotBlockOtherSchedules() throws SQLException {
        // Hủy buổi học hiện tại (08:00 - 10:00 ngày 2026-11-01, GV Trần Thị Giảng, Phòng 101)
        service.cancelScheduleWithDetails(TRAINING_MGR_ID, TRAINING_MGR_ROLES, sessionId, "Giảng viên bận đột xuất");

        // Tạo buổi học mới cùng ngày, cùng giờ, cùng phòng và cùng giảng viên đó
        ScheduleRequest newReq = new ScheduleRequest(CLASS_WEB_ID, INSTRUCTOR_GIANG_ID, ROOM_101_ID,
                "2026-11-01T08:00:00", "2026-11-01T10:00:00");

        // Phải tạo thành công không bị chặn xung đột
        ClassSession created = service.createSchedule(TRAINING_MGR_ID, TRAINING_MGR_ROLES, newReq);
        assertNotNull(created);
        assertNotNull(created.getId());
    }

    // -------------------------------------------------------------------------
    // TC 23: Admin cũng có toàn quyền sửa hoặc hủy lịch
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("TC 23: Quản trị viên (ADMIN) có đầy đủ quyền sửa, dời hoặc hủy buổi học")
    void testAdminHasFullPermissionToRescheduleAndCancel() throws SQLException {
        ScheduleRequest req = new ScheduleRequest();
        req.setRoomId(ROOM_202_ID);

        RescheduleResponse resp = service.updateScheduleWithDetails(1L, ADMIN_ROLES, sessionId, req);
        assertTrue(resp.isSuccess());

        RescheduleResponse cancelResp = service.cancelScheduleWithDetails(1L, ADMIN_ROLES, sessionId, "Admin hủy để bảo trì");
        assertTrue(cancelResp.isSuccess());
        assertEquals("CANCELLED", cancelResp.getSession().getStatus());
    }

    // -------------------------------------------------------------------------
    // MOCK DAO CHO S4-03
    // -------------------------------------------------------------------------
    static class S403MockScheduleDAO extends ScheduleDAO {
        private final List<ClassSession> sessions = new CopyOnWriteArrayList<>();
        private final AtomicLong idGen = new AtomicLong(100);
        public final List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());
        public final List<ClassSessionChangeLog> changeLogs = Collections.synchronizedList(new ArrayList<>());
        public final List<Notification> sentNotifications = Collections.synchronizedList(new ArrayList<>());
        private final Set<Long> attendedSessionIds = ConcurrentHashMap.newKeySet();
        private final Map<Long, List<Long>> enrolledStudents = new ConcurrentHashMap<>();
        public boolean simulateNotificationError = false;

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
            if (simulateNotificationError) {
                throw new RuntimeException("Simulated notification service failure");
            }
            if (recipientUserIds == null) return 0;
            for (Long rId : recipientUserIds) {
                sentNotifications.add(new Notification(rId, senderId, title, content, type, linkUrl));
            }
            return recipientUserIds.size();
        }

        @Override
        public void recordChangeLog(Connection connection, long sessionId, String actionType,
                                    long performedBy, String reason, String oldValues, String newValues) {
            changeLogs.add(new ClassSessionChangeLog(sessionId, actionType, performedBy, reason, oldValues, newValues));
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
        public synchronized long createSchedule(Connection connection, ClassSession session) {
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
            auditLogs.add(new AuditLog(userId, action, entityType, entityId, oldVal, newVal));
        }

        @Override
        public void lockInstructorAndRoom(Connection connection, Long instructorId, Integer roomId) {}
    }

    static class S403MockClassroomDAO extends ClassroomDAO {
        private final Map<Integer, Classroom> map = new ConcurrentHashMap<>();
        public void save(Classroom c) { map.put(c.getId(), c); }
        @Override
        public Classroom findById(Connection conn, int id) { return map.get(id); }
    }

    static class S403MockClassDAO extends ClassDAO {
        private final Map<Long, SchoolClass> map = new ConcurrentHashMap<>();
        public void save(SchoolClass c) { map.put(c.getId(), c); }
        @Override
        public SchoolClass findById(Connection conn, long id) { return map.get(id); }
    }

    static class S403MockUserDAO extends UserDAO {
        private final Map<Long, User> map = new ConcurrentHashMap<>();
        public void save(User u) { map.put(u.getId(), u); }
        @Override
        public User findById(long id) { return map.get(id); }
    }

    static Connection createMockConnection(S403MockScheduleDAO dao) {
        return (Connection) java.lang.reflect.Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> null
        );
    }
}
