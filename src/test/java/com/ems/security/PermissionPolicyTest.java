package com.ems.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PermissionPolicyTest {
    @Test
    void enforcesLeastPrivilegeForAdminInstructorAndStudent() {
        Set<String> admin = Set.of("ROLE_PERMISSION_READ", "ROLE_PERMISSION_UPDATE", "USER_ROLE_ASSIGN", "MENU_VIEW");
        Set<String> instructor = Set.of("MENU_VIEW", "ATTENDANCE_MANAGE", "ASSIGNMENT_MANAGE", "GRADE_MANAGE");
        Set<String> student = Set.of("MENU_VIEW", "ASSIGNMENT_READ", "GRADE_READ");

        assertTrue(PermissionPolicy.allows(admin, "ROLE_PERMISSION_UPDATE"));
        assertFalse(PermissionPolicy.allows(instructor, "ROLE_PERMISSION_UPDATE"));
        assertTrue(PermissionPolicy.allows(instructor, "ATTENDANCE_MANAGE"));
        assertTrue(PermissionPolicy.allows(student, "GRADE_READ"));
        assertFalse(PermissionPolicy.allows(student, "GRADE_MANAGE"));
        assertFalse(PermissionPolicy.allows(student, "UNKNOWN_PERMISSION"));
    }

    @Test
    void unknownApiOperationsAreDeniedAndRoleUpdatesUsePermissionCodes() {
        assertEquals("ROLE_PERMISSION_UPDATE",
                PermissionPolicy.requiredPermission("PUT", "/api/admin/roles/INSTRUCTOR/permissions"));
        assertEquals("USER_ROLE_ASSIGN",
                PermissionPolicy.requiredPermission("PUT", "/api/admin/users/8/roles"));
        assertEquals("MENU_VIEW", PermissionPolicy.requiredPermission("GET", "/api/me/menu"));
        assertEquals(null, PermissionPolicy.requiredPermission("DELETE", "/api/admin/users/8"));
    }

    @Test
    void userMayHoldMultipleRolesButCannotRemoveTheirOwnAdminRole() {
        assertTrue(PermissionPolicy.mayReplaceRoles(4, 5, List.of("ADMIN"), List.of("STUDENT")));
        assertFalse(PermissionPolicy.mayReplaceRoles(4, 4, List.of("ADMIN"), List.of("STUDENT")));
        assertTrue(PermissionPolicy.mayReplaceRoles(4, 4, List.of("ADMIN"), List.of("ADMIN", "INSTRUCTOR")));
    }

    @Test
    void attendanceRoutesOfS4HaveRules() {
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/attendance/session/401"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/attendance/session/401"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/attendance/session/401/all"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/attendance/session/401/logs"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/sessions/401/attendance"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/sessions/401/attendance"));
    }

    @Test
    void transcriptRoutesOfS8HaveRules() {
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/transcript/mine"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/transcripts/mine"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/transcripts/student/HV260101"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/transcripts/student/HV260101/pdf"));
    }

    @Test
    void paymentRoutesOfS7HaveRules() {
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/payments/student/HV260142"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/payments"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/payments/receipt/BL-2026-000001"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/payments/receipt/BL-2026-000001/pdf"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("POST", "/api/payments/BL-2026-000001/adjustments"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/receipts/BL-2026-000001"));
    }
}