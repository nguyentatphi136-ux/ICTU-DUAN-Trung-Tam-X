package com.ems.test;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.model.User;
import com.ems.security.PermissionPolicy;
import com.ems.service.LeadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử ma trận phân quyền RBAC cho Module Lead (S2-09 [IDTTX-45])
 * Tiêu chí cốt lõi:
 * 1. Tư vấn tuyển sinh (Admissions): Được Tạo, Đọc, Sửa (Create/Read/Update). TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA!
 * 2. Quản lý đào tạo (Training Manager): Toàn quyền Tạo, Đọc, Sửa, XÓA (Create/Read/Update/Delete).
 * 3. Quản trị hệ thống (Admin): Toàn quyền.
 * 4. Các vai trò khác: Bị từ chối hoàn toàn.
 */
public class LeadRoleAuthorizationTest {

    @Test
    @DisplayName("Vai trò Tư vấn tuyển sinh (Admissions): CÓ quyền Xem, Tạo, Sửa; TUYỆT ĐỐI KHÔNG CÓ quyền Xóa")
    void testAdmissionsPermissions() {
        List<String> admissionsRoles = Collections.singletonList(RoleConstant.ADMISSIONS);

        // 1. Admissions được xem Lead
        assertTrue(PermissionConstant.hasPermission(admissionsRoles, PermissionConstant.LEAD_VIEW),
                "Tư vấn tuyển sinh phải có quyền tra cứu và xem danh sách lead");

        // 2. Admissions được tạo Lead
        assertTrue(PermissionConstant.hasPermission(admissionsRoles, PermissionConstant.LEAD_CREATE),
                "Tư vấn tuyển sinh phải có quyền tạo mới lead");

        // 3. Admissions được sửa Lead
        assertTrue(PermissionConstant.hasPermission(admissionsRoles, PermissionConstant.LEAD_UPDATE),
                "Tư vấn tuyển sinh phải có quyền cập nhật thông tin lead");

        // 4. TIÊU CHÍ S2-09: Chỉ Quản lý đào tạo được xóa lead -> Admissions KHÔNG ĐƯỢC XÓA
        assertFalse(PermissionConstant.hasPermission(admissionsRoles, PermissionConstant.LEAD_DELETE),
                "Tư vấn tuyển sinh TUYỆT ĐỐI KHÔNG ĐƯỢC PHÉP xóa lead (Chỉ Quản lý đào tạo hoặc Admin được xóa)");

        // Kiểm tra qua LeadService helper
        User admissionsUser = new User(10L, "Nguyễn Tư Vấn", "admissions@example.com", "ACTIVE", admissionsRoles);
        assertTrue(LeadService.canCreateLead(admissionsUser));
        assertTrue(LeadService.canReadLead(admissionsUser));
        assertTrue(LeadService.canUpdateLead(admissionsUser));
        assertFalse(LeadService.canDeleteLead(admissionsUser),
                "LeadService.canDeleteLead phải trả về false cho tài khoản Admissions");
    }

    @Test
    @DisplayName("Vai trò Quản lý đào tạo (Training Manager): CÓ toàn quyền CRUD (Tạo, Đọc, Sửa, XÓA)")
    void testTrainingManagerPermissions() {
        List<String> tmRoles = Collections.singletonList(RoleConstant.TRAINING_MANAGER);

        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.LEAD_VIEW));
        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.LEAD_CREATE));
        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.LEAD_UPDATE));
        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.LEAD_DELETE),
                "Quản lý đào tạo phải có quyền xóa lead theo tiêu chí S2-09");

        User tmUser = new User(11L, "Trần Quản Lý", "tm@example.com", "ACTIVE", tmRoles);
        assertTrue(LeadService.canCreateLead(tmUser));
        assertTrue(LeadService.canReadLead(tmUser));
        assertTrue(LeadService.canUpdateLead(tmUser));
        assertTrue(LeadService.canDeleteLead(tmUser),
                "LeadService.canDeleteLead phải trả về true cho Quản lý đào tạo");
    }

    @Test
    @DisplayName("Vai trò Quản trị hệ thống (Admin): Có toàn quyền CRUD")
    void testAdminPermissions() {
        List<String> adminRoles = Collections.singletonList(RoleConstant.ADMIN);

        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.LEAD_VIEW));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.LEAD_CREATE));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.LEAD_UPDATE));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.LEAD_DELETE));

        User adminUser = new User(1L, "Admin Root", "admin@example.com", "ACTIVE", adminRoles);
        assertTrue(LeadService.canCreateLead(adminUser));
        assertTrue(LeadService.canReadLead(adminUser));
        assertTrue(LeadService.canUpdateLead(adminUser));
        assertTrue(LeadService.canDeleteLead(adminUser));
    }

    @Test
    @DisplayName("Các vai trò khác (Instructor, Accountant, Student, Guest): Bị từ chối toàn bộ quyền Lead")
    void testOtherRolesDeniedLeadPermissions() {
        List<List<String>> deniedRoles = List.of(
                Collections.singletonList(RoleConstant.INSTRUCTOR),
                Collections.singletonList(RoleConstant.TEACHING_ASSISTANT),
                Collections.singletonList(RoleConstant.ACCOUNTANT),
                Collections.singletonList(RoleConstant.STUDENT),
                Collections.singletonList(RoleConstant.GUEST)
        );

        for (List<String> roleList : deniedRoles) {
            String roleName = roleList.get(0);
            assertFalse(PermissionConstant.hasPermission(roleList, PermissionConstant.LEAD_VIEW),
                    roleName + " không được có quyền LEAD_VIEW");
            assertFalse(PermissionConstant.hasPermission(roleList, PermissionConstant.LEAD_CREATE),
                    roleName + " không được có quyền LEAD_CREATE");
            assertFalse(PermissionConstant.hasPermission(roleList, PermissionConstant.LEAD_UPDATE),
                    roleName + " không được có quyền LEAD_UPDATE");
            assertFalse(PermissionConstant.hasPermission(roleList, PermissionConstant.LEAD_DELETE),
                    roleName + " không được có quyền LEAD_DELETE");

            User u = new User(99L, "Test User", "test@example.com", "ACTIVE", roleList);
            assertFalse(LeadService.canCreateLead(u), roleName + " không được phép gọi canCreateLead");
            assertFalse(LeadService.canReadLead(u), roleName + " không được phép gọi canReadLead");
            assertFalse(LeadService.canUpdateLead(u), roleName + " không được phép gọi canUpdateLead");
            assertFalse(LeadService.canDeleteLead(u), roleName + " không được phép gọi canDeleteLead");
        }
    }

    @Test
    @DisplayName("Kiểm tra ánh xạ đường dẫn URL và Quyền yêu cầu trong PermissionPolicy")
    void testPermissionPolicyUrlMappings() {
        assertEquals("LEAD_VIEW", PermissionPolicy.requiredPermission("GET", "/api/leads"));
        assertEquals("LEAD_CREATE", PermissionPolicy.requiredPermission("POST", "/api/leads"));
        assertEquals("LEAD_VIEW", PermissionPolicy.requiredPermission("GET", "/api/leads/check-phone"));
        assertEquals("LEAD_VIEW", PermissionPolicy.requiredPermission("GET", "/api/leads/123"));
        assertEquals("LEAD_UPDATE", PermissionPolicy.requiredPermission("PUT", "/api/leads/123"));
        assertEquals("LEAD_DELETE", PermissionPolicy.requiredPermission("DELETE", "/api/leads/123"));
    }

    @Test
    @DisplayName("Tài khoản bị khóa hoặc ngừng hoạt động không được phép thực hiện thao tác dù có vai trò")
    void testLockedOrInactiveUserDenied() {
        User lockedAdmin = new User(1L, "Admin Locked", "admin@example.com", "LOCKED",
                Collections.singletonList(RoleConstant.ADMIN));
        assertFalse(LeadService.canCreateLead(lockedAdmin));
        assertFalse(LeadService.canReadLead(lockedAdmin));
        assertFalse(LeadService.canUpdateLead(lockedAdmin));
        assertFalse(LeadService.canDeleteLead(lockedAdmin));

        User inactiveTM = new User(2L, "TM Inactive", "tm@example.com", "INACTIVE",
                Collections.singletonList(RoleConstant.TRAINING_MANAGER));
        assertFalse(LeadService.canCreateLead(inactiveTM));
        assertFalse(LeadService.canReadLead(inactiveTM));
        assertFalse(LeadService.canUpdateLead(inactiveTM));
        assertFalse(LeadService.canDeleteLead(inactiveTM));
    }
}
