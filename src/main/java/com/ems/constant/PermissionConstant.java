package com.ems.constant;

import java.util.*;

/**
 * Danh mục quyền hạn và ma trận phân quyền cho 8 vai trò nghiệp vụ (S1-05)
 * Đảm bảo: Giảng viên không sửa được học phí, Kế toán không sửa được điểm
 */
public class PermissionConstant {

    // Quyền điểm số
    public static final String GRADE_VIEW = "GRADE_VIEW";
    public static final String GRADE_EDIT = "GRADE_EDIT";

    // Quyền học phí
    public static final String TUITION_VIEW = "TUITION_VIEW";
    public static final String TUITION_EDIT = "TUITION_EDIT";

    // Quyền quản trị
    public static final String USER_MANAGE = "USER_MANAGE";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String CLASS_MANAGE = "CLASS_MANAGE";
    public static final String LEAD_MANAGE = "LEAD_MANAGE";
    public static final String LEAD_VIEW = "LEAD_VIEW";
    public static final String LEAD_CREATE = "LEAD_CREATE";
    public static final String LEAD_UPDATE = "LEAD_UPDATE";
    public static final String LEAD_DELETE = "LEAD_DELETE";
    public static final String PUBLIC_VIEW = "PUBLIC_VIEW";

    // Ma trận phân quyền theo vai trò
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // 1. Quản trị hệ thống (Admin): Có toàn quyền
        ROLE_PERMISSIONS.put(RoleConstant.ADMIN, new HashSet<>(Arrays.asList(
                USER_MANAGE, ROLE_MANAGE, CLASS_MANAGE, LEAD_MANAGE,
                LEAD_VIEW, LEAD_CREATE, LEAD_UPDATE, LEAD_DELETE,
                GRADE_VIEW, GRADE_EDIT, TUITION_VIEW, TUITION_EDIT, PUBLIC_VIEW
        )));

        // 2. Quản lý đào tạo (TrainingManager): Quản lý lớp, xem và sửa điểm, toàn quyền CRUD lead (gồm cả xóa)
        ROLE_PERMISSIONS.put(RoleConstant.TRAINING_MANAGER, new HashSet<>(Arrays.asList(
                CLASS_MANAGE, GRADE_VIEW, GRADE_EDIT, PUBLIC_VIEW,
                LEAD_VIEW, LEAD_CREATE, LEAD_UPDATE, LEAD_DELETE, LEAD_MANAGE
        )));

        // 3. Tư vấn tuyển sinh (Admissions): Quản lý lead: Create, Read, Update (TUYỆT ĐỐI KHÔNG CÓ LEAD_DELETE)
        ROLE_PERMISSIONS.put(RoleConstant.ADMISSIONS, new HashSet<>(Arrays.asList(
                LEAD_MANAGE, LEAD_VIEW, LEAD_CREATE, LEAD_UPDATE, PUBLIC_VIEW
        )));

        // 4. Giảng viên (Instructor): CÓ QUYỀN SỬA ĐIỂM, TUYỆT ĐỐI KHÔNG CÓ QUYỀN SỬA HỌC PHÍ
        ROLE_PERMISSIONS.put(RoleConstant.INSTRUCTOR, new HashSet<>(Arrays.asList(
                GRADE_VIEW, GRADE_EDIT, PUBLIC_VIEW
        )));

        // 5. Trợ giảng (TeachingAssistant): Chỉ xem điểm và hỗ trợ
        ROLE_PERMISSIONS.put(RoleConstant.TEACHING_ASSISTANT, new HashSet<>(Arrays.asList(
                GRADE_VIEW, PUBLIC_VIEW
        )));

        // 6. Kế toán (Accountant): CÓ QUYỀN SỬA HỌC PHÍ, TUYỆT ĐỐI KHÔNG CÓ QUYỀN SỬA ĐIỂM
        ROLE_PERMISSIONS.put(RoleConstant.ACCOUNTANT, new HashSet<>(Arrays.asList(
                TUITION_VIEW, TUITION_EDIT, GRADE_VIEW, PUBLIC_VIEW
        )));

        // 7. Học viên (Student): Chỉ xem điểm và học phí của bản thân
        ROLE_PERMISSIONS.put(RoleConstant.STUDENT, new HashSet<>(Arrays.asList(
                GRADE_VIEW, TUITION_VIEW, PUBLIC_VIEW
        )));

        // 8. Khách (Guest): Chỉ xem thông tin công khai
        ROLE_PERMISSIONS.put(RoleConstant.GUEST, new HashSet<>(Collections.singletonList(
                PUBLIC_VIEW
        )));
    }

    /**
     * Kiểm tra xem danh sách vai trò có quyền yêu cầu hay không
     */
    public static boolean hasPermission(List<String> userRoles, String requiredPermission) {
        if (userRoles == null || userRoles.isEmpty() || requiredPermission == null) {
            return false;
        }
        for (String role : userRoles) {
            Set<String> perms = ROLE_PERMISSIONS.get(role);
            if (perms != null && perms.contains(requiredPermission)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> getPermissionsByRole(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Collections.emptySet());
    }
}
