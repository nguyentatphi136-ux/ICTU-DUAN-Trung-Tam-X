package com.ems.test;

import com.ems.dto.TrainingProgramPageResponse;
import com.ems.dto.TrainingProgramRequest;
import com.ems.dto.TrainingProgramResponse;
import com.ems.model.TrainingProgram;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử Model Entity và DTO của Chương trình đào tạo
 */
public class TrainingProgramModelTest {

    @Test
    @DisplayName("Kiểm tra khởi tạo Model TrainingProgram đầy đủ thuộc tính")
    void testTrainingProgramModel() {
        TrainingProgram p = new TrainingProgram(1, "JAVA-WEB", "Java Web", "Mô tả khóa học",
                240, new BigDecimal("15000000"), "ACTIVE");
        Timestamp now = new Timestamp(System.currentTimeMillis());
        p.setCreatedAt(now);
        p.setUpdatedAt(now);

        assertEquals(1, p.getId());
        assertEquals("JAVA-WEB", p.getCode());
        assertEquals("Java Web", p.getName());
        assertEquals("Mô tả khóa học", p.getDescription());
        assertEquals(240, p.getDuration());
        assertEquals(new BigDecimal("15000000"), p.getStandardTuition());
        assertEquals("ACTIVE", p.getStatus());
        assertEquals(now, p.getCreatedAt());
        assertEquals(now, p.getUpdatedAt());
        assertNotNull(p.toString());
    }

    @Test
    @DisplayName("Kiểm tra DTO TrainingProgramResponse chuyển đổi từ Entity")
    void testTrainingProgramResponseFromEntity() {
        TrainingProgram p = new TrainingProgram(2, "REACT-FE", "React Frontend", "Mô tả",
                180, new BigDecimal("12000000"), "ACTIVE");
        p.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        TrainingProgramResponse resp = TrainingProgramResponse.fromEntity(p);
        assertNotNull(resp);
        assertEquals(2, resp.getId());
        assertEquals("REACT-FE", resp.getCode());
        assertEquals("React Frontend", resp.getName());
        assertEquals(180, resp.getDuration());
        assertEquals(new BigDecimal("12000000"), resp.getStandardTuition());
        assertEquals("ACTIVE", resp.getStatus());
        assertNotNull(resp.getCreatedAt());
    }

    @Test
    @DisplayName("Kiểm tra TrainingProgramPageResponse tính toán số trang đúng")
    void testPageResponseCalculations() {
        TrainingProgramResponse item = new TrainingProgramResponse();
        item.setId(1);
        item.setCode("PROG-01");

        TrainingProgramPageResponse page = new TrainingProgramPageResponse(Collections.singletonList(item), 25, 1, 10);
        assertEquals(25, page.getTotal());
        assertEquals(1, page.getPage());
        assertEquals(10, page.getPageSize());
        assertEquals(3, page.getTotalPages());
        assertEquals(1, page.getItems().size());
    }
}
