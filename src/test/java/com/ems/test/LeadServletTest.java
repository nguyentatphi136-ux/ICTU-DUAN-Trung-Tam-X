package com.ems.test;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.dao.LeadDAO;
import com.ems.model.Lead;
import com.ems.model.User;
import com.ems.service.LeadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử nghiệp vụ S2-09 – EP-03 – Tư vấn tuyển sinh:
 * 1. Tạo và sửa lead với họ tên, số điện thoại, email, nguồn, chương trình quan tâm.
 * 2. Cảnh báo khi số điện thoại trùng với lead đã có.
 * 3. Chỉ Quản lý đào tạo (và Admin) được xoá lead, Tư vấn tuyển sinh KHÔNG được xoá.
 */
public class LeadServletTest {

    static class InMemoryLeadDAO extends LeadDAO {
        private final Map<Long, Lead> db = new HashMap<>();
        private long counter = 1;

        @Override
        public Lead findById(long id) {
            return db.get(id);
        }

        @Override
        public Lead findByPhone(String phone) {
            if (phone == null) return null;
            String clean = phone.replaceAll("[\\s.-]", "");
            return db.values().stream()
                    .filter(l -> l.getPhone().replaceAll("[\\s.-]", "").equals(clean))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean existsByPhone(String phone, Long excludeId) {
            if (phone == null) return false;
            String clean = phone.replaceAll("[\\s.-]", "");
            return db.values().stream()
                    .anyMatch(l -> l.getPhone().replaceAll("[\\s.-]", "").equals(clean)
                            && (excludeId == null || !l.getId().equals(excludeId)));
        }

        @Override
        public long create(Lead lead) {
            long id = counter++;
            lead.setId(id);
            db.put(id, lead);
            return id;
        }

        @Override
        public boolean update(Lead lead) {
            if (lead.getId() == null || !db.containsKey(lead.getId())) return false;
            db.put(lead.getId(), lead);
            return true;
        }

        @Override
        public boolean delete(long id) {
            return db.remove(id) != null;
        }

        @Override
        public Map<String, Object> searchLeads(String keyword, String status, String source,
                                               Long assignedTo, String startDate, String endDate,
                                               int page, int pageSize) {
            List<Lead> items = db.values().stream().toList();
            return Map.of(
                    "items", items,
                    "page", page,
                    "pageSize", pageSize,
                    "totalItems", items.size(),
                    "totalPages", 1
            );
        }
    }

    private InMemoryLeadDAO dao;
    private LeadService leadService;

    @BeforeEach
    void setUp() {
        dao = new InMemoryLeadDAO();
        leadService = new LeadService(dao);
    }

    @Test
    @DisplayName("Tiêu chí 1: Tạo và sửa lead đầy đủ họ tên, SĐT, email, nguồn, chương trình quan tâm")
    void testCreateAndEditLeadFullFields() throws SQLException {
        // 1. Tạo mới lead
        Lead newLead = new Lead();
        newLead.setFullName("Nguyễn Văn Anh");
        newLead.setPhone("0912345678");
        newLead.setEmail("vananh.nguyen@gmail.com");
        newLead.setSource("FACEBOOK");
        newLead.setProgramInterest("Lập trình Java Web Fullstack");
        newLead.setStatus("NEW");
        newLead.setNotes("Khách muốn học ca tối thứ 2-4-6");

        List<String> createErrors = leadService.validateLead(newLead, false);
        assertTrue(createErrors.isEmpty(), "Dữ liệu hợp lệ không được có lỗi validation");

        long createdId = leadService.createLead(newLead);
        assertTrue(createdId > 0, "Lead mới phải có ID tự sinh hợp lệ");

        Lead fetched = leadService.findById(createdId);
        assertNotNull(fetched);
        assertEquals("Nguyễn Văn Anh", fetched.getFullName());
        assertEquals("0912345678", fetched.getPhone());
        assertEquals("vananh.nguyen@gmail.com", fetched.getEmail());
        assertEquals("FACEBOOK", fetched.getSource());
        assertEquals("Lập trình Java Web Fullstack", fetched.getProgramInterest());
        assertEquals("NEW", fetched.getStatus());

        // 2. Chỉnh sửa lead
        fetched.setFullName("Nguyễn Văn Anh (Đã cập nhật)");
        fetched.setProgramInterest("Lập trình Python & AI");
        fetched.setStatus("CONSULTING");
        fetched.setNotes("Đã gọi điện lần 1, khách chuyển sang quan tâm khóa AI");

        List<String> updateErrors = leadService.validateLead(fetched, true);
        assertTrue(updateErrors.isEmpty());

        boolean updateSuccess = leadService.updateLead(fetched);
        assertTrue(updateSuccess);

        Lead updated = leadService.findById(createdId);
        assertEquals("Nguyễn Văn Anh (Đã cập nhật)", updated.getFullName());
        assertEquals("Lập trình Python & AI", updated.getProgramInterest());
        assertEquals("CONSULTING", updated.getStatus());
    }

    @Test
    @DisplayName("Tiêu chí 2: Cảnh báo khi số điện thoại trùng với lead đã có")
    void testDuplicatePhoneWarning() throws SQLException {
        // Lead thứ nhất
        Lead first = new Lead("Trần Thị Hoa", "0987654321", "hoa.tran@example.com", "Tester QA");
        first.setSource("WEBSITE");
        first.setStatus("NEW");
        long firstId = leadService.createLead(first);

        // Lead thứ hai nhập cùng số điện thoại
        String duplicatePhone = "0987654321";
        Lead duplicateFound = leadService.checkDuplicatePhone(duplicatePhone, null);

        assertNotNull(duplicateFound, "Hệ thống phải phát hiện số điện thoại đã tồn tại");
        assertEquals(firstId, duplicateFound.getId());
        assertEquals("Trần Thị Hoa", duplicateFound.getFullName());

        // Kiểm tra biến thể định dạng (có dấu cách, dấu gạch nối) vẫn phát hiện trùng
        Lead duplicateWithFormatting = leadService.checkDuplicatePhone("0987-654-321", null);
        assertNotNull(duplicateWithFormatting, "Định dạng có gạch nối vẫn phải phát hiện trùng");

        // Khi sửa chính lead đó thì không bị coi là trùng với bản thân
        Lead selfDuplicate = leadService.checkDuplicatePhone(duplicatePhone, firstId);
        assertNull(selfDuplicate, "Cập nhật chính lead hiện tại với số điện thoại cũ không bị coi là trùng");
    }

    @Test
    @DisplayName("Tiêu chí 3: Phân quyền - Chỉ Quản lý đào tạo (Training Manager) và Admin được xóa lead; Admissions TUYỆT ĐỐI KHÔNG được xóa")
    void testOnlyTrainingManagerCanDeleteLead() throws SQLException {
        User admissions = new User(1L, "Tư Vấn Tuyển Sinh", "admissions@ems.edu.vn", "ACTIVE",
                Collections.singletonList(RoleConstant.ADMISSIONS));

        User trainingManager = new User(2L, "Quản Lý Đào Tạo", "tm@ems.edu.vn", "ACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));

        User admin = new User(3L, "Quản Trị Viên", "admin@ems.edu.vn", "ACTIVE",
                Collections.singletonList(RoleConstant.ADMIN));

        // 1. Kiểm tra quyền của Tư vấn tuyển sinh (Admissions):
        assertTrue(LeadService.canCreateLead(admissions), "Admissions được tạo lead");
        assertTrue(LeadService.canReadLead(admissions), "Admissions được xem lead");
        assertTrue(LeadService.canUpdateLead(admissions), "Admissions được sửa lead");
        assertFalse(LeadService.canDeleteLead(admissions),
                "Tiêu chí S2-09: Tư vấn tuyển sinh TUYỆT ĐỐI KHÔNG được xóa lead!");
        assertFalse(PermissionConstant.hasPermission(admissions.getRoles(), PermissionConstant.LEAD_DELETE),
                "Ma trận phân quyền không được cấp LEAD_DELETE cho Admissions");

        // 2. Kiểm tra quyền của Quản lý đào tạo (Training Manager):
        assertTrue(LeadService.canCreateLead(trainingManager));
        assertTrue(LeadService.canReadLead(trainingManager));
        assertTrue(LeadService.canUpdateLead(trainingManager));
        assertTrue(LeadService.canDeleteLead(trainingManager),
                "Tiêu chí S2-09: Chỉ Quản lý đào tạo (và Admin) được xóa lead");
        assertTrue(PermissionConstant.hasPermission(trainingManager.getRoles(), PermissionConstant.LEAD_DELETE));

        // 3. Kiểm tra quyền của Admin:
        assertTrue(LeadService.canDeleteLead(admin));

        // 4. Thực thi hành động xóa trên dữ liệu:
        Lead leadToDelete = new Lead("Lead Cần Xóa", "0901234567", "delete@example.com", "Khóa ngắn hạn");
        long leadId = leadService.createLead(leadToDelete);
        assertNotNull(leadService.findById(leadId));

        // Khi Quản lý đào tạo thực hiện xóa:
        if (LeadService.canDeleteLead(trainingManager)) {
            boolean deleted = leadService.deleteLead(leadId);
            assertTrue(deleted);
            assertNull(leadService.findById(leadId), "Lead phải bị xóa khỏi hệ thống");
        }
    }
}
