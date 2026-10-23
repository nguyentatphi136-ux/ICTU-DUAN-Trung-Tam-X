package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dao.AttendanceDAO;
import com.ems.dao.ClassSessionDAO;
import com.ems.dao.LeaveRequestDAO;
import com.ems.dao.NotificationDAO;
import com.ems.dto.ClassSessionResponse;
import com.ems.exception.ApiException;
import com.ems.model.User;
import com.ems.security.PermissionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S4-07 điểm danh, S4-08 đơn xin phép trên H2. Hôm nay là 2026-10-23. Lớp S4T-K15 có 3 học viên (S4T-HV1..3), giảng
 * viên LE HOANG NAM (9101) dạy; NGO THI THU (9102) không dạy lớp này. Buổi 9201 hôm nay, 9202 đã qua, 9203 sắp tới;
 * buổi 9204 của lớp S4T-K16.
 */
class AttendanceLeaveTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);
    private final User teacher = new User(9101L, "LE HOANG NAM", "s4t.gv1@test.vn", "ACTIVE", List.of("INSTRUCTOR"));
    private final User otherTeacher = new User(9102L, "NGO THI THU", "s4t.gv2@test.vn", "ACTIVE", List.of("INSTRUCTOR"));
    private final User manager = new User(9103L, "PHAM GIA HUY", "s4t.tg1@test.vn", "ACTIVE", List.of("TRAINING_MANAGER"));
    private final User student = new User(9111L, "NGUYEN MINH ANH", "s4t.hv1@test.vn", "ACTIVE", List.of("STUDENT"));

    private AttendanceDAO attendance;
    private LeaveRequestDAO leaves;

    @BeforeEach
    void setUp() throws Exception {
        S4TestData.reset();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO class_sessions (id, class_id, class_subject_id, subject_lesson_id, session_number, session_date, "
                    + "start_time, end_time, classroom_id, instructor_id, status) VALUES "
                    + "(9201, 9102, 9104, NULL, 3, '2026-10-23', '18:00:00', '20:30:00', 9101, 9101, 'SCHEDULED'), "
                    + "(9202, 9102, 9104, NULL, 2, '2026-10-20', '18:00:00', '20:30:00', 9101, 9101, 'SCHEDULED'), "
                    + "(9203, 9102, 9104, NULL, 4, '2026-10-26', '18:00:00', '20:30:00', 9101, 9101, 'SCHEDULED'), "
                    + "(9204, 9101, 9101, NULL, 1, '2026-11-02', '18:00:00', '20:30:00', 9101, 9101, 'SCHEDULED')");
        }
        attendance = new AttendanceDAO(() -> TODAY);
        leaves = new LeaveRequestDAO(() -> TODAY);
    }

    private static int status(Executable e) {
        return assertThrows(ApiException.class, e::run).getStatusCode();
    }

    interface Executable {
        void run() throws Exception;
    }

    private static Map<String, String> marks(String m1, String m2, String m3) {
        return Map.of("S4T-HV1", m1, "S4T-HV2", m2, "S4T-HV3", m3);
    }

    @Test
    void sessionJsonHasTopicSeqAndKeepsSessionsWithoutSubject() throws Exception {
        List<ClassSessionResponse> list = new ClassSessionDAO().findWhere("WHERE cs.class_id = ?", List.of(9102L));
        assertEquals(List.of(9202L, 9201L, 9203L), list.stream().map(ClassSessionResponse::getId).toList());
        assertEquals(List.of(1, 2, 3), list.stream().map(ClassSessionResponse::getSeq).toList(), "Buổi thứ mấy trong lớp theo ngày");
        assertEquals("S4T-GIT", list.get(0).getSubjectCode());
        assertEquals("LE HOANG NAM", list.get(0).getInstructorName());
    }

    @Test
    void teacherTakesTodayAttendanceAndLogIsWritten() throws Exception {
        Map<String, Object> sheet = attendance.get(teacher, 9201);
        assertEquals(3, ((List<?>) sheet.get("roster")).size());
        assertNull(sheet.get("sheet"));
        assertEquals(true, sheet.get("canEdit"));

        assertEquals(400, status(() -> attendance.save(teacher, 9201, Map.of("S4T-HV1", "PRESENT"), Map.of(), null)), "Còn học viên chưa điểm danh");
        assertEquals(400, status(() -> attendance.save(teacher, 9201, marks("PRESENT", "OOPS", "PRESENT"), Map.of(), null)));

        Map<String, Object> saved = attendance.save(teacher, 9201, marks("PRESENT", "PRESENT", "UNEXCUSED_ABSENCE"), Map.of("S4T-HV2", "Đến muộn 5 phút"), null);
        @SuppressWarnings("unchecked")
        Map<String, Object> s = (Map<String, Object>) saved.get("sheet");
        assertEquals("LE HOANG NAM", s.get("savedBy"));
        assertEquals(Map.of("S4T-HV2", "Đến muộn 5 phút"), s.get("notes"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> logs = (List<Map<String, String>>) saved.get("logs");
        assertEquals("Lưu điểm danh lần đầu: 2 có mặt · 1 vắng không phép.", logs.get(0).get("text"));
        assertEquals("Giảng viên", logs.get(0).get("role"));
        assertEquals(400, status(() -> attendance.save(teacher, 9201, marks("PRESENT", "PRESENT", "UNEXCUSED_ABSENCE"),
                Map.of("S4T-HV2", "Đến muộn 5 phút"), null)), "Chưa có thay đổi nào");
    }

    @Test
    void pastSessionOnlyManagerAndEditNeedsReason() throws Exception {
        assertEquals(403, status(() -> attendance.save(teacher, 9202, marks("PRESENT", "PRESENT", "PRESENT"), Map.of(), null)));
        assertEquals(false, attendance.get(teacher, 9202).get("canEdit"));
        assertEquals(400, status(() -> attendance.save(manager, 9203, marks("PRESENT", "PRESENT", "PRESENT"), Map.of(), null)), "Chưa tới ngày học");

        attendance.save(manager, 9202, marks("PRESENT", "PRESENT", "PRESENT"), Map.of(), null);
        assertEquals(400, status(() -> attendance.save(manager, 9202, marks("LATE", "PRESENT", "PRESENT"), Map.of(), " ")));
        Map<String, Object> edited = attendance.save(manager, 9202, marks("LATE", "PRESENT", "PRESENT"), Map.of(), "Giảng viên báo đánh nhầm");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> logs = (List<Map<String, String>>) edited.get("logs");
        assertEquals("NGUYEN MINH ANH: có mặt → đi muộn. Lý do: Giảng viên báo đánh nhầm", logs.get(0).get("text"));
        assertEquals("Quản lý đào tạo", logs.get(0).get("role"));
    }

    @Test
    void onlyTeachersOfTheClassSeeItsSessions() throws Exception {
        assertEquals(403, status(() -> attendance.get(otherTeacher, 9201)));
        List<Long> mine = attendance.visibleSessions(teacher, TODAY.minusDays(14), TODAY).stream()
                .map(r -> ((ClassSessionResponse) r.get("session")).getId()).toList();
        assertTrue(mine.containsAll(List.of(9201L, 9202L)));
        assertFalse(mine.contains(9203L), "Buổi sắp tới nằm ngoài khoảng");
        assertTrue(attendance.visibleSessions(otherTeacher, TODAY.minusDays(14), TODAY).stream()
                .noneMatch(r -> ((ClassSessionResponse) r.get("session")).getClassId() == 9102L));
    }

    @Test
    void studentSendsLeaveAndTeacherApproves() throws Exception {
        assertEquals(400, status(() -> leaves.create(student, 9203, "Ngắn", null)), "Lý do ít nhất 10 ký tự");
        assertEquals(400, status(() -> leaves.create(student, 9202, "Em bận việc gia đình đột xuất", null)), "Buổi đã qua");
        assertEquals(403, status(() -> leaves.create(student, 9204, "Em bận việc gia đình đột xuất", null)), "Không phải lớp mình");
        assertEquals(400, status(() -> leaves.create(student, 9203, "Em bận việc gia đình đột xuất", file("virus.exe", 10))));
        assertEquals(400, status(() -> leaves.create(student, 9203, "Em bận việc gia đình đột xuất", file("to.pdf", LeaveRequestDAO.MAX_PROOF + 1))), "Quá 5 MB");
        assertEquals(403, status(() -> leaves.create(teacher, 9203, "Em bận việc gia đình đột xuất", null)), "Không phải học viên");

        Map<String, Object> sent = leaves.create(student, 9203, "Em có lịch thi ở trường vào tối hôm đó", file("lich-thi.pdf", 2048));
        assertEquals("PENDING", sent.get("status"));
        assertEquals("lich-thi.pdf", sent.get("fileName"));
        assertEquals(2048L, sent.get("fileSize"));
        assertEquals(9203L, ((ClassSessionResponse) sent.get("session")).getId());
        assertEquals(409, status(() -> leaves.create(student, 9203, "Em có lịch thi ở trường vào tối hôm đó", null)), "Trùng đơn");
        assertEquals(1, leaves.mine(student).size());

        long id = (Long) sent.get("id");
        LeaveRequestDAO.Proof proof = leaves.proof(teacher, id);
        assertEquals("application/pdf", proof.type(), "Kiểu tệp theo đuôi, không theo trình duyệt gửi lên");
        assertEquals(2048, proof.data().length);
        assertEquals(403, status(() -> leaves.proof(otherTeacher, id)));
        assertEquals(List.of("NGUYEN MINH ANH xin phép vắng"), titles(9101), "Giảng viên lớp nhận thông báo đơn mới");
        assertTrue(titles(9102).isEmpty());
        assertEquals(1, leaves.forReviewer(teacher).size());
        assertTrue(leaves.forReviewer(otherTeacher).isEmpty());
        assertEquals(1, leaves.forReviewer(manager).stream().filter(r -> r.get("id").equals(id)).count());
        assertEquals(403, status(() -> leaves.decide(otherTeacher, id, true, null)));
        assertEquals(400, status(() -> leaves.decide(teacher, id, false, " ")), "Từ chối phải có lý do");

        Map<String, Object> approved = leaves.decide(teacher, id, true, "Chúc em thi tốt");
        assertEquals("APPROVED", approved.get("status"));
        assertEquals("LE HOANG NAM", approved.get("decidedBy"));
        assertEquals(409, status(() -> leaves.decide(teacher, id, false, "Đổi ý")), "Đơn đã xử lý");
        assertEquals(List.of("Đơn xin phép đã được duyệt"), titles(9111), "Học viên nhận kết quả duyệt");
        assertEquals(1, new NotificationDAO().inbox(9111).size());
        new NotificationDAO().markRead(9111, List.of());
        assertEquals(true, new NotificationDAO().inbox(9111).get(0).get("read"));
        assertEquals(List.of("S4T-HV1"), attendance.get(manager, 9203).get("approved"), "Chưa điểm danh: điền sẵn khi mở");
        assertNull(attendance.get(manager, 9203).get("sheet"), "Duyệt đơn không tạo bảng điểm danh dở dang");
    }

    @Test
    void approvingLeaveOfAnAlreadyMarkedSessionExcusesTheStudent() throws Exception {
        attendance.save(teacher, 9201, marks("UNEXCUSED_ABSENCE", "PRESENT", "PRESENT"), Map.of(), null);
        long id = (Long) leaves.create(student, 9201, "Em bị sốt, có giấy khám bệnh", null).get("id");
        leaves.decide(teacher, id, true, null);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT status, notes FROM attendance WHERE session_id = 9201 AND student_id = 9111")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("EXCUSED_ABSENCE", rs.getString(1));
                assertEquals("Đơn xin phép đã duyệt", rs.getString(2));
            }
        }
        assertEquals(1, leaves.mine(student).get(0).get("excused"));
    }

    @Test
    void teacherAsksManagerToEditPastSession() throws Exception {
        assertEquals(400, status(() -> attendance.requestEdit(teacher, 9201, null)), "Buổi hôm nay tự sửa được");
        assertEquals(400, status(() -> attendance.requestEdit(teacher, 9203, null)), "Buổi chưa diễn ra");
        assertEquals(403, status(() -> attendance.requestEdit(otherTeacher, 9202, null)));
        attendance.requestEdit(teacher, 9202, "Đánh nhầm học viên đi muộn");
        assertEquals(List.of("Yêu cầu sửa điểm danh S4T-K15 buổi 1"), titles(9103));
    }

    private static LeaveRequestDAO.Proof file(String name, int size) {
        return new LeaveRequestDAO.Proof(name, "text/html", new byte[size]);
    }

    private static List<String> titles(long userId) throws Exception {
        return new NotificationDAO().inbox(userId).stream().map(n -> (String) n.get("title")).toList();
    }

    @Test
    void permissionRulesOfNewApis() {
        assertEquals("SCHEDULE_READ", PermissionPolicy.requiredPermission("GET", "/api/me/schedule"));
        assertEquals("ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/attendance/sessions"));
        assertEquals("ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/sessions/9201/attendance"));
        assertEquals("ABSENCE_REQUEST|ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/leave-requests"));
        assertEquals("ABSENCE_REQUEST", PermissionPolicy.requiredPermission("POST", "/api/leave-requests"));
        assertEquals("ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("PATCH", "/api/leave-requests/5"));
        assertEquals("STUDENT_READ", PermissionPolicy.requiredPermission("GET", "/api/student-status/HV260101"));
        assertEquals("CLASS_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/student-status/HV260101"));
        assertEquals("SCHEDULE_READ", PermissionPolicy.requiredPermission("GET", "/api/schedules/range"));
        assertEquals("ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/sessions/9201/attendance/edit-request"));
        assertEquals("ABSENCE_REQUEST|ATTENDANCE_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/leave-requests/5/proof"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/me/notifications"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/me/notifications/read"));
        assertNull(PermissionPolicy.requiredPermission("DELETE", "/api/sessions/9201"));
    }
}
