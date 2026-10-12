package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.dao.TrainingProgramDAO;
import com.ems.dto.TrainingProgramPageResponse;
import com.ems.dto.TrainingProgramRequest;
import com.ems.dto.TrainingProgramResponse;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.model.TrainingProgram;
import com.ems.service.TrainingProgramService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp toàn bộ nghiệp vụ CSDL & Service cho Chương trình đào tạo (S2-04)
 */
public class TrainingProgramServiceTest {

    private TrainingProgramService service;
    private TrainingProgramDAO dao;

    @BeforeAll
    static void initDatabase() {
        TestDatabaseHelper.initTestDatabase();
    }

    @BeforeEach
    void setUp() {
        TestDatabaseHelper.clearData();
        dao = new TrainingProgramDAO();
        service = new TrainingProgramService(dao);
    }

    @Test
    @DisplayName("1. Create Training Program thành công")
    void testCreateTrainingProgramSuccess() throws SQLException {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Lập trình Java Web", "Khóa học chuyên sâu Java Backend",
                240, new BigDecimal("15000000"), "ACTIVE");

        TrainingProgramResponse response = service.create(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("JAVA-WEB", response.getCode());
        assertEquals("Lập trình Java Web", response.getName());
        assertEquals(240, response.getDuration());
        assertEquals(new BigDecimal("15000000.00"), response.getStandardTuition());
        assertEquals("ACTIVE", response.getStatus());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("2. Create với code trùng -> ném ngoại lệ ConflictException (409)")
    void testCreateDuplicateCodeThrowsConflictException() throws SQLException {
        TrainingProgramRequest first = new TrainingProgramRequest(
                "JAVA-WEB", "Lập trình Java Web", "Mô tả 1",
                240, new BigDecimal("15000000"), "ACTIVE");
        service.create(first);

        // Tạo tiếp với cùng code JAVA-WEB (kể cả chữ thường java-web)
        TrainingProgramRequest duplicate = new TrainingProgramRequest(
                "java-web", "Java Web Khác", "Mô tả 2",
                180, new BigDecimal("12000000"), "ACTIVE");

        ConflictException ex = assertThrows(ConflictException.class, () -> service.create(duplicate));
        assertEquals(409, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("đã tồn tại"));
    }

    @Test
    @DisplayName("6. Get list: Lấy danh sách, phân trang và tìm kiếm theo keyword")
    void testGetListAndSearch() throws SQLException {
        service.create(new TrainingProgramRequest("JAVA-01", "Java Core Cơ Bản", "Mô tả", 100, new BigDecimal("8000000"), "ACTIVE"));
        service.create(new TrainingProgramRequest("JAVA-02", "Java Web Nâng Cao", "Mô tả", 200, new BigDecimal("15000000"), "ACTIVE"));
        service.create(new TrainingProgramRequest("REACT-01", "ReactJS Frontend", "Mô tả", 150, new BigDecimal("10000000"), "ACTIVE"));
        service.create(new TrainingProgramRequest("PYTHON-01", "Python AI", "Mô tả", 180, new BigDecimal("14000000"), "INACTIVE"));

        // Test tìm kiếm keyword "Java"
        TrainingProgramPageResponse javaSearch = service.getList("Java", "ALL", 1, 10);
        assertEquals(2, javaSearch.getTotal());

        // Test lọc theo status INACTIVE
        TrainingProgramPageResponse inactiveList = service.getList(null, "INACTIVE", 1, 10);
        assertEquals(1, inactiveList.getTotal());
        assertEquals("PYTHON-01", inactiveList.getItems().get(0).getCode());

        // Test phân trang: page 1, size 2
        TrainingProgramPageResponse paged = service.getList(null, "ALL", 1, 2);
        assertEquals(4, paged.getTotal());
        assertEquals(2, paged.getItems().size());
        assertEquals(2, paged.getTotalPages());
    }

    @Test
    @DisplayName("7. Get detail thành công")
    void testGetDetailSuccess() throws SQLException {
        TrainingProgramResponse created = service.create(new TrainingProgramRequest(
                "CS-DEV", "C# .NET Developer", "Mô tả", 200, new BigDecimal("12000000"), "ACTIVE"));

        TrainingProgramResponse detail = service.getById(created.getId());
        assertNotNull(detail);
        assertEquals(created.getId(), detail.getId());
        assertEquals("CS-DEV", detail.getCode());
        assertEquals("C# .NET Developer", detail.getName());
    }

    @Test
    @DisplayName("8. Get detail với ID không tồn tại -> ném ResourceNotFoundException (404)")
    void testGetDetailNotFoundThrowsException() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getById(99999));
        assertEquals(404, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy chương trình đào tạo"));
    }

    @Test
    @DisplayName("9. Update chương trình đào tạo thành công")
    void testUpdateSuccess() throws SQLException {
        TrainingProgramResponse created = service.create(new TrainingProgramRequest(
                "ORIG-CODE", "Tên Ban Đầu", "Mô tả cũ", 100, new BigDecimal("5000000"), "ACTIVE"));

        TrainingProgramRequest updateReq = new TrainingProgramRequest(
                "UPD-CODE", "Tên Mới", "Mô tả mới", 150, new BigDecimal("7000000"), "ACTIVE");

        TrainingProgramResponse updated = service.update(created.getId(), updateReq);
        assertNotNull(updated);
        assertEquals("UPD-CODE", updated.getCode());
        assertEquals("Tên Mới", updated.getName());
        assertEquals(150, updated.getDuration());
        assertEquals(new BigDecimal("7000000.00"), updated.getStandardTuition());
    }

    @Test
    @DisplayName("10. Update với code trùng với chương trình khác -> ném ConflictException (409)")
    void testUpdateDuplicateCodeThrowsConflictException() throws SQLException {
        service.create(new TrainingProgramRequest("PROG-A", "Chương trình A", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));
        TrainingProgramResponse progB = service.create(new TrainingProgramRequest("PROG-B", "Chương trình B", "Mô tả", 120, new BigDecimal("6000000"), "ACTIVE"));

        // Thử cập nhật progB với code PROG-A
        TrainingProgramRequest updateReq = new TrainingProgramRequest("PROG-A", "Chương trình B đổi tên", "Mô tả", 120, new BigDecimal("6000000"), "ACTIVE");
        ConflictException ex = assertThrows(ConflictException.class, () -> service.update(progB.getId(), updateReq));
        assertEquals(409, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("đã được sử dụng"));
    }

    @Test
    @DisplayName("11. Delete chương trình không có lớp đang chạy -> xoá thành công")
    void testDeleteProgramWithoutRunningClassesSuccess() throws SQLException {
        TrainingProgramResponse prog = service.create(new TrainingProgramRequest(
                "DELETE-ME", "Chương trình thử nghiệm", "Mô tả", 50, new BigDecimal("2000000"), "ACTIVE"));

        // Thêm lớp đã KẾT THÚC (FINISHED)
        addClassToProgram(prog.getId(), "CLASS-FINISHED", "FINISHED");

        // Được phép xoá vì không có lớp đang chạy
        assertDoesNotThrow(() -> service.delete(prog.getId()));

        // Kiểm tra đã xoá khỏi database
        assertNull(dao.findById(prog.getId()));
    }

    @Test
    @DisplayName("12. Không cho delete chương trình có lớp đang chạy -> ném ConflictException (409)")
    void testDeleteProgramWithRunningClassBlocked() throws SQLException {
        TrainingProgramResponse prog = service.create(new TrainingProgramRequest(
                "RUNNING-PROG", "Chương trình đang chạy", "Mô tả", 200, new BigDecimal("15000000"), "ACTIVE"));

        // Thêm lớp ĐANG CHẠY (IN_PROGRESS)
        addClassToProgram(prog.getId(), "CLASS-RUNNING-01", "IN_PROGRESS");

        ConflictException ex = assertThrows(ConflictException.class, () -> service.delete(prog.getId()));
        assertEquals(409, ex.getStatusCode());
        assertEquals("Không thể xoá chương trình đào tạo vì đang có lớp học đang chạy.", ex.getMessage());

        // Dữ liệu vẫn còn nguyên vẹn trong DB
        assertNotNull(dao.findById(prog.getId()));
    }

    @Test
    @DisplayName("13. Deactivate chương trình thành công -> status = INACTIVE, không xoá dữ liệu")
    void testDeactivateProgramSuccess() throws SQLException {
        TrainingProgramResponse prog = service.create(new TrainingProgramRequest(
                "DEACT-PROG", "Chương trình cần dừng", "Mô tả", 100, new BigDecimal("8000000"), "ACTIVE"));

        TrainingProgramResponse deactivated = service.deactivate(prog.getId());
        assertEquals("INACTIVE", deactivated.getStatus());

        // Kiểm tra trong database: dữ liệu vẫn tồn tại và có status = INACTIVE
        TrainingProgram inDb = dao.findById(prog.getId());
        assertNotNull(inDb);
        assertEquals("INACTIVE", inDb.getStatus());
    }

    @Test
    @DisplayName("15. Kiểm tra database UNIQUE constraint cho program_code")
    void testDatabaseUniqueConstraint() throws SQLException {
        service.create(new TrainingProgramRequest("UNIQUE-CODE", "Tên 1", "Mô tả", 100, new BigDecimal("5000000"), "ACTIVE"));

        // Bỏ qua service validation, cố tình gọi thẳng DAO create để kiểm tra Database UNIQUE constraint
        TrainingProgram directInsert = new TrainingProgram();
        directInsert.setCode("UNIQUE-CODE");
        directInsert.setName("Tên 2");
        directInsert.setDuration(120);
        directInsert.setStandardTuition(new BigDecimal("6000000"));
        directInsert.setStatus("ACTIVE");

        assertThrows(SQLException.class, () -> dao.create(directInsert));
    }

    private void addClassToProgram(int programId, String classCode, String status) throws SQLException {
        String sql = "INSERT INTO classes (class_code, class_name, program_id, start_date, max_capacity, status) "
                + "VALUES (?, ?, ?, CURRENT_DATE, 30, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, classCode);
            stmt.setString(2, "Lớp " + classCode);
            stmt.setInt(3, programId);
            stmt.setString(4, status);
            stmt.executeUpdate();
        }
    }
}
