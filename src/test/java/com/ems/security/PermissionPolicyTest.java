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
        assertEquals(null, PermissionPolicy.requiredPermission("DELETE", "/api/admin/roles/INSTRUCTOR"));
        assertEquals(null, PermissionPolicy.requiredPermission("PATCH", "/api/admin/users/8"));
    }

    @Test
    void profileImportAndTrashRoutesHaveRules() {
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("GET", "/api/profile"));
        assertEquals("@authenticated", PermissionPolicy.requiredPermission("PUT", "/api/profile"));
        assertEquals("USER_CREATE", PermissionPolicy.requiredPermission("POST", "/api/admin/users/import/preview"));
        assertEquals("USER_CREATE", PermissionPolicy.requiredPermission("POST", "/api/admin/users/import"));
        assertEquals("USER_CREATE", PermissionPolicy.requiredPermission("GET", "/api/admin/users/import/template"));
        assertEquals("USER_READ", PermissionPolicy.requiredPermission("GET", "/api/admin/users/trash"));
        assertEquals("USER_ROLE_ASSIGN", PermissionPolicy.requiredPermission("DELETE", "/api/admin/users/8"));
        assertEquals("USER_ROLE_ASSIGN", PermissionPolicy.requiredPermission("DELETE", "/api/admin/users/8/purge"));
        assertEquals("USER_ROLE_ASSIGN", PermissionPolicy.requiredPermission("POST", "/api/admin/users/8/restore"));
        assertEquals("PROGRAM_MANAGE", PermissionPolicy.requiredPermission("PUT", "/api/training-programs/WEB-FS/subjects"));
    }

    @Test
    void userMayHoldMultipleRolesButCannotRemoveTheirOwnAdminRole() {
        assertTrue(PermissionPolicy.mayReplaceRoles(4, 5, List.of("ADMIN"), List.of("STUDENT")));
        assertFalse(PermissionPolicy.mayReplaceRoles(4, 4, List.of("ADMIN"), List.of("STUDENT")));
        assertTrue(PermissionPolicy.mayReplaceRoles(4, 4, List.of("ADMIN"), List.of("ADMIN", "INSTRUCTOR")));
    }
}