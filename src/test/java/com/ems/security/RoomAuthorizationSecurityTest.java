package com.ems.security;

import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử bảo mật & phân quyền chức năng Quản lý phòng học (User Story S3-09 / Bước 5)
 */
class RoomAuthorizationSecurityTest {

    @Test
    @DisplayName("Chỉ Training Manager và Admin được cấp quyền quản lý phòng học (ROOM_MANAGE & ROOM_VIEW)")
    void testTrainingManagerAndAdminHaveRoomPermissions() {
        List<String> tmRoles = List.of(RoleConstant.TRAINING_MANAGER);
        List<String> adminRoles = List.of(RoleConstant.ADMIN);

        // Training Manager được quản lý phòng
        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.ROOM_VIEW),
                "Training Manager phải có quyền xem danh sách phòng học");
        assertTrue(PermissionConstant.hasPermission(tmRoles, PermissionConstant.ROOM_MANAGE),
                "Training Manager phải có quyền quản trị phòng học");

        // Admin có toàn quyền
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.ROOM_VIEW),
                "Admin phải có quyền xem danh sách phòng học");
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.ROOM_MANAGE),
                "Admin phải có quyền quản trị phòng học");
    }

    @Test
    @DisplayName("Các vai trò khác (Giảng viên, Kế toán, Học viên, Tuyển sinh, Khách) TUYỆT ĐỐI KHÔNG có quyền quản lý phòng")
    void testOtherRolesCannotManageRooms() {
        List<String> instructor = List.of(RoleConstant.INSTRUCTOR);
        List<String> accountant = List.of(RoleConstant.ACCOUNTANT);
        List<String> student = List.of(RoleConstant.STUDENT);
        List<String> admissions = List.of(RoleConstant.ADMISSIONS);
        List<String> guest = List.of(RoleConstant.GUEST);

        assertFalse(PermissionConstant.hasPermission(instructor, PermissionConstant.ROOM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(accountant, PermissionConstant.ROOM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(student, PermissionConstant.ROOM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(admissions, PermissionConstant.ROOM_MANAGE));
        assertFalse(PermissionConstant.hasPermission(guest, PermissionConstant.ROOM_MANAGE));
    }

    @Test
    @DisplayName("Kiểm tra ánh xạ Endpoint API phòng học sang Permission Policy")
    void testPermissionPolicyRoutingForRoomEndpoints() {
        assertEquals("ROOM_VIEW", PermissionPolicy.requiredPermission("GET", "/api/rooms"));
        assertEquals("ROOM_MANAGE", PermissionPolicy.requiredPermission("POST", "/api/rooms"));
        assertEquals("ROOM_VIEW", PermissionPolicy.requiredPermission("GET", "/api/rooms/10"));
        assertEquals("ROOM_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/rooms/10"));
        assertEquals("ROOM_MANAGE", PermissionPolicy.requiredPermission("DELETE", "/api/rooms/10"));
        assertEquals("ROOM_VIEW", PermissionPolicy.requiredPermission("GET", "/api/rooms/10/validate-capacity"));
        assertEquals("ROOM_VIEW", PermissionPolicy.requiredPermission("POST", "/api/rooms/10/validate-capacity"));
    }

    @Test
    @DisplayName("Kiểm tra PermissionPolicy.allows cho quyền ROOM_VIEW và ROOM_MANAGE")
    void testPermissionPolicyAllowsEvaluation() {
        Set<String> tmGrants = Set.of("ROOM_VIEW", "ROOM_MANAGE", "CLASS_MANAGE");
        Set<String> studentGrants = Set.of("GRADE_READ", "SCHEDULE_READ");
        Set<String> legacyGrants = Set.of("CLASS_MANAGE");

        assertTrue(PermissionPolicy.allows(tmGrants, "ROOM_MANAGE"));
        assertTrue(PermissionPolicy.allows(tmGrants, "ROOM_VIEW"));
        assertTrue(PermissionPolicy.allows(legacyGrants, "ROOM_MANAGE"), "User có CLASS_MANAGE được tự động cấp quyền ROOM");
        assertFalse(PermissionPolicy.allows(studentGrants, "ROOM_MANAGE"));
        assertFalse(PermissionPolicy.allows(studentGrants, "ROOM_VIEW"));
        assertFalse(PermissionPolicy.allows(Collections.emptySet(), "ROOM_MANAGE"));
        assertFalse(PermissionPolicy.allows(null, "ROOM_MANAGE"));
    }
}
