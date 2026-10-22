package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.controller.StudentStatusServlet;
import com.ems.dao.StudentStatusDAO;
import com.ems.model.ClassItem;
import com.ems.model.Student;
import com.ems.model.StudentStatusLog;
import com.ems.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử S4-06: Bảo lưu, Chuyển lớp, Thôi học và danh sách điểm danh theo ngày,
 * chạy trên H2 với học viên, ghi danh, lớp của S3 (xem S4TestData).
 */
public class StudentStatusTest {

    private StudentStatusDAO dao;

    @BeforeEach
    void setUp() throws Exception {
        S4TestData.reset();
        dao = new StudentStatusDAO();
    }

    private String enrollmentStatus(long classId, long studentId) throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT status FROM enrollments WHERE class_id = ? AND student_id = ?")) {
            ps.setLong(1, classId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    @Test
    @DisplayName("S4-06 AC1: Bảo lưu có lý do và ngày hiệu lực riêng, lưu vào lịch sử hồ sơ")
    void testSuspend() throws Exception {
        Student student = dao.getStudentByCode("S4T-HV1");
        assertEquals("S4T-K15", student.getCurrentClass());
        assertEquals("Lập trình web Full-stack", student.getProgram());

        dao.processStatusChange("S4T-HV1", StudentStatusDAO.SUSPEND, "2026-10-25",
                "Lý do sức khoẻ", "Điều trị 1 tháng", null, "Nguyễn Minh Ngọc", 1L);

        Student updated = dao.getStudentByCode("S4T-HV1");
        assertEquals("Bảo lưu", updated.getStatus());
        assertNull(updated.getCurrentClass());
        assertEquals("SUSPENDED", enrollmentStatus(9102, 9111));
        assertEquals("S4T-K15", updated.getHistory().get(0).getClassCode());
        assertEquals("Bảo lưu", updated.getHistory().get(0).getResult());

        StudentStatusLog log = updated.getStatusChanges().get(0);
        assertEquals("Bảo lưu", log.getActionType());
        assertEquals("2026-10-25", log.getEffectiveDate());
        assertEquals("Lý do sức khoẻ", log.getReason());
        assertEquals("Điều trị 1 tháng", log.getNotes());
        assertEquals("Nguyễn Minh Ngọc", log.getActorName());
        assertEquals("S4T-K15", log.getFromClass());
    }

    @Test
    @DisplayName("S4-06 AC2: Chuyển lớp chỉ chọn được lớp cùng chương trình, đang chạy và còn chỗ")
    void testTransferTargets() {
        List<String> codes = dao.getAvailableClassesForTransfer("S4T-K15", "Lập trình web Full-stack")
                .stream().map(ClassItem::getCode).toList();
        assertTrue(codes.contains("S4T-K16"));
        assertTrue(codes.contains("S4T-K17"));
        assertFalse(codes.contains("S4T-K15"), "Không chuyển sang chính lớp đang học");
        assertFalse(codes.contains("S4T-J01"), "Khác chương trình");
        assertFalse(codes.contains("S4T-OLD"), "Lớp đã kết thúc");
    }

    @Test
    @DisplayName("S4-06 AC3: Chuyển lớp cập nhật ghi danh hai lớp, lớp cũ vào lịch sử; lớp đủ chỗ hoặc khác chương trình bị từ chối")
    void testTransfer() throws Exception {
        dao.processStatusChange("S4T-HV1", StudentStatusDAO.TRANSFER, "2026-11-01",
                "Đổi lịch học", null, "S4T-K17", "Nguyễn Minh Ngọc", 1L);

        Student updated = dao.getStudentByCode("S4T-HV1");
        assertEquals("S4T-K17", updated.getCurrentClass());
        assertEquals("Đang học", updated.getStatus());
        assertEquals("TRANSFERRED", enrollmentStatus(9102, 9111));
        assertEquals("ENROLLED", enrollmentStatus(9103, 9111));
        assertEquals("S4T-K15", updated.getHistory().get(0).getClassCode());
        assertEquals("S4T-K17", updated.getStatusChanges().get(0).getToClass());

        // S4T-K17 chỉ có 1 chỗ, đã kín
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV2", StudentStatusDAO.TRANSFER,
                "2026-11-01", "Đổi lịch học", null, "S4T-K17", "QLDT", 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV2", StudentStatusDAO.TRANSFER,
                "2026-11-01", "Đổi lịch học", null, "S4T-J01", "QLDT", 1L));
        assertEquals("ENROLLED", enrollmentStatus(9102, 9112), "Bị từ chối thì không đổi gì");
        assertTrue(dao.getStatusLogsForStudent("S4T-HV2").isEmpty(), "Bị từ chối thì không ghi nhật ký");
    }

    @Test
    @DisplayName("S4-06 AC4: Roster on date - Rời lớp thì vắng từ ngày hiệu lực, chuyển đến thì có mặt từ ngày hiệu lực")
    void testRosterOnDate() {
        dao.processStatusChange("S4T-HV2", StudentStatusDAO.SUSPEND, "2026-10-20",
                "Bận công việc", null, null, "QLDT", 1L);
        dao.processStatusChange("S4T-HV1", StudentStatusDAO.TRANSFER, "2026-11-01",
                "Đổi lịch học", null, "S4T-K16", "QLDT", 1L);

        List<String> before = dao.getRosterOnDate("S4T-K15", "2026-10-15").stream().map(Student::getCode).toList();
        assertTrue(before.containsAll(List.of("S4T-HV1", "S4T-HV2", "S4T-HV3")));

        List<String> after = dao.getRosterOnDate("S4T-K15", "2026-11-02").stream().map(Student::getCode).toList();
        assertEquals(List.of("S4T-HV3"), after);

        assertTrue(dao.getRosterOnDate("S4T-K16", "2026-10-31").isEmpty(), "Chưa tới ngày chuyển đến");
        assertEquals("S4T-HV1", dao.getRosterOnDate("S4T-K16", "2026-11-01").get(0).getCode());
    }

    @Test
    @DisplayName("S4-06 AC5: Thôi học cập nhật trạng thái, ghi danh và lưu vết; đã thôi học thì không thao tác tiếp")
    void testDropOut() throws Exception {
        dao.processStatusChange("S4T-HV3", StudentStatusDAO.DROP_OUT, "2026-10-30",
                "Không theo kịp chương trình", null, null, "QLDT", 1L);

        Student updated = dao.getStudentByCode("S4T-HV3");
        assertEquals("Thôi học", updated.getStatus());
        assertNull(updated.getCurrentClass());
        assertEquals("DROPPED", enrollmentStatus(9102, 9113));
        assertEquals("Thôi học", updated.getStatusChanges().get(0).getActionType());
        assertTrue(dao.getAllStatusLogs(15).stream().anyMatch(l -> "TRAN DUC HUY".equals(l.getStudentName())));

        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV3", StudentStatusDAO.SUSPEND,
                "2026-11-01", "Lý do", null, null, "QLDT", 1L));
    }

    @Test
    @DisplayName("S4-06 AC6: Kiểm tra dữ liệu đầu vào và lọc danh sách theo tình trạng")
    void testValidationAndFilter() {
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV1", "Xoá", "2026-11-01", "x", null, null, "QLDT", 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV1", StudentStatusDAO.SUSPEND, "01/11/2026", "x", null, null, "QLDT", 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-HV1", StudentStatusDAO.SUSPEND, "2026-11-01", " ", null, null, "QLDT", 1L));
        assertThrows(IllegalArgumentException.class, () -> dao.processStatusChange("S4T-KHONG-CO", StudentStatusDAO.SUSPEND, "2026-11-01", "x", null, null, "QLDT", 1L));
        assertNull(dao.getStudentByCode("S4T-KHONG-CO"));

        dao.processStatusChange("S4T-HV1", StudentStatusDAO.SUSPEND, "2026-11-01", "x", null, null, "QLDT", 1L);
        List<String> suspended = dao.getAllStudents("S4T-", "Bảo lưu", null, null).stream().map(Student::getCode).toList();
        assertEquals(List.of("S4T-HV1"), suspended, "Bộ lọc gửi nhãn tiếng Việt vẫn phải lọc đúng");
    }

    @Test
    @DisplayName("S4-06 AC7: Phân quyền - cán bộ xem được, chỉ Quản lý đào tạo và Admin đổi tình trạng học")
    void testAuthorization() {
        User qldt = new User(1L, "QLDT", "a@x.vn", "ACTIVE", List.of("TRAINING_MANAGER"));
        User gv = new User(2L, "GV", "b@x.vn", "ACTIVE", List.of("INSTRUCTOR"));
        User hv = new User(3L, "HV", "c@x.vn", "ACTIVE", List.of("STUDENT"));

        assertTrue(StudentStatusServlet.isStaff(qldt));
        assertTrue(StudentStatusServlet.isStaff(gv));
        assertFalse(StudentStatusServlet.isStaff(hv), "Học viên không xem tình trạng học của người khác");
        assertTrue(StudentStatusServlet.canChangeStatus(qldt));
        assertFalse(StudentStatusServlet.canChangeStatus(gv));
        assertFalse(StudentStatusServlet.canChangeStatus(hv));
        assertFalse(StudentStatusServlet.canChangeStatus(null));
    }
}
