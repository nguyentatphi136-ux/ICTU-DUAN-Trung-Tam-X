package com.ems.test;

import com.ems.dao.ClassDAO;
import com.ems.dao.ProgramDAO;
import com.ems.dto.ClassResponse;
import com.ems.dto.CreateClassRequest;
import com.ems.exception.BusinessRuleException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.Clazz;
import com.ems.model.Subject;
import com.ems.service.ClassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp (Integration Test) cho nghiệp vụ Mở lớp học (User Story S3-05)
 * Chạy trên CSDL In-Memory H2 tương thích MySQL
 * Kiểm tra các tiêu chí: Transaction Atomicity, Rollback, Duplicate Code, Data Copying
 */
public class ClassServiceIntegrationTest {

    private ClassService classService;
    private ClassDAO classDAO;
    private ProgramDAO programDAO;

    @BeforeEach
    void setUp() throws SQLException {
        TestDbHelper.initDatabase();
        classDAO = new ClassDAO(TestDbHelper::getConnection);
        programDAO = new ProgramDAO(TestDbHelper::getConnection);
        classService = new ClassService(classDAO, programDAO, TestDbHelper::getConnection);
    }

    @Test
    @DisplayName("TC 1 & 2: Mở lớp thành công với trạng thái ban đầu là Chuẩn bị (PLANNING)")
    void testOpenClassSuccessInitialStatusPlanning() throws SQLException {
        CreateClassRequest request = new CreateClassRequest(
                "CNTT-K23-01",
                1,
                "2026-11-01",
                30,
                "Offline"
        );

        ClassResponse response = classService.openClass(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("CNTT-K23-01", response.getClassCode());
        assertEquals(Clazz.STATUS_PLANNING, response.getStatus());
        assertEquals("Chuẩn bị", response.getStatusLabel());
        assertEquals(1, response.getProgramId());
        assertEquals("Lập trình viên Quốc tế", response.getProgramName());
        assertEquals("2026-11-01", response.getExpectedStartDate());
        assertEquals(30, response.getMaxStudents());
        assertEquals("OFFLINE", response.getStudyMode());
    }

    @Test
    @DisplayName("TC 8: Chương trình có nhiều môn học: toàn bộ môn học được tự động sao chép theo đúng thứ tự")
    void testOpenClassCopiesAllProgramSubjectsInOrder() throws SQLException {
        CreateClassRequest request = new CreateClassRequest(
                "CNTT-K23-FULL",
                1,
                "2026-11-15",
                25,
                "Hybrid"
        );

        ClassResponse response = classService.openClass(request);

        // Chương trình 1 có 3 môn học: CS101, DB101, JAVA101
        assertEquals(3, response.getSubjectCount());
        assertNotNull(response.getSubjects());
        assertEquals(3, response.getSubjects().size());

        // Kiểm tra thứ tự và thông tin môn học được sao chép
        ClassResponse.SubjectItem s1 = response.getSubjects().get(0);
        assertEquals("CS101", s1.getSubjectCode());
        assertEquals("Nhập môn lập trình", s1.getSubjectName());
        assertEquals(1, s1.getOrderIndex());
        assertEquals("NOT_STARTED", s1.getStatus());

        ClassResponse.SubjectItem s2 = response.getSubjects().get(1);
        assertEquals("DB101", s2.getSubjectCode());
        assertEquals(2, s2.getOrderIndex());

        ClassResponse.SubjectItem s3 = response.getSubjects().get(2);
        assertEquals("JAVA101", s3.getSubjectCode());
        assertEquals(3, s3.getOrderIndex());

        // Xác minh trực tiếp trong CSDL bảng class_subjects
        try (Connection conn = TestDbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM class_subjects WHERE class_id = ?")) {
            stmt.setLong(1, response.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("TC 3: Mã lớp trùng lặp bị từ chối với DuplicateResourceException (HTTP 409)")
    void testDuplicateClassCodeRejected() throws SQLException {
        CreateClassRequest request1 = new CreateClassRequest(
                "CNTT-K23-DUP",
                1,
                "2026-11-01",
                30,
                "Offline"
        );
        classService.openClass(request1);

        CreateClassRequest request2 = new CreateClassRequest(
                "CNTT-K23-DUP",
                1,
                "2026-12-01",
                35,
                "Online"
        );

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> classService.openClass(request2)
        );
        assertTrue(exception.getMessage().contains("CNTT-K23-DUP"));
    }

    @Test
    @DisplayName("TC 4: ProgramId không tồn tại hoặc đã ngừng hoạt động bị từ chối (HTTP 404)")
    void testNonExistentOrInactiveProgramRejected() {
        // ID 999 không tồn tại
        CreateClassRequest reqNonExistent = new CreateClassRequest(
                "CLASS-404",
                999,
                "2026-11-01",
                30,
                "Offline"
        );
        assertThrows(ResourceNotFoundException.class, () -> classService.openClass(reqNonExistent));

        // Program 3 ở trạng thái INACTIVE
        CreateClassRequest reqInactive = new CreateClassRequest(
                "CLASS-INACTIVE",
                3,
                "2026-11-01",
                30,
                "Offline"
        );
        assertThrows(ResourceNotFoundException.class, () -> classService.openClass(reqInactive));
    }

    @Test
    @DisplayName("TC 5: MaxStudents bằng 0 hoặc số âm bị từ chối với ValidationException (HTTP 400)")
    void testInvalidMaxStudentsRejected() {
        CreateClassRequest reqZero = new CreateClassRequest("CLASS-ZERO", 1, "2026-11-01", 0, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqZero));

        CreateClassRequest reqNegative = new CreateClassRequest("CLASS-NEG", 1, "2026-11-01", -5, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqNegative));
    }

    @Test
    @DisplayName("TC 6: Thiếu ClassCode hoặc ClassCode sai định dạng bị từ chối (HTTP 400)")
    void testInvalidClassCodeRejected() {
        CreateClassRequest reqNull = new CreateClassRequest(null, 1, "2026-11-01", 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqNull));

        CreateClassRequest reqBlank = new CreateClassRequest("   ", 1, "2026-11-01", 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqBlank));

        // Ký tự đặc biệt không hợp lệ
        CreateClassRequest reqSpecial = new CreateClassRequest("CLASS@#$%", 1, "2026-11-01", 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqSpecial));
    }

    @Test
    @DisplayName("TC 7: Ngày khai giảng không hợp lệ bị từ chối (HTTP 400)")
    void testInvalidExpectedStartDateRejected() {
        CreateClassRequest reqNull = new CreateClassRequest("CLASS-DATE", 1, null, 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqNull));

        CreateClassRequest reqFormat = new CreateClassRequest("CLASS-DATE", 1, "01/11/2026", 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqFormat));

        CreateClassRequest reqInvalidDay = new CreateClassRequest("CLASS-DATE", 1, "2026-02-30", 30, "Offline");
        assertThrows(ValidationException.class, () -> classService.openClass(reqInvalidDay));
    }

    @Test
    @DisplayName("TC 9: Chương trình không có môn học: ném BusinessRuleException (HTTP 400) và không tạo lớp")
    void testProgramWithoutSubjectsRejected() throws SQLException {
        // Program 2 là EMPTY_PROG, chưa có môn học nào trong program_subjects
        CreateClassRequest request = new CreateClassRequest(
                "CLASS-EMPTY",
                2,
                "2026-11-01",
                30,
                "Offline"
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> classService.openClass(request)
        );
        assertTrue(exception.getMessage().contains("chưa có môn học hợp lệ"));

        // Kiểm tra chắc chắn lớp không bị tạo trong CSDL
        assertFalse(classDAO.existsByClassCode("CLASS-EMPTY"));
    }

    @Test
    @DisplayName("TC 13: Lỗi khi sao chép môn học khiến toàn bộ Transaction Rollback (không lưu lớp dở dang)")
    void testTransactionRollbackWhenSubjectCopyingFails() throws SQLException {
        // Tạo một DAO bị lỗi cố ý khi sao chép môn học
        ClassDAO faultyDAO = new ClassDAO(TestDbHelper::getConnection) {
            @Override
            public Clazz createClassWithSubjects(Clazz clazz, List<Subject> subjects, Connection connection) throws SQLException {
                // Gọi cha để insert classes trước
                super.createClassWithSubjects(clazz, subjects, connection);
                // Giả lập lỗi đột ngột khi sao chép môn học
                throw new SQLException("Lỗi mô phỏng: kết nối bị ngắt quãng giữa chừng khi sao chép môn học.");
            }
        };

        ClassService serviceWithFault = new ClassService(faultyDAO, programDAO, TestDbHelper::getConnection);

        CreateClassRequest request = new CreateClassRequest(
                "CLASS-ROLLBACK",
                1,
                "2026-11-01",
                30,
                "Offline"
        );

        assertThrows(SQLException.class, () -> serviceWithFault.openClass(request));

        // Kiểm tra sau khi rollback, lớp KHÔNG được tồn tại trong bảng classes!
        try (Connection conn = TestDbHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM classes WHERE class_code = ?")) {
            stmt.setString(1, "CLASS-ROLLBACK");
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1), "Lớp không được lưu lại khi thao tác sao chép môn học bị lỗi (Atomicity)");
            }
        }
    }

    @Test
    @DisplayName("TC 14: Hai request đồng thời sử dụng cùng mã lớp: đảm bảo tính duy nhất ở CSDL")
    void testConcurrentClassCreationSameCode() throws InterruptedException, ExecutionException {
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateErrorCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    CreateClassRequest req = new CreateClassRequest("CNTT-CONCURRENT-01", 1, "2026-11-01", 30, "Offline");
                    classService.openClass(req);
                    successCount.incrementAndGet();
                } catch (DuplicateResourceException e) {
                    duplicateErrorCount.incrementAndGet();
                } catch (Exception e) {
                    // Các lỗi khác nếu có
                }
            }));
        }

        // Bắn đồng thời các request
        latch.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Đúng 1 request thành công, các request còn lại bị chặn do trùng mã
        assertEquals(1, successCount.get(), "Chỉ duy nhất 1 lớp được tạo thành công với cùng 1 mã lớp");
        assertEquals(threads - 1, duplicateErrorCount.get(), "Các request đồng thời còn lại phải bị từ chối");
    }
}
