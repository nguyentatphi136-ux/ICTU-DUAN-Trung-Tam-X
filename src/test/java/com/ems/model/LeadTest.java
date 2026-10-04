package com.ems.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho thực thể Lead (S2-09 [IDTTX-45])
 */
public class LeadTest {

    @Test
    @DisplayName("Kiểm tra khởi tạo mặc định của Lead")
    void testDefaultConstructor() {
        Lead lead = new Lead();
        assertEquals(Lead.SOURCE_WEBSITE, lead.getSource(), "Nguồn mặc định phải là WEBSITE");
        assertEquals(Lead.STATUS_NEW, lead.getStatus(), "Trạng thái mặc định phải là NEW");
        assertNull(lead.getId());
        assertNull(lead.getFullName());
    }

    @Test
    @DisplayName("Kiểm tra khởi tạo đầy đủ tham số và các Getter/Setter")
    void testParameterizedConstructorAndSetters() {
        Lead lead = new Lead(1L, "Nguyễn Văn An", "0912345678", "an.nguyen@example.com",
                Lead.SOURCE_FACEBOOK, 2, "Lập trình Java Web", Lead.STATUS_CONTACTED,
                "Khách hẹn gọi lại", 3L, 1L, 1L);

        assertEquals(1L, lead.getId());
        assertEquals("Nguyễn Văn An", lead.getFullName());
        assertEquals("0912345678", lead.getPhone());
        assertEquals("an.nguyen@example.com", lead.getEmail());
        assertEquals(Lead.SOURCE_FACEBOOK, lead.getSource());
        assertEquals(2, lead.getProgramId());
        assertEquals("Lập trình Java Web", lead.getProgramInterest());
        assertEquals(Lead.STATUS_CONTACTED, lead.getStatus());
        assertEquals("Khách hẹn gọi lại", lead.getNotes());
        assertEquals(3L, lead.getAssignedTo());
        assertEquals(1L, lead.getCreatedBy());
        assertEquals(1L, lead.getUpdatedBy());
    }

    @Test
    @DisplayName("Kiểm tra tính hợp lệ của trạng thái Lead (Valid Statuses)")
    void testValidStatuses() {
        assertTrue(Lead.isValidStatus(Lead.STATUS_NEW));
        assertTrue(Lead.isValidStatus(Lead.STATUS_CONTACTED));
        assertTrue(Lead.isValidStatus(Lead.STATUS_CONSULTING));
        assertTrue(Lead.isValidStatus(Lead.STATUS_TRIAL));
        assertTrue(Lead.isValidStatus(Lead.STATUS_WON));
        assertTrue(Lead.isValidStatus(Lead.STATUS_LOST));
        assertTrue(Lead.isValidStatus(Lead.STATUS_ENROLLED));
        assertTrue(Lead.isValidStatus("new")); // Case-insensitive
        assertTrue(Lead.isValidStatus("won"));

        assertFalse(Lead.isValidStatus(null));
        assertFalse(Lead.isValidStatus(""));
        assertFalse(Lead.isValidStatus("UNKNOWN_STATUS"));
        assertFalse(Lead.isValidStatus("COMPLETED"));
    }

    @Test
    @DisplayName("Kiểm tra tính hợp lệ của nguồn Lead (Valid Sources)")
    void testValidSources() {
        assertTrue(Lead.isValidSource(Lead.SOURCE_WEBSITE));
        assertTrue(Lead.isValidSource(Lead.SOURCE_FACEBOOK));
        assertTrue(Lead.isValidSource(Lead.SOURCE_REFERRAL));
        assertTrue(Lead.isValidSource(Lead.SOURCE_DIRECT));
        assertTrue(Lead.isValidSource(Lead.SOURCE_ADS));
        assertTrue(Lead.isValidSource(Lead.SOURCE_EVENT));
        assertTrue(Lead.isValidSource(Lead.SOURCE_OTHER));
        assertTrue(Lead.isValidSource("website"));

        assertFalse(Lead.isValidSource(null));
        assertFalse(Lead.isValidSource(""));
        assertFalse(Lead.isValidSource("INVALID_SOURCE"));
    }
}
