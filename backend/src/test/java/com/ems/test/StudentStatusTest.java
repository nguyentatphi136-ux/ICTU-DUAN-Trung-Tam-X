package com.ems.test;

import com.ems.dao.StudentStatusDAO;
import com.ems.model.ClassItem;
import com.ems.model.Student;
import com.ems.model.StudentStatusLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động nghiệp vụ S4-06 (User Story Jira IDTTX-252, Subtask IDTTX-268 [BE]):
 * 1. Ba loại thao tác có lý do và ngày hiệu lực riêng (Bảo lưu, Chuyển lớp, Thôi học).
 * 2. Học viên bảo lưu/thôi học không còn xuất hiện trong danh sách điểm danh từ ngày hiệu lực.
 * 3. Chuyển lớp giữ nguyên lịch sử điểm và điểm danh ở lớp cũ.
 * 4. Toàn bộ thao tác được ghi vào lịch sử hồ sơ học viên.
 */
public class StudentStatusTest {

    private StudentStatusDAO dao;

    @BeforeEach
    public void setUp() {
        dao = new StudentStatusDAO();
    }

    @Test
    @DisplayName("S4-06 AC1: Bảo lưu có lý do và ngày hiệu lực riêng, lưu vào lịch sử hồ sơ")
    public void testSuspendStudent() {
        String studentCode = "HV260101";
        Student student = dao.getStudentByCode(studentCode);
        assertNotNull(student);
        String oldClass = student.getCurrentClass();
        assertEquals("WEB-K15", oldClass);

        boolean ok = dao.processStatusChange(
                studentCode,
                "Bảo lưu",
                "2026-10-25",
                "Lý do sức khoẻ",
                "Phẫu thuật dây chằng, nghỉ điều trị 2 tháng",
                null,
                "Nguyễn Minh Ngọc",
                1L
        );

        assertTrue(ok);
        Student updated = dao.getStudentByCode(studentCode);
        assertEquals("Bảo lưu", updated.getStatus());
        assertNull(updated.getCurrentClass());

        // Kiểm tra lịch sử lớp đã học
        assertFalse(updated.getHistory().isEmpty());
        assertEquals(oldClass, updated.getHistory().get(0).getClassCode());
        assertEquals("Bảo lưu", updated.getHistory().get(0).getResult());

        // Kiểm tra lịch sử biến động học vụ
        assertFalse(updated.getStatusChanges().isEmpty());
        StudentStatusLog latestLog = updated.getStatusChanges().get(0);
        assertEquals("Bảo lưu", latestLog.getActionType());
        assertEquals("2026-10-25", latestLog.getEffectiveDate());
        assertTrue(latestLog.getReason().contains("Lý do sức khoẻ"));
        assertEquals("Nguyễn Minh Ngọc", latestLog.getActorName());
    }

    @Test
    @DisplayName("S4-06 AC2: Chuyển lớp chỉ chọn được lớp cùng chương trình, đang chạy và còn chỗ")
    public void testAvailableClassesForTransfer() {
        Student student = dao.getStudentByCode("HV260102");
        assertNotNull(student);

        List<ClassItem> available = dao.getAvailableClassesForTransfer(student.getCurrentClass(), student.getProgram());
        assertFalse(available.isEmpty());

        for (ClassItem c : available) {
            // Cùng chương trình
            assertEquals(student.getProgram(), c.getProgram());
            // Khác lớp hiện tại
            assertNotEquals(student.getCurrentClass(), c.getCode());
            // Còn chỗ
            assertTrue(c.hasAvailableSeats());
            // Đang mở
            assertTrue(c.isOpen());
        }
    }

    @Test
    @DisplayName("S4-06 AC3: Chuyển lớp thành công, lớp cũ được ghi vào lịch sử và lưu vết hồ sơ")
    public void testTransferStudent() {
        String studentCode = "HV260102";
        Student student = dao.getStudentByCode(studentCode);
        String fromClass = student.getCurrentClass();
        String toClass = "WEB-K16";

        boolean ok = dao.processStatusChange(
                studentCode,
                "Chuyển lớp",
                "2026-11-01",
                "Đổi sang lịch học phù hợp hơn",
                "Xin chuyển sang học lớp ca tối",
                toClass,
                "Nguyễn Tất Phi",
                1L
        );

        assertTrue(ok);
        Student updated = dao.getStudentByCode(studentCode);
        assertEquals(toClass, updated.getCurrentClass());
        assertEquals("Đang học", updated.getStatus());

        // Kiểm tra lịch sử
        assertEquals(fromClass, updated.getHistory().get(0).getClassCode());
        assertEquals("Chuyển sang " + toClass, updated.getHistory().get(0).getResult());
    }

    @Test
    @DisplayName("S4-06 AC4: Roster on date - Học viên rời lớp không còn trong danh sách từ ngày hiệu lực")
    public void testRosterOnDate() {
        // HV260131 đang học WEB-K16, bảo lưu từ ngày 2026-10-20
        String studentCode = "HV260131";
        String classCode = "WEB-K16";

        dao.processStatusChange(
                studentCode,
                "Bảo lưu",
                "2026-10-20",
                "Bận công việc",
                "Đi công tác nước ngoài",
                null,
                "Nguyễn Minh Ngọc",
                1L
        );

        // 1. Vào ngày trước ngày hiệu lực: học viên VẪN CÓ TÊN trong danh sách điểm danh lớp cũ
        List<Student> rosterBefore = dao.getRosterOnDate(classCode, "2026-10-15");
        boolean presentBefore = rosterBefore.stream().anyMatch(s -> s.getCode().equals(studentCode));
        assertTrue(presentBefore, "Trước ngày hiệu lực, học viên vẫn phải có tên trong danh sách điểm danh");

        // 2. Vào ngày bằng hoặc sau ngày hiệu lực: học viên KHÔNG CÒN trong danh sách điểm danh
        List<Student> rosterAfter = dao.getRosterOnDate(classCode, "2026-10-20");
        boolean presentAfter = rosterAfter.stream().anyMatch(s -> s.getCode().equals(studentCode));
        assertFalse(presentAfter, "Từ ngày hiệu lực, học viên bảo lưu không được xuất hiện trong danh sách điểm danh");
    }

    @Test
    @DisplayName("S4-06 AC5: Thôi học cập nhật trạng thái và lưu vết kiểm toán")
    public void testDropOutStudent() {
        String studentCode = "HV260128";
        boolean ok = dao.processStatusChange(
                studentCode,
                "Thôi học",
                "2026-10-22",
                "Không theo kịp chương trình",
                "Gia đình xin thôi học",
                null,
                "Nguyễn Minh Ngọc",
                1L
        );

        assertTrue(ok);
        Student updated = dao.getStudentByCode(studentCode);
        assertEquals("Thôi học", updated.getStatus());
        assertNull(updated.getCurrentClass());
    }
}
