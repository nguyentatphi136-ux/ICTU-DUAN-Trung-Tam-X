package com.ems.service;

import com.ems.constant.RoleConstant;
import com.ems.model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kho lưu trữ và quản lý người dùng & vai trò (In-Memory Data Store & Fallback)
 * Tác vụ Jira: IDTTX-24 [BE] Gán và thu hồi vai trò của một người dùng
 * Đặc tả:
 *   1. Một người dùng có thể giữ nhiều vai trò cùng lúc (vd: vừa là Giảng viên vừa là Quản lý đào tạo).
 *   2. Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại.
 *   3. Không thể tự thu hồi vai trò quản trị (Admin) của chính mình.
 *   4. Cung cấp tài khoản demo dự phòng giúp hệ thống luôn hoạt động ổn định ngay cả khi chưa khởi động MySQL.
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class UserStore {

    private static final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private static final Map<String, Long> idByEmail = new ConcurrentHashMap<>();
    private static final Map<String, String> demoPasswords = new ConcurrentHashMap<>();

    static {
        resetToDefault();
    }

    public static synchronized void resetToDefault() {
        usersById.clear();
        idByEmail.clear();
        demoPasswords.clear();

        // 1. Quản trị hệ thống
        initUser(new User(1L, "Quản trị viên Hệ thống", "admin@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.ADMIN))), "Admin@123");
        idByEmail.put("admin@example.com", 1L);
        idByEmail.put("admin", 1L);

        // 2. Giảng viên
        initUser(new User(2L, "Giảng viên Nguyễn Văn An", "giangvien@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.INSTRUCTOR))), "Giangvien@123");
        idByEmail.put("instructor@example.com", 2L);
        idByEmail.put("giangvien", 2L);

        // 3. Học viên
        initUser(new User(3L, "Học viên Trần Thị Bình", "hocvien@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.STUDENT))), "Hocvien@123");
        idByEmail.put("student@example.com", 3L);
        idByEmail.put("hocvien", 3L);

        // 4. Kế toán
        initUser(new User(4L, "Kế toán Lê Thị Cúc", "ketoan@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.ACCOUNTANT))), "Ketoan@123");
        idByEmail.put("accountant@example.com", 4L);
        idByEmail.put("ketoan", 4L);

        // 5. Quản lý đào tạo
        initUser(new User(5L, "Quản lý đào tạo Hoàng Dũng", "daotao@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.TRAINING_MANAGER))), "Daotao@123");
        idByEmail.put("training@example.com", 5L);
        idByEmail.put("daotao", 5L);
        idByEmail.put("quanlydaotao@edumanager.vn", 5L);

        // 6. Tư vấn tuyển sinh
        initUser(new User(6L, "Tư vấn tuyển sinh Phạm Mai", "tuvan@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.ADMISSIONS))), "Tuyensinh@123");
        idByEmail.put("admissions@example.com", 6L);
        idByEmail.put("tuvan", 6L);

        // 7. Trợ giảng
        initUser(new User(7L, "Trợ giảng Đỗ Tuấn", "trogiang@edumanager.vn", "ACTIVE",
                new ArrayList<>(Collections.singletonList(RoleConstant.TEACHING_ASSISTANT))), "Trogiang@123");
        idByEmail.put("ta@example.com", 7L);
        idByEmail.put("trogiang", 7L);
    }

    private static void initUser(User user, String defaultPassword) {
        usersById.put(user.getId(), user);
        String emailLower = user.getEmail().toLowerCase(Locale.ROOT);
        idByEmail.put(emailLower, user.getId());
        demoPasswords.put(emailLower, defaultPassword);
    }

    public static List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    public static User findById(long id) {
        return usersById.get(id);
    }

    public static User findByEmail(String email) {
        if (email == null) return null;
        Long id = idByEmail.get(email.trim().toLowerCase(Locale.ROOT));
        return id != null ? usersById.get(id) : null;
    }

    /**
     * Xác thực thông tin đăng nhập demo khi DB MySQL không khả dụng
     */
    public static User authenticate(String email, String password) {
        if (email == null || password == null || password.trim().isEmpty()) {
            return null;
        }
        User user = findByEmail(email);
        if (user == null) {
            return null;
        }
        // Kiểm tra tài khoản có bị khoá hay không
        if ("LOCKED".equalsIgnoreCase(user.getStatus()) || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            return null;
        }

        String userEmail = user.getEmail().toLowerCase(Locale.ROOT);
        String expectedPass = demoPasswords.get(userEmail);

        // Chấp nhận mật khẩu mặc định của role hoặc mật khẩu chung 123456 / admin123
        if (password.equals(expectedPass)
                || "123456".equals(password)
                || "admin123".equals(password)
                || (expectedPass != null && password.equalsIgnoreCase(expectedPass.replace("@", "")))) {
            return user;
        }

        return null;
    }

    /**
     * Gán vai trò cho người dùng (IDTTX-24)
     * Cho phép một người dùng giữ nhiều vai trò cùng lúc.
     * Có hiệu lực ngay lập tức.
     */
    public static synchronized void assignRole(long adminUserId, long targetUserId, String role) {
        if (!RoleConstant.isValidRole(role)) {
            throw new IllegalArgumentException("Mã vai trò '" + role + "' không hợp lệ trong hệ thống.");
        }

        User targetUser = usersById.get(targetUserId);
        if (targetUser == null) {
            throw new NoSuchElementException("Không tìm thấy người dùng có ID: " + targetUserId);
        }

        List<String> roles = targetUser.getRoles();
        if (roles == null) {
            roles = new ArrayList<>();
            targetUser.setRoles(roles);
        }

        if (roles.contains(role)) {
            throw new IllegalStateException("ROLE_ALREADY_ASSIGNED: Người dùng đã sở hữu vai trò " + role);
        }

        roles.add(role);
    }

    /**
     * Thu hồi vai trò của người dùng (IDTTX-24)
     * Ràng buộc nghiệp vụ: Không thể tự thu hồi vai trò quản trị (Admin) của chính mình.
     * Có hiệu lực ngay lập tức.
     */
    public static synchronized void revokeRole(long adminUserId, long targetUserId, String role) {
        if (!RoleConstant.isValidRole(role)) {
            throw new IllegalArgumentException("Mã vai trò '" + role + "' không hợp lệ trong hệ thống.");
        }

        User targetUser = usersById.get(targetUserId);
        if (targetUser == null) {
            throw new NoSuchElementException("Không tìm thấy người dùng có ID: " + targetUserId);
        }

        // Kiểm tra ràng buộc bảo vệ: Admin không thể tự tước quyền Admin của chính mình
        if (adminUserId == targetUserId && RoleConstant.ADMIN.equalsIgnoreCase(role)) {
            throw new SecurityException("CANNOT_REVOKE_OWN_ADMIN: Quản trị viên không thể tự thu hồi vai trò quản trị của chính mình.");
        }

        List<String> roles = targetUser.getRoles();
        if (roles == null || !roles.contains(role)) {
            throw new IllegalStateException("ROLE_NOT_ASSIGNED: Người dùng hiện không nắm giữ vai trò " + role);
        }

        roles.remove(role);
    }
}
