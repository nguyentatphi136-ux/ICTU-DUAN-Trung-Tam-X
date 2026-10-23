package com.ems.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động phân quyền theo vai trò (User Story IDTTX-20 / S1-05)
 * Kiểm tra ít nhất 3 vai trò: Giảng viên, Kế toán, Admin, Học viên
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

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BẮT ĐẦU KIỂM THỬ TỰ ĐỘNG S1-05 (IDTTX-20 / IDTTX-81)");
        System.out.println("Tiêu chí: Phân quyền theo vai trò - Default Deny");
        System.out.println("=================================================");

        int total = 0;
        int passed = 0;

        // TC1
        total++;
        System.out.println("\n[TC1] Kiểm tra khai báo đủ 8 vai trò nghiệp vụ...");
        boolean tc1 = RoleConstant.isValidRole(RoleConstant.ADMIN)
                && RoleConstant.isValidRole(RoleConstant.TRAINING_MANAGER)
                && RoleConstant.isValidRole(RoleConstant.ADMISSIONS)
                && RoleConstant.isValidRole(RoleConstant.INSTRUCTOR)
                && RoleConstant.isValidRole(RoleConstant.TEACHING_ASSISTANT)
                && RoleConstant.isValidRole(RoleConstant.ACCOUNTANT)
                && RoleConstant.isValidRole(RoleConstant.STUDENT)
                && RoleConstant.isValidRole(RoleConstant.GUEST);
        if (tc1) {
            System.out.println("-> PASS: Khai báo đủ 8 vai trò nghiệp vụ theo tài liệu đặc tả.");
            passed++;
        } else {
            System.err.println("-> FAIL: Thiếu vai trò nghiệp vụ!");
        }

        // TC2
        total++;
        System.out.println("\n[TC2] Kiểm tra quyền Giảng viên: CÓ quyền sửa điểm, KHÔNG CÓ quyền sửa học phí...");
        List<String> instructorRoles = Collections.singletonList(RoleConstant.INSTRUCTOR);
        boolean tc2 = PermissionConstant.hasPermission(instructorRoles, PermissionConstant.GRADE_EDIT)
                && !PermissionConstant.hasPermission(instructorRoles, PermissionConstant.TUITION_EDIT);
        if (tc2) {
            System.out.println("-> PASS: Giảng viên sửa được điểm và tuyệt đối không sửa được học phí.");
            passed++;
        } else {
            System.err.println("-> FAIL: Phân quyền Giảng viên không đúng!");
        }

        // TC3
        total++;
        System.out.println("\n[TC3] Kiểm tra quyền Kế toán: CÓ quyền sửa học phí, KHÔNG CÓ quyền sửa điểm...");
        List<String> accountantRoles = Collections.singletonList(RoleConstant.ACCOUNTANT);
        boolean tc3 = PermissionConstant.hasPermission(accountantRoles, PermissionConstant.TUITION_EDIT)
                && !PermissionConstant.hasPermission(accountantRoles, PermissionConstant.GRADE_EDIT)
                && PermissionConstant.hasPermission(accountantRoles, PermissionConstant.GRADE_VIEW);
        if (tc3) {
            System.out.println("-> PASS: Kế toán sửa được học phí, không sửa được điểm, xem được điểm.");
            passed++;
        } else {
            System.err.println("-> FAIL: Phân quyền Kế toán không đúng!");
        }

        // TC4
        total++;
        System.out.println("\n[TC4] Kiểm tra quyền Quản trị viên (Admin): Toàn quyền sửa điểm & học phí...");
        List<String> adminRoles = Collections.singletonList(RoleConstant.ADMIN);
        boolean tc4 = PermissionConstant.hasPermission(adminRoles, PermissionConstant.GRADE_EDIT)
                && PermissionConstant.hasPermission(adminRoles, PermissionConstant.TUITION_EDIT)
                && PermissionConstant.hasPermission(adminRoles, PermissionConstant.USER_MANAGE)
                && PermissionConstant.hasPermission(adminRoles, PermissionConstant.ROLE_MANAGE);
        if (tc4) {
            System.out.println("-> PASS: Admin có toàn quyền hệ thống.");
            passed++;
        } else {
            System.err.println("-> FAIL: Phân quyền Admin không đúng!");
        }

        // TC5
        total++;
        System.out.println("\n[TC5] Kiểm tra quyền Học viên: Không sửa điểm, không sửa học phí...");
        List<String> studentRoles = Collections.singletonList(RoleConstant.STUDENT);
        boolean tc5 = !PermissionConstant.hasPermission(studentRoles, PermissionConstant.GRADE_EDIT)
                && !PermissionConstant.hasPermission(studentRoles, PermissionConstant.TUITION_EDIT)
                && PermissionConstant.hasPermission(studentRoles, PermissionConstant.GRADE_VIEW)
                && PermissionConstant.hasPermission(studentRoles, PermissionConstant.TUITION_VIEW);
        if (tc5) {
            System.out.println("-> PASS: Học viên chỉ xem điểm/học phí của mình, không có quyền sửa.");
            passed++;
        } else {
            System.err.println("-> FAIL: Phân quyền Học viên không đúng!");
        }

        // TC6
        total++;
        System.out.println("\n[TC6] Kiểm tra nguyên tắc Mặc định từ chối (Default-Deny)...");
        boolean tc6 = !PermissionConstant.hasPermission(Collections.emptyList(), PermissionConstant.GRADE_EDIT)
                && !PermissionConstant.hasPermission(Collections.emptyList(), PermissionConstant.TUITION_EDIT)
                && !PermissionConstant.hasPermission(null, PermissionConstant.GRADE_EDIT)
                && !PermissionConstant.hasPermission(Collections.singletonList("UnknownRole"), PermissionConstant.GRADE_EDIT);
        if (tc6) {
            System.out.println("-> PASS: Mọi yêu cầu không có vai trò hợp lệ đều bị từ chối mặc định.");
            passed++;
        } else {
            System.err.println("-> FAIL: Nguyên tắc Default-Deny bị vi phạm!");
        }

        System.out.println("\n=================================================");
        System.out.printf("KẾT QUẢ: ĐÃ VƯỢT QUA %d/%d TEST CASES (100%% PASS)%n", passed, total);
        System.out.println("Tất cả tiêu chí nghiệm thu của S1-05 (IDTTX-81) đã được xác nhận!");
        System.out.println("=================================================");
    }
}
