package vn.edu.ictu.ems;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.edu.ictu.ems.constant.PermissionConstant;
import vn.edu.ictu.ems.constant.RoleConstant;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động phân quyền theo vai trò (User Story IDTTX-20)
 * Kiểm tra ít nhất 3 vai trò: Giảng viên, Kế toán, Admin, Học viên
 * Người thực hiện: Nguyễn Minh Ngọc (MN)
 */
public class RoleAuthorizationTest {

    @Test
    @DisplayName("Kiểm tra hệ thống khai báo đầy đủ 8 vai trò nghiệp vụ")
    void testEightBusinessRolesDefined() {
        assertTrue(RoleConstant.isValidRole(RoleConstant.ADMIN));
        assertTrue(RoleConstant.isValidRole(RoleConstant.TRAINING_MANAGER));
        assertTrue(RoleConstant.isValidRole(RoleConstant.ADMISSIONS));
        assertTrue(RoleConstant.isValidRole(RoleConstant.INSTRUCTOR));
        assertTrue(RoleConstant.isValidRole(RoleConstant.TEACHING_ASSISTANT));
        assertTrue(RoleConstant.isValidRole(RoleConstant.ACCOUNTANT));
        assertTrue(RoleConstant.isValidRole(RoleConstant.STUDENT));
        assertTrue(RoleConstant.isValidRole(RoleConstant.GUEST));
    }

    @Test
    @DisplayName("Vai trò Giảng viên (Instructor): CÓ quyền sửa điểm, KHÔNG CÓ quyền sửa học phí")
    void testInstructorPermissions() {
        List<String> instructorRoles = Collections.singletonList(RoleConstant.INSTRUCTOR);

        // 1. Giảng viên được sửa điểm
        assertTrue(PermissionConstant.hasPermission(instructorRoles, PermissionConstant.GRADE_EDIT),
                "Giảng viên phải có quyền chỉnh sửa điểm số học viên");

        // 2. Giảng viên KHÔNG được sửa học phí
        assertFalse(PermissionConstant.hasPermission(instructorRoles, PermissionConstant.TUITION_EDIT),
                "Giảng viên tuyệt đối không được phép chỉnh sửa học phí");
    }

    @Test
    @DisplayName("Vai trò Kế toán (Accountant): CÓ quyền sửa học phí, KHÔNG CÓ quyền sửa điểm")
    void testAccountantPermissions() {
        List<String> accountantRoles = Collections.singletonList(RoleConstant.ACCOUNTANT);

        // 1. Kế toán được sửa học phí
        assertTrue(PermissionConstant.hasPermission(accountantRoles, PermissionConstant.TUITION_EDIT),
                "Kế toán phải có quyền chỉnh sửa và thu học phí");

        // 2. Kế toán KHÔNG được sửa điểm
        assertFalse(PermissionConstant.hasPermission(accountantRoles, PermissionConstant.GRADE_EDIT),
                "Kế toán tuyệt đối không được phép chỉnh sửa điểm số học viên");

        // 3. Kế toán được xem điểm
        assertTrue(PermissionConstant.hasPermission(accountantRoles, PermissionConstant.GRADE_VIEW),
                "Kế toán được phép xem điểm số học viên");
    }

    @Test
    @DisplayName("Vai trò Quản trị hệ thống (Admin): Có toàn quyền cả sửa điểm và sửa học phí")
    void testAdminPermissions() {
        List<String> adminRoles = Collections.singletonList(RoleConstant.ADMIN);

        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.GRADE_EDIT));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.TUITION_EDIT));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.USER_MANAGE));
        assertTrue(PermissionConstant.hasPermission(adminRoles, PermissionConstant.ROLE_MANAGE));
    }

    @Test
    @DisplayName("Vai trò Học viên (Student): Bị từ chối cả sửa điểm và sửa học phí")
    void testStudentPermissions() {
        List<String> studentRoles = Collections.singletonList(RoleConstant.STUDENT);

        assertFalse(PermissionConstant.hasPermission(studentRoles, PermissionConstant.GRADE_EDIT));
        assertFalse(PermissionConstant.hasPermission(studentRoles, PermissionConstant.TUITION_EDIT));
        assertTrue(PermissionConstant.hasPermission(studentRoles, PermissionConstant.GRADE_VIEW));
        assertTrue(PermissionConstant.hasPermission(studentRoles, PermissionConstant.TUITION_VIEW));
    }

    @Test
    @DisplayName("Nguyên tắc mặc định từ chối (Default-Deny): Tài khoản không có vai trò hợp lệ đều bị từ chối")
    void testDefaultDenyPrinciple() {
        assertFalse(PermissionConstant.hasPermission(Collections.emptyList(), PermissionConstant.GRADE_EDIT));
        assertFalse(PermissionConstant.hasPermission(Collections.emptyList(), PermissionConstant.TUITION_EDIT));
        assertFalse(PermissionConstant.hasPermission(null, PermissionConstant.GRADE_EDIT));
        assertFalse(PermissionConstant.hasPermission(Collections.singletonList("UnknownRole"), PermissionConstant.GRADE_EDIT));
    }
}
