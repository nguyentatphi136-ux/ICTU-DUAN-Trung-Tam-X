package com.ems.test;

import com.ems.dao.AssignmentDAO;
import com.ems.model.Assignment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị các ràng buộc nghiệp vụ của chức năng S5-05:
 * Tạo bài tập và giao cho lớp (Tiêu đề, Hạn nộp không ở quá khứ, Điểm tối đa, Lưu nháp).
 */
class AssignmentValidationTest {

    @Test
    void testTitleCannotBeEmpty() {
        Assignment a = new Assignment();
        a.setTitle("   ");
        a.setDueDate(LocalDateTime.now().plusDays(3));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AssignmentDAO.validateAssignment(a));
        assertTrue(ex.getMessage().contains("tiêu đề"));
    }

    @Test
    void testDueDateCannotBeInThePast() {
        Assignment a = new Assignment();
        a.setTitle("Bài tập thực hành SQL");
        a.setDueDate(LocalDateTime.now().minusMinutes(5)); // Hạn nộp ở quá khứ

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AssignmentDAO.validateAssignment(a));
        assertTrue(ex.getMessage().contains("quá khứ"), "Phải báo lỗi khi hạn nộp ở quá khứ");
    }

    @Test
    void testDueDateInTheFutureIsValid() {
        Assignment a = new Assignment();
        a.setTitle("Bài tập thực hành React & Vite");
        a.setDueDate(LocalDateTime.now().plusDays(7)); // Hạn nộp ở tương lai
        a.setMaxScore(new BigDecimal("10.0"));
        a.setIsDraft(true);

        assertDoesNotThrow(() -> AssignmentDAO.validateAssignment(a));
    }

    @Test
    void testMaxScoreBounds() {
        Assignment a = new Assignment();
        a.setTitle("Bài tập lớn cuối kỳ");
        a.setDueDate(LocalDateTime.now().plusDays(10));

        // Điểm < 1
        a.setMaxScore(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> AssignmentDAO.validateAssignment(a));

        // Điểm > 100
        a.setMaxScore(new BigDecimal("105"));
        assertThrows(IllegalArgumentException.class, () -> AssignmentDAO.validateAssignment(a));

        // Điểm hợp lệ (100)
        a.setMaxScore(new BigDecimal("100"));
        assertDoesNotThrow(() -> AssignmentDAO.validateAssignment(a));
    }

    @Test
    void testDraftAndPublishStatus() {
        Assignment a = new Assignment();
        a.setTitle("Bài tập 01 - Java Core");
        a.setIsDraft(true);
        assertTrue(a.getIsDraft(), "Mặc định có thể lưu nháp");

        a.setIsDraft(false);
        assertFalse(a.getIsDraft(), "Khi giao cho lớp, trạng thái nháp = false");
    }
}
