package com.ems.test;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.security.PermissionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động Phân quyền truy cập chức năng Quản lý chương trình đào tạo (S2-04)
 * Yêu cầu: TrainingManager và Admin CÓ quyền, các vai trò khác KHÔNG CÓ quyền.
 */
public class TrainingProgramAuthTest {

    @Test
    @DisplayName("14. Quản lý đào tạo (TrainingManager): CÓ QUYỀN PROGRAM_MANAGE")
    void testTrainingManagerHasProgramManagePermission() {
        List<String> roles = Collections.singletonList(RoleConstant.TRAINING_MANAGER);
        assertTrue(PermissionConstant.hasPermission(roles, PermissionConstant.PROGRAM_MANAGE),
                "Quản lý đào tạo phải có quyền PROGRAM_MANAGE");
    }

    @Test
    @DisplayName("14. Quản trị hệ thống (Admin): CÓ QUYỀN PROGRAM_MANAGE")
    void testAdminHasProgramManagePermission() {
        List<String> roles = Collections.singletonList(RoleConstant.ADMIN);
        assertTrue(PermissionConstant.hasPermission(roles, PermissionConstant.PROGRAM_MANAGE),
                "Admin phải có quyền PROGRAM_MANAGE");
    }

    @Test
    @DisplayName("14. Các vai trò khác (Admissions, Instructor, Student, etc.): KHÔNG CÓ QUYỀN PROGRAM_MANAGE")
    void testOtherRolesDeniedProgramManagePermission() {
        // Tư vấn tuyển sinh
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.ADMISSIONS), PermissionConstant.PROGRAM_MANAGE));

        // Giảng viên
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.INSTRUCTOR), PermissionConstant.PROGRAM_MANAGE));

        // Trợ giảng
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.TEACHING_ASSISTANT), PermissionConstant.PROGRAM_MANAGE));

        // Kế toán
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.ACCOUNTANT), PermissionConstant.PROGRAM_MANAGE));

        // Học viên
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.STUDENT), PermissionConstant.PROGRAM_MANAGE));

        // Khách
        assertFalse(PermissionConstant.hasPermission(
                Collections.singletonList(RoleConstant.GUEST), PermissionConstant.PROGRAM_MANAGE));
    }

    @Test
    @DisplayName("Default Deny: Người dùng không có vai trò hoặc vai trò lạ đều bị từ chối")
    void testDefaultDenyForUnknownRoles() {
        assertFalse(PermissionConstant.hasPermission(Collections.emptyList(), PermissionConstant.PROGRAM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(null, PermissionConstant.PROGRAM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(Collections.singletonList("UNKNOWN_ROLE"), PermissionConstant.PROGRAM_MANAGE));
    }

    @Test
    @DisplayName("PermissionPolicy: Các endpoint /api/training-programs yêu cầu quyền PROGRAM_MANAGE")
    void testPermissionPolicyMapping() {
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/training-programs"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/training-programs"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("GET", "/api/training-programs/1"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/training-programs/1"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("DELETE", "/api/training-programs/1"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("PATCH", "/api/training-programs/1/deactivate"));
    }
}
