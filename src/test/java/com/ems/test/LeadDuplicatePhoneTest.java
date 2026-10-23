package com.ems.test;

import com.ems.dao.LeadDAO;
import com.ems.model.Lead;
import com.ems.service.LeadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử xử lý trùng lặp số điện thoại (Duplicate Phone Handling - S2-09 [IDTTX-45])
 */
public class LeadDuplicatePhoneTest {

    // Lớp LeadDAO giả lập trong bộ nhớ để kiểm thử độc lập mà không cần MySQL server chạy thực tế
    static class MockLeadDAO extends LeadDAO {
        private final Map<Long, Lead> storage = new HashMap<>();
        private long idCounter = 1;

        public MockLeadDAO() {
            // Dữ liệu mẫu
            Lead l1 = new Lead(idCounter++, "Trần Minh Quang", "0912345678", "quang.tran@gmail.com",
                    Lead.SOURCE_WEBSITE, 1, "Java", Lead.STATUS_NEW, "Khách mới", null, 1L, 1L);
            storage.put(l1.getId(), l1);

            Lead l2 = new Lead(idCounter++, "Lê Hoàng Yến", "0987654321", "yen.le@yahoo.com",
                    Lead.SOURCE_FACEBOOK, 2, "React", Lead.STATUS_CONTACTED, "Khách hẹn", null, 1L, 1L);
            storage.put(l2.getId(), l2);
        }

        @Override
        public Lead findById(long id) {
            return storage.get(id);
        }

        @Override
        public Lead findByPhone(String phone) {
            if (phone == null) return null;
            String clean = phone.replaceAll("[\\s.-]", "");
            return storage.values().stream()
                    .filter(l -> l.getPhone().replaceAll("[\\s.-]", "").equals(clean))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean existsByPhone(String phone, Long excludeId) {
            if (phone == null) return false;
            String clean = phone.replaceAll("[\\s.-]", "");
            return storage.values().stream()
                    .anyMatch(l -> l.getPhone().replaceAll("[\\s.-]", "").equals(clean)
                            && (excludeId == null || !l.getId().equals(excludeId)));
        }

        @Override
        public long create(Lead lead) {
            long newId = idCounter++;
            lead.setId(newId);
            storage.put(newId, lead);
            return newId;
        }

        @Override
        public boolean update(Lead lead) {
            if (lead.getId() == null || !storage.containsKey(lead.getId())) return false;
            storage.put(lead.getId(), lead);
            return true;
        }

        @Override
        public boolean delete(long id) {
            return storage.remove(id) != null;
        }
    }

    @Test
    @DisplayName("Phát hiện số điện thoại đã tồn tại khi thêm mới lead")
    void testDetectDuplicatePhoneOnCreate() throws SQLException {
        MockLeadDAO mockDAO = new MockLeadDAO();
        LeadService service = new LeadService(mockDAO);

        // Số điện thoại 0912345678 đã có trong hệ thống (của Trần Minh Quang - Lead #1)
        Lead duplicateLead = service.checkDuplicatePhone("0912345678", null);
        assertNotNull(duplicateLead, "Phải phát hiện số điện thoại đã tồn tại");
        assertEquals(1L, duplicateLead.getId());
        assertEquals("Trần Minh Quang", duplicateLead.getFullName());

        // Kiểm tra với định dạng có dấu cách và dấu gạch nối
        Lead duplicateFormatted = service.checkDuplicatePhone("0912 345 678", null);
        assertNotNull(duplicateFormatted, "Phải nhận diện được dù số điện thoại có dấu cách");
        assertEquals(1L, duplicateFormatted.getId());
    }

    @Test
    @DisplayName("Cho phép thêm số điện thoại mới chưa có trong hệ thống")
    void testAllowUniquePhoneOnCreate() throws SQLException {
        MockLeadDAO mockDAO = new MockLeadDAO();
        LeadService service = new LeadService(mockDAO);

        Lead duplicate = service.checkDuplicatePhone("0933445566", null);
        assertNull(duplicate, "Số điện thoại chưa có không được coi là trùng lặp");
    }

    @Test
    @DisplayName("Khi cập nhật: Giữ nguyên số điện thoại của chính lead đó thì không bị báo trùng")
    void testSelfPhoneNotTreatedAsDuplicateOnUpdate() throws SQLException {
        MockLeadDAO mockDAO = new MockLeadDAO();
        LeadService service = new LeadService(mockDAO);

        // Lead #1 đang có số 0912345678, khi sửa Lead #1 mà giữ nguyên số điện thoại
        Lead duplicate = service.checkDuplicatePhone("0912345678", 1L);
        assertNull(duplicate, "Không được báo trùng với chính bản ghi đang cập nhật");
    }

    @Test
    @DisplayName("Khi cập nhật: Thay đổi sang số điện thoại của lead khác phải bị báo trùng")
    void testChangingToAnotherLeadsPhoneTriggersDuplicate() throws SQLException {
        MockLeadDAO mockDAO = new MockLeadDAO();
        LeadService service = new LeadService(mockDAO);

        // Lead #2 (Lê Hoàng Yến) sửa số điện thoại thành 0912345678 (số của Lead #1)
        Lead duplicate = service.checkDuplicatePhone("0912345678", 2L);
        assertNotNull(duplicate, "Phải phát hiện trùng khi sửa sang số của lead khác");
        assertEquals(1L, duplicate.getId());
        assertEquals("Trần Minh Quang", duplicate.getFullName());
    }

    @Test
    @DisplayName("CRUD đầy đủ hoạt động chính xác qua Service và DAO")
    void testFullCrudLifecycle() throws SQLException {
        MockLeadDAO mockDAO = new MockLeadDAO();
        LeadService service = new LeadService(mockDAO);

        // 1. Create
        Lead newLead = new Lead();
        newLead.setFullName("Phạm Băng Băng");
        newLead.setPhone("0977889900");
        newLead.setEmail("bb.pham@example.com");
        newLead.setSource("ADS");
        newLead.setStatus("NEW");

        long createdId = service.createLead(newLead);
        assertTrue(createdId > 0);

        // 2. Read
        Lead retrieved = service.findById(createdId);
        assertNotNull(retrieved);
        assertEquals("Phạm Băng Băng", retrieved.getFullName());

        // 3. Update
        retrieved.setFullName("Phạm Băng Băng (VIP)");
        retrieved.setStatus("CONTACTED");
        boolean updated = service.updateLead(retrieved);
        assertTrue(updated);

        Lead afterUpdate = service.findById(createdId);
        assertEquals("Phạm Băng Băng (VIP)", afterUpdate.getFullName());
        assertEquals("CONTACTED", afterUpdate.getStatus());

        // 4. Delete
        boolean deleted = service.deleteLead(createdId);
        assertTrue(deleted);
        assertNull(service.findById(createdId));
    }
}
