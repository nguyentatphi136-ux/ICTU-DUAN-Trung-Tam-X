package com.ems.test;

import com.ems.model.Lead;
import com.ems.service.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tính đúng đắn của bộ quy tắc Validation cho Lead (S2-09 [IDTTX-45])
 */
public class LeadValidationTest {

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService();
    }

    @Test
    @DisplayName("Kiểm tra định dạng số điện thoại di động Việt Nam hợp lệ")
    void testValidVietnamesePhoneNumbers() {
        assertTrue(LeadService.isValidPhoneNumber("0912345678"));
        assertTrue(LeadService.isValidPhoneNumber("0987654321"));
        assertTrue(LeadService.isValidPhoneNumber("0388991122"));
        assertTrue(LeadService.isValidPhoneNumber("0566778899"));
        assertTrue(LeadService.isValidPhoneNumber("0799887766"));
        assertTrue(LeadService.isValidPhoneNumber("0855443322"));
        assertTrue(LeadService.isValidPhoneNumber("+84912345678"));
        assertTrue(LeadService.isValidPhoneNumber("0912 345 678")); // Có dấu cách ngắt
        assertTrue(LeadService.isValidPhoneNumber("0912-345-678")); // Có dấu gạch nối
    }

    @Test
    @DisplayName("Kiểm tra số điện thoại không hợp lệ bị từ chối")
    void testInvalidPhoneNumbers() {
        assertFalse(LeadService.isValidPhoneNumber(null));
        assertFalse(LeadService.isValidPhoneNumber(""));
        assertFalse(LeadService.isValidPhoneNumber("   "));
        assertFalse(LeadService.isValidPhoneNumber("12345")); // Quá ngắn
        assertFalse(LeadService.isValidPhoneNumber("0123456789")); // Đầu số 01x không còn hiệu lực
        assertFalse(LeadService.isValidPhoneNumber("091234567890")); // 12 số (quá dài)
        assertFalse(LeadService.isValidPhoneNumber("abcdefghij")); // Ký tự chữ
        assertFalse(LeadService.isValidPhoneNumber("0912abc678")); // Lẫn chữ
        assertFalse(LeadService.isValidPhoneNumber("0243888999")); // Số bàn cố định
    }

    @Test
    @DisplayName("Kiểm tra định dạng email hợp lệ và không hợp lệ")
    void testEmailValidation() {
        assertTrue(LeadService.isValidEmail("quang.tran@gmail.com"));
        assertTrue(LeadService.isValidEmail("test_user+tag@domain.edu.vn"));
        assertTrue(LeadService.isValidEmail("admissions@ictu.vn"));

        assertFalse(LeadService.isValidEmail(null));
        assertFalse(LeadService.isValidEmail(""));
        assertFalse(LeadService.isValidEmail("not-an-email"));
        assertFalse(LeadService.isValidEmail("missing@domain"));
        assertFalse(LeadService.isValidEmail("@missing-username.com"));
        assertFalse(LeadService.isValidEmail("space in@email.com"));
    }

    @Test
    @DisplayName("Lead hợp lệ đầy đủ không có lỗi validation")
    void testValidLeadPassesValidation() {
        Lead lead = new Lead();
        lead.setFullName("Nguyễn Văn Hùng");
        lead.setPhone("0988112233");
        lead.setEmail("hung.nguyen@example.com");
        lead.setSource("FACEBOOK");
        lead.setStatus("NEW");

        List<String> errors = leadService.validateLead(lead, false);
        assertTrue(errors.isEmpty(), "Lead hợp lệ không được có lỗi: " + errors);
    }

    @Test
    @DisplayName("Lead thiếu tên hoặc tên quá ngắn/dài sinh lỗi validation")
    void testFullNameValidationErrors() {
        Lead leadEmptyName = new Lead("", "0988112233", "test@example.com", "Lập trình");
        List<String> errors1 = leadService.validateLead(leadEmptyName, false);
        assertFalse(errors1.isEmpty());
        assertTrue(errors1.stream().anyMatch(e -> e.contains("Họ và tên không được để trống")));

        Lead leadShortName = new Lead("A", "0988112233", "test@example.com", "Lập trình");
        List<String> errors2 = leadService.validateLead(leadShortName, false);
        assertFalse(errors2.isEmpty());
        assertTrue(errors2.stream().anyMatch(e -> e.contains("độ dài từ 2 đến 100 ký tự")));

        String longName = "A".repeat(101);
        Lead leadLongName = new Lead(longName, "0988112233", "test@example.com", "Lập trình");
        List<String> errors3 = leadService.validateLead(leadLongName, false);
        assertFalse(errors3.isEmpty());
        assertTrue(errors3.stream().anyMatch(e -> e.contains("độ dài từ 2 đến 100 ký tự")));
    }

    @Test
    @DisplayName("Lead thiếu số điện thoại sinh lỗi validation")
    void testMissingPhoneValidationError() {
        Lead lead = new Lead("Nguyễn Thị Mai", "", "mai@example.com", "Tester");
        List<String> errors = leadService.validateLead(lead, false);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Số điện thoại không được để trống")));
    }

    @Test
    @DisplayName("Lead có trạng thái hoặc nguồn không hợp lệ sinh lỗi validation")
    void testInvalidStatusAndSourceErrors() {
        Lead lead = new Lead("Lê Văn Bình", "0912345678", "binh@gmail.com", "Java");
        lead.setStatus("INVALID_STATUS");
        lead.setSource("UNKNOWN_SOURCE");

        List<String> errors = leadService.validateLead(lead, false);
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.contains("Trạng thái không hợp lệ")));
        assertTrue(errors.stream().anyMatch(e -> e.contains("Nguồn khách hàng không hợp lệ")));
    }
}
