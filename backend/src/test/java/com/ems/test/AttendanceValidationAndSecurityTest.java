package com.ems.test;

import com.ems.constant.RoleConstant;
import com.ems.dao.AttendanceDAO;
import com.ems.dto.AttendanceSheetResponse;
import com.ems.dto.SaveAttendanceRequest;
import com.ems.model.AttendanceAuditLog;
import com.ems.model.AttendanceRecord;
import com.ems.model.AttendanceStatus;
import com.ems.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bộ kiểm thử toàn diện cho chức năng S4-07 (Jira EP-05): Điểm danh học viên cho một buổi học.
 * Kiểm tra 5 tiêu chí chấp nhận (Acceptance Criteria):
 *  - AC1: Bốn trạng thái: Có mặt, Đi muộn, Vắng có phép, Vắng không phép
 *  - AC2: Thao tác đánh dấu tất cả có mặt và chỉnh trường hợp ngoại lệ
 *  - AC3: Phân quyền điểm danh hôm nay vs quá khứ vs chặn buổi tương lai
 *  - AC4: Ghi nhật ký kiểm toán (audit log) khi sửa điểm danh đã lưu
 *  - AC5: Cấu trúc dữ liệu phản hồi tinh gọn, hỗ trợ hiển thị tối ưu trên màn hình 360px
 */
class AttendanceValidationAndSecurityTest {

    private AttendanceDAO attendanceDAO;
    private User instructor;
    private User trainingManager;
    private User admin;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        attendanceDAO = new AttendanceDAO();
        AttendanceDAO.initSampleData();

        today = LocalDate.now();

        instructor = new User(2L, "VU DUC LONG", "long.vd@tms.vn", "ACTIVE", List.of(RoleConstant.INSTRUCTOR));
        trainingManager = new User(3L, "NGUYEN VAN PHONG", "phong.nv@tms.vn", "ACTIVE", List.of(RoleConstant.TRAINING_MANAGER));
        admin = new User(1L, "ADMINISTRATOR", "admin@tms.vn", "ACTIVE", List.of(RoleConstant.ADMIN));
    }

    // ==========================================
    // AC1: BỐN TRẠNG THÁI ĐIỂM DANH HỢP LỆ
    // ==========================================

    @Test
    void testAC1_AttendanceStatusEnumContainsFourRequiredStates() {
        assertEquals("Có mặt", AttendanceStatus.PRESENT.getLabel());
        assertEquals("Đi muộn", AttendanceStatus.LATE.getLabel());
        assertEquals("Vắng có phép", AttendanceStatus.EXCUSED_ABSENCE.getLabel());
        assertEquals("Vắng không phép", AttendanceStatus.UNEXCUSED_ABSENCE.getLabel());
    }

    @Test
    void testAC1_AttendanceStatusParsingFlexibility() {
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.from("Có mặt"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.from("PRESENT"));
        assertEquals(AttendanceStatus.PRESENT, AttendanceStatus.from("Co mat"));

        assertEquals(AttendanceStatus.LATE, AttendanceStatus.from("Đi muộn"));
        assertEquals(AttendanceStatus.LATE, AttendanceStatus.from("LATE"));
        assertEquals(AttendanceStatus.LATE, AttendanceStatus.from("Muộn"));

        assertEquals(AttendanceStatus.EXCUSED_ABSENCE, AttendanceStatus.from("Vắng có phép"));
        assertEquals(AttendanceStatus.EXCUSED_ABSENCE, AttendanceStatus.from("EXCUSED_ABSENCE"));
        assertEquals(AttendanceStatus.EXCUSED_ABSENCE, AttendanceStatus.from("Có phép"));

        assertEquals(AttendanceStatus.UNEXCUSED_ABSENCE, AttendanceStatus.from("Vắng không phép"));
        assertEquals(AttendanceStatus.UNEXCUSED_ABSENCE, AttendanceStatus.from("UNEXCUSED_ABSENCE"));
        assertEquals(AttendanceStatus.UNEXCUSED_ABSENCE, AttendanceStatus.from("Vắng"));
    }

    @Test
    void testAC1_InvalidStatusThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AttendanceStatus.from("TRANG_THAI_LA_HOAN_TOAN"));
        assertTrue(ex.getMessage().contains("Trạng thái điểm danh không hợp lệ"));
    }

    // ==========================================
    // AC2: ĐÁNH DẤU TẤT CẢ CÓ MẶT RỒI CHỈNH NGOẠI LỆ
    // ==========================================

    @Test
    void testAC2_MarkAllPresentThenAdjustExceptions() throws SQLException {
        Long sessionId = 401L; // Buổi hôm nay

        // 1. Thao tác nhanh 1 chạm: Đánh dấu tất cả có mặt
        AttendanceSheetResponse batchResp = attendanceDAO.markAllPresent(sessionId, instructor, today);
        assertNotNull(batchResp);
        assertEquals(5, batchResp.getTotalStudents());
        assertEquals(5, batchResp.getPresentCount());
        assertEquals(0, batchResp.getLateCount());
        assertEquals(0, batchResp.getExcusedAbsenceCount());
        assertEquals(0, batchResp.getUnexcusedAbsenceCount());
        assertEquals("100.0%", batchResp.getAttendanceRate());

        // 2. Chỉnh 2 trường hợp ngoại lệ: 1 bạn đi muộn, 1 bạn vắng có phép
        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionId);
        List<SaveAttendanceRequest.AttendanceItem> items = new ArrayList<>();

        for (AttendanceSheetResponse.StudentAttendanceItem s : batchResp.getStudents()) {
            if ("HV260102".equals(s.getStudentCode())) {
                items.add(new SaveAttendanceRequest.AttendanceItem(s.getStudentId(), "Đi muộn", "Đến trễ 15 phút kẹt xe"));
            } else if ("HV260104".equals(s.getStudentCode())) {
                items.add(new SaveAttendanceRequest.AttendanceItem(s.getStudentId(), "Vắng có phép", "Có đơn gửi từ sáng"));
            } else {
                items.add(new SaveAttendanceRequest.AttendanceItem(s.getStudentId(), "Có mặt", ""));
            }
        }
        req.setItems(items);

        AttendanceSheetResponse updatedResp = attendanceDAO.saveAttendance(req, instructor, today);
        assertNotNull(updatedResp);
        assertEquals(3, updatedResp.getPresentCount());
        assertEquals(1, updatedResp.getLateCount());
        assertEquals(1, updatedResp.getExcusedAbsenceCount());
        assertEquals(0, updatedResp.getUnexcusedAbsenceCount());
    }

    // ==========================================
    // AC3: PHÂN QUYỀN HÔM NAY VS QUÁ KHỨ VS BUỔI TƯƠNG LAI
    // ==========================================

    @Test
    void testAC3_InstructorCanTakeAttendanceToday() throws SQLException {
        Long sessionTodayId = 401L;

        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionTodayId);
        List<SaveAttendanceRequest.AttendanceItem> items = List.of(
                new SaveAttendanceRequest.AttendanceItem(1001L, "Có mặt", "Đúng giờ"),
                new SaveAttendanceRequest.AttendanceItem(1002L, "Có mặt", "")
        );
        req.setItems(items);

        AttendanceSheetResponse resp = attendanceDAO.saveAttendance(req, instructor, today);
        assertNotNull(resp);
        assertTrue(resp.isToday());
        assertTrue(resp.isCanEdit());
    }

    @Test
    void testAC3_FutureSessionCannotBeMarkedInAdvance() {
        Long sessionFutureId = 403L; // Buổi 3 ngày sau

        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionFutureId);
        req.setItems(List.of(new SaveAttendanceRequest.AttendanceItem(1001L, "Có mặt", "")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> attendanceDAO.saveAttendance(req, instructor, today));
        assertTrue(ex.getMessage().contains("Chưa tới ngày diễn ra buổi học"));
    }

    @Test
    void testAC3_InstructorBlockedFromEditingPastSession() {
        Long sessionPastId = 402L; // Buổi 2 ngày trước

        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionPastId);
        req.setItems(List.of(new SaveAttendanceRequest.AttendanceItem(1001L, "Có mặt", "")));

        SecurityException ex = assertThrows(SecurityException.class,
                () -> attendanceDAO.saveAttendance(req, instructor, today));
        assertTrue(ex.getMessage().contains("Giảng viên chỉ điểm danh được buổi của ngày hôm nay"));
        assertTrue(ex.getMessage().contains("Sửa buổi quá khứ cần quyền Quản lý đào tạo"));
    }

    @Test
    void testAC3_TrainingManagerMustProvideReasonWhenEditingPastSession() {
        Long sessionPastId = 402L;

        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionPastId);
        req.setItems(List.of(new SaveAttendanceRequest.AttendanceItem(1001L, "Có mặt", "")));
        req.setReason(null); // Thiếu lý do

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> attendanceDAO.saveAttendance(req, trainingManager, today));
        assertTrue(ex.getMessage().contains("Vui lòng nhập lý do sửa"));
    }

    @Test
    void testAC3_TrainingManagerAndAdminCanEditPastSessionWithReason() throws SQLException {
        Long sessionPastId = 402L;

        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionPastId);
        req.setReason("Học viên bổ sung giấy xác nhận y tế");
        req.setItems(List.of(
                new SaveAttendanceRequest.AttendanceItem(1001L, "Có mặt", ""),
                new SaveAttendanceRequest.AttendanceItem(1002L, "Vắng có phép", "Có giấy khám bệnh viện")
        ));

        AttendanceSheetResponse resp = attendanceDAO.saveAttendance(req, trainingManager, today);
        assertNotNull(resp);
        assertTrue(resp.isPast());

        // Kiểm tra với Admin cũng cho phép
        req.setReason("Admin điều chỉnh theo đơn khiếu nại");
        AttendanceSheetResponse adminResp = attendanceDAO.saveAttendance(req, admin, today);
        assertNotNull(adminResp);
    }

    // ==========================================
    // AC4: NHẬT KÝ KIỂM TOÁN (AUDIT LOGS)
    // ==========================================

    @Test
    void testAC4_AuditLogRecordedWhenAttendanceModified() throws SQLException {
        Long sessionId = 401L;

        // Lưu lần 1: Đánh dấu tất cả có mặt
        attendanceDAO.markAllPresent(sessionId, instructor, today);

        // Lưu lần 2: Sửa 1 bạn từ Có mặt sang Vắng không phép
        SaveAttendanceRequest req = new SaveAttendanceRequest();
        req.setSessionId(sessionId);
        req.setReason("Học viên bỏ về giữa giờ không báo cáo");
        req.setItems(List.of(
                new SaveAttendanceRequest.AttendanceItem(1001L, "Vắng không phép", "Bỏ về giữa giờ")
        ));

        attendanceDAO.saveAttendance(req, instructor, today);

        // Kiểm tra audit logs
        List<AttendanceAuditLog> logs = attendanceDAO.getAuditLogs(sessionId);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());

        AttendanceAuditLog changeLog = logs.stream()
                .filter(l -> l.getStudentId() != null && l.getStudentId().equals(1001L))
                .findFirst()
                .orElse(null);

        assertNotNull(changeLog, "Phải có nhật ký ghi nhận sự thay đổi trạng thái của học viên 1001");
        assertEquals("Có mặt", changeLog.getOldStatus());
        assertEquals("Vắng không phép", changeLog.getNewStatus());
        assertEquals("VU DUC LONG", changeLog.getActorName());
        assertEquals(RoleConstant.INSTRUCTOR, changeLog.getActorRole());
    }

    // ==========================================
    // AC5: DỮ LIỆU TINH GỌN, TỐI ƯU GIAO DIỆN 360PX
    // ==========================================

    @Test
    void testAC5_ResponseStructureSupportsCompact360pxView() throws SQLException {
        Long sessionId = 401L;
        AttendanceSheetResponse sheet = attendanceDAO.getAttendanceSheet(sessionId, instructor, today);

        assertNotNull(sheet);
        assertNotNull(sheet.getClassCode());
        assertNotNull(sheet.getClassName());
        assertNotNull(sheet.getStartTime());
        assertNotNull(sheet.getEndTime());
        assertNotNull(sheet.getRoomName());

        // Tỷ lệ chuyên cần và số liệu tổng hợp được tính toán sẵn
        assertNotNull(sheet.getAttendanceRate());
        assertEquals(5, sheet.getTotalStudents());

        // Danh sách học viên có đầy đủ mã và trạng thái cho chip/badge trên mobile 360px
        for (AttendanceSheetResponse.StudentAttendanceItem item : sheet.getStudents()) {
            assertNotNull(item.getStudentId());
            assertNotNull(item.getStudentCode());
            assertNotNull(item.getStudentName());
        }
    }
}
