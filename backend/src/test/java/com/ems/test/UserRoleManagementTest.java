package com.ems.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.ems.constant.PermissionConstant;
import com.ems.constant.RoleConstant;
import com.ems.model.User;
import com.ems.service.UserStore;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tự động Quản lý Phân bổ & Thu hồi Vai trò (User Story IDTTX-24)
 * Tiêu chí nghiệm thu (Acceptance Criteria):
 *   1. Một người dùng có thể giữ nhiều vai trò cùng lúc (vd: vừa là Giảng viên vừa là Quản lý đào tạo).
 *   2. Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại.
 *   3. Không thể tự thu hồi vai trò quản trị (Admin) của chính mình.
 *   4. Gán và thu hồi hợp lệ với các vai trò trong danh mục.
 * 
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class UserRoleManagementTest {

    private static final long ADMIN_ID = 1L;
    private static final long INSTRUCTOR_ID = 2L;
    private static final long STUDENT_ID = 3L;

    @BeforeEach
    void setUp() {
        UserStore.resetToDefault();
    }

    @Test
    @DisplayName("IDTTX-24: Một người dùng có thể giữ nhiều vai trò cùng lúc (vừa Giảng viên vừa Quản lý đào tạo)")
    void testUserCanHoldMultipleRoles_InstructorAndTrainingManager() {
        User instructor = UserStore.findById(INSTRUCTOR_ID);
        assertNotNull(instructor);
        assertEquals(1, instructor.getRoles().size());
        assertTrue(instructor.hasRole(RoleConstant.INSTRUCTOR));
        assertFalse(instructor.hasRole(RoleConstant.TRAINING_MANAGER));

        // Admin gán thêm vai trò TrainingManager cho giảng viên
        UserStore.assignRole(ADMIN_ID, INSTRUCTOR_ID, RoleConstant.TRAINING_MANAGER);

        // Kiểm tra giảng viên hiện tại sở hữu cả 2 vai trò
        assertEquals(2, instructor.getRoles().size());
        assertTrue(instructor.hasRole(RoleConstant.INSTRUCTOR), "Vẫn giữ vai trò Giảng viên");
        assertTrue(instructor.hasRole(RoleConstant.TRAINING_MANAGER), "Được bổ sung vai trò Quản lý đào tạo");

        // Kiểm tra quyền hạn kép: có cả quyền của Instructor (sửa điểm) và TrainingManager (quản lý lớp)
        assertTrue(PermissionConstant.hasPermission(instructor.getRoles(), PermissionConstant.GRADE_EDIT));
        assertTrue(PermissionConstant.hasPermission(instructor.getRoles(), PermissionConstant.CLASS_MANAGE));
    }

    @Test
    @DisplayName("IDTTX-24: Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại")
    void testImmediateEffectOnNextOperation() {
        User student = UserStore.findById(STUDENT_ID);
        assertNotNull(student);
        assertFalse(PermissionConstant.hasPermission(student.getRoles(), PermissionConstant.GRADE_VIEW) &&
                    PermissionConstant.hasPermission(student.getRoles(), PermissionConstant.GRADE_EDIT));

        // Gán vai trò Instructor cho học viên
        UserStore.assignRole(ADMIN_ID, STUDENT_ID, RoleConstant.INSTRUCTOR);

        // Lấy lại user từ UserStore mô phỏng thao tác kế tiếp trong Filter
        User refreshedUser = UserStore.findById(STUDENT_ID);
        assertTrue(refreshedUser.hasRole(RoleConstant.INSTRUCTOR), "Vai trò mới có hiệu lực ngay lập tức");
        assertTrue(PermissionConstant.hasPermission(refreshedUser.getRoles(), PermissionConstant.GRADE_EDIT),
                "Quyền sửa điểm có hiệu lực ngay ở thao tác tiếp theo");

        // Thu hồi vai trò Instructor
        UserStore.revokeRole(ADMIN_ID, STUDENT_ID, RoleConstant.INSTRUCTOR);
        User userAfterRevoke = UserStore.findById(STUDENT_ID);
        assertFalse(userAfterRevoke.hasRole(RoleConstant.INSTRUCTOR), "Thu hồi vai trò có hiệu lực ngay");
        assertFalse(PermissionConstant.hasPermission(userAfterRevoke.getRoles(), PermissionConstant.GRADE_EDIT),
                "Quyền sửa điểm bị thu hồi ngay lập tức");
    }

    @Test
    @DisplayName("IDTTX-24: Admin TUYỆT ĐỐI KHÔNG THỂ tự thu hồi vai trò Quản trị (Admin) của chính mình")
    void testAdminCannotRevokeOwnAdminRole() {
        User admin = UserStore.findById(ADMIN_ID);
        assertNotNull(admin);
        assertTrue(admin.hasRole(RoleConstant.ADMIN));

        // Admin (ID 1) tự thu hồi role Admin của chính mình (ID 1)
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            UserStore.revokeRole(ADMIN_ID, ADMIN_ID, RoleConstant.ADMIN);
        });

        assertTrue(exception.getMessage().contains("CANNOT_REVOKE_OWN_ADMIN"),
                "Hệ thống phải chặn với thông báo CANNOT_REVOKE_OWN_ADMIN");

        // Đảm bảo Admin vẫn giữ nguyên quyền sau khi bị từ chối
        assertTrue(admin.hasRole(RoleConstant.ADMIN), "Vai trò Admin của chính mình phải được bảo toàn");
    }

    @Test
    @DisplayName("IDTTX-24: Admin vẫn có thể thu hồi vai trò Admin của tài khoản quản trị khác")
    void testAdminCanRevokeOtherAdminRole() {
        // Gán quyền Admin cho học viên (ID 3)
        UserStore.assignRole(ADMIN_ID, STUDENT_ID, RoleConstant.ADMIN);
        assertTrue(UserStore.findById(STUDENT_ID).hasRole(RoleConstant.ADMIN));

        // Admin gốc (ID 1) thu hồi quyền Admin của học viên (ID 3)
        assertDoesNotThrow(() -> {
            UserStore.revokeRole(ADMIN_ID, STUDENT_ID, RoleConstant.ADMIN);
        });

        assertFalse(UserStore.findById(STUDENT_ID).hasRole(RoleConstant.ADMIN),
                "Quyền Admin của tài khoản khác đã được thu hồi thành công");
    }

    @Test
    @DisplayName("IDTTX-24: Thu hồi một vai trò, các vai trò còn lại vẫn được giữ nguyên đầy đủ")
    void testRevokeOneRoleRetainsOtherRoles() {
        // Gán thêm Trợ giảng và Quản lý đào tạo
        UserStore.assignRole(ADMIN_ID, INSTRUCTOR_ID, RoleConstant.TEACHING_ASSISTANT);
        UserStore.assignRole(ADMIN_ID, INSTRUCTOR_ID, RoleConstant.TRAINING_MANAGER);

        User user = UserStore.findById(INSTRUCTOR_ID);
        assertEquals(3, user.getRoles().size());

        // Thu hồi chỉ vai trò TeachingAssistant
        UserStore.revokeRole(ADMIN_ID, INSTRUCTOR_ID, RoleConstant.TEACHING_ASSISTANT);

        assertEquals(2, user.getRoles().size());
        assertTrue(user.hasRole(RoleConstant.INSTRUCTOR));
        assertTrue(user.hasRole(RoleConstant.TRAINING_MANAGER));
        assertFalse(user.hasRole(RoleConstant.TEACHING_ASSISTANT));
    }

    @Test
    @DisplayName("IDTTX-24: Báo lỗi khi gán trùng vai trò đã có (ROLE_ALREADY_ASSIGNED)")
    void testDuplicateRoleAssignmentFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            UserStore.assignRole(ADMIN_ID, INSTRUCTOR_ID, RoleConstant.INSTRUCTOR);
        });
        assertTrue(ex.getMessage().contains("ROLE_ALREADY_ASSIGNED"));
    }

    @Test
    @DisplayName("IDTTX-24: Báo lỗi khi thu hồi vai trò mà người dùng không có (ROLE_NOT_ASSIGNED)")
    void testRevokeUnassignedRoleFails() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            UserStore.revokeRole(ADMIN_ID, STUDENT_ID, RoleConstant.ACCOUNTANT);
        });
        assertTrue(ex.getMessage().contains("ROLE_NOT_ASSIGNED"));
    }

    @Test
    @DisplayName("IDTTX-24: Báo lỗi khi truyền vai trò không hợp lệ hoặc người dùng không tồn tại")
    void testInvalidRoleOrUserFails() {
        assertThrows(IllegalArgumentException.class, () -> {
            UserStore.assignRole(ADMIN_ID, STUDENT_ID, "InvalidFakeRole");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            UserStore.revokeRole(ADMIN_ID, STUDENT_ID, "InvalidFakeRole");
        });

        assertThrows(NoSuchElementException.class, () -> {
            UserStore.assignRole(ADMIN_ID, 9999L, RoleConstant.STUDENT);
        });
    }
}
