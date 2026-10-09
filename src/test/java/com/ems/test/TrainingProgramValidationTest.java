package com.ems.test;

import com.ems.dto.TrainingProgramRequest;
import com.ems.exception.ValidationException;
import com.ems.service.TrainingProgramService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử Validation dữ liệu đầu vào cho Chương trình đào tạo (S2-04)
 */
public class TrainingProgramValidationTest {

    private TrainingProgramService service;

    @BeforeEach
    void setUp() {
        service = new TrainingProgramService();
    }

    @Test
    @DisplayName("3. Create với name rỗng -> ném lỗi ValidationException")
    void testEmptyNameThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "", "Mô tả", 200, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Tên chương trình đào tạo không được để trống"));
    }

    @Test
    @DisplayName("Create với code rỗng -> ném lỗi ValidationException")
    void testEmptyCodeThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "", "Java Web", "Mô tả", 200, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Mã chương trình đào tạo không được để trống"));
    }

    @Test
    @DisplayName("Create với code chứa ký tự đặc biệt không hợp lệ -> ném lỗi ValidationException")
    void testInvalidCodeCharactersThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA WEB @#$", "Java Web", "Mô tả", 200, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Mã chương trình đào tạo chỉ được chứa"));
    }

    @Test
    @DisplayName("4. Create với duration = 0 -> ném lỗi ValidationException")
    void testZeroDurationThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Java Web", "Mô tả", 0, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Thời lượng chương trình đào tạo phải lớn hơn 0"));
    }

    @Test
    @DisplayName("4. Create với duration < 0 -> ném lỗi ValidationException")
    void testNegativeDurationThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Java Web", "Mô tả", -50, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Thời lượng chương trình đào tạo phải lớn hơn 0"));
    }

    @Test
    @DisplayName("Create với duration là null -> ném lỗi ValidationException")
    void testNullDurationThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Java Web", "Mô tả", null, new BigDecimal("15000000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Thời lượng chương trình đào tạo không được để trống"));
    }

    @Test
    @DisplayName("5. Create với standardTuition < 0 -> ném lỗi ValidationException")
    void testNegativeStandardTuitionThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Java Web", "Mô tả", 200, new BigDecimal("-1000"), "ACTIVE");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Học phí chuẩn không được là số âm"));
    }

    @Test
    @DisplayName("Create với standardTuition = 0 -> hợp lệ (chấp nhận học phí 0 đồng)")
    void testZeroStandardTuitionValid() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-FREE", "Java Miễn Phí", "Mô tả", 200, BigDecimal.ZERO, "ACTIVE");
        assertDoesNotThrow(() -> service.validateRequest(request, true));
    }

    @Test
    @DisplayName("Create với status không hợp lệ -> ném lỗi ValidationException")
    void testInvalidStatusThrowsValidationException() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB", "Java Web", "Mô tả", 200, new BigDecimal("15000000"), "UNKNOWN_STATUS");
        ValidationException ex = assertThrows(ValidationException.class, () -> service.validateRequest(request, true));
        assertTrue(ex.getMessage().contains("Trạng thái chỉ có thể là ACTIVE hoặc INACTIVE"));
    }

    @Test
    @DisplayName("Dữ liệu chuẩn hợp lệ -> vượt qua kiểm tra validation thành công")
    void testValidRequestPasses() {
        TrainingProgramRequest request = new TrainingProgramRequest(
                "JAVA-WEB-2026", "Java Web Chuyên Sâu", "Khóa học đầy đủ",
                240, new BigDecimal("15000000"), "ACTIVE");
        assertDoesNotThrow(() -> service.validateRequest(request, true));
    }
}
