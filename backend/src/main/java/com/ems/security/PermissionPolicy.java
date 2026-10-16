package com.ems.security;

import java.util.Collection;
import java.util.Map;

public final class PermissionPolicy {
    private static final Map<String, String> API_PERMISSIONS = Map.ofEntries(
            Map.entry("GET /api/me/menu", "MENU_VIEW"),
            Map.entry("GET /api/admin/users", "USER_READ"),
            Map.entry("POST /api/admin/users", "USER_CREATE"),
            Map.entry("GET /api/admin/roles", "ROLE_PERMISSION_READ"),
            Map.entry("GET /api/admin/permissions", "ROLE_PERMISSION_READ"),
            Map.entry("GET /api/admin/menus", "ROLE_PERMISSION_READ"),
            Map.entry("GET /api/admin/users/trash", "USER_READ"),
            Map.entry("GET /api/admin/users/import/template", "USER_CREATE"),
            Map.entry("POST /api/admin/users/import/preview", "USER_CREATE"),
            Map.entry("POST /api/admin/users/import", "USER_CREATE")
    );

    private PermissionPolicy() {}

    public static String requiredPermission(String method, String path) {
        String exact = API_PERMISSIONS.get(method + " " + path);
        if (exact != null) return exact;
        if ("PUT".equals(method) && path.matches("^/api/admin/roles/[A-Z_]+/permissions$")) {
            return "ROLE_PERMISSION_UPDATE";
        }
        if (("POST".equals(method) || "PUT".equals(method)) && path.matches("^/api/admin/users/\\d+/roles$")) {
            return "USER_ROLE_ASSIGN";
        }
        if ("DELETE".equals(method) && path.matches("^/api/admin/users/\\d+/roles/[A-Za-z0-9_]+$")) {
            return "USER_ROLE_ASSIGN";
        }
        if ("PUT".equals(method) && path.matches("^/api/admin/users/\\d+/status$")) {
            return "USER_ROLE_ASSIGN";
        }
        if ("PUT".equals(method) && path.matches("^/api/admin/users/\\d+$")) {
            return "USER_ROLE_ASSIGN";
        }
        // Thùng rác tài khoản: chuyển vào, khôi phục, xoá vĩnh viễn (xoá vĩnh viễn còn kiểm vai trò ADMIN trong servlet).
        if ("DELETE".equals(method) && path.matches("^/api/admin/users/\\d+(/purge)?$")) {
            return "USER_ROLE_ASSIGN";
        }
        if ("POST".equals(method) && path.matches("^/api/admin/users/\\d+/restore$")) {
            return "USER_ROLE_ASSIGN";
        }
        // Hồ sơ cá nhân: ai đăng nhập cũng xem và sửa được hồ sơ của chính mình.
        if (("GET".equals(method) || "PUT".equals(method) || "POST".equals(method))
                && ("/api/profile".equals(path) || "/api/user/profile".equals(path))) {
            return "@authenticated";
        }
        // Nhận xét mentor S2: chỉ Quản trị xoá vĩnh viễn. Quản trị không có quyền module (sheet 4 chỉ giao tài khoản)
        // nên dùng TRASH_PURGE; xem thùng rác cần quyền module hoặc TRASH_PURGE ("A|B" là một trong hai).
        if ("DELETE".equals(method) && path.matches("^/api/(training-programs|subjects|leads)/\\d+/purge$")) {
            return "TRASH_PURGE";
        }
        if ("GET".equals(method) && path.matches("^/api/(training-programs|subjects)/trash$")) {
            return "PROGRAM_MANAGE|TRASH_PURGE";
        }
        if ("GET".equals(method) && "/api/leads/trash".equals(path)) {
            return "LEAD_ASSIGN|TRASH_PURGE";
        }
        // Sheet "2. User Roles": nhiều vai trò được xem (R) chương trình, môn học; chỉ Quản lý đào tạo được sửa.
        // GET dùng PROGRAM_READ, riêng thùng rác và mọi thao tác ghi vẫn cần PROGRAM_MANAGE.
        if (path.startsWith("/api/training-programs") || "/api/subjects".equals(path) || path.startsWith("/api/subjects/")) {
            return "GET".equals(method) && !path.endsWith("/trash") ? "PROGRAM_READ" : "PROGRAM_MANAGE";
        }
        // S2-10: phân công, xem tư vấn viên, xoá vào thùng rác và khôi phục lead dành cho người điều phối (Quản lý đào tạo).
        if (path.matches("^/api/leads/(assign|counselors)$") || path.matches("^/api/leads/\\d+/restore$")
                || "DELETE".equals(method) && path.matches("^/api/leads/\\d+$")) {
            return "LEAD_ASSIGN";
        }
        // Sheet 2: Tư vấn toàn quyền lead (LEAD_MANAGE); Kế toán, Quản lý đào tạo chỉ xem (LEAD_READ).
        if ("/api/leads".equals(path) || path.startsWith("/api/leads/")) {
            return "GET".equals(method) ? "LEAD_MANAGE|LEAD_READ" : "LEAD_MANAGE";
        }
        // S3-08: giảng viên, trợ giảng xem lớp của chính mình; danh sách đã lọc theo người đăng nhập.
        if ("GET".equals(method) && "/api/classes/mine".equals(path)) {
            return "@authenticated";
        }
        // Sheet 2 "Lớp học & thời khoá biểu": trợ giảng, giảng viên, tư vấn xem (R); sửa vẫn cần CLASS_MANAGE.
        if ("GET".equals(method) && path.matches("^/api/(classes|rooms)(/.*)?$")) {
            return "CLASS_READ";
        }
        // S3-04: xem danh sách, hồ sơ học viên (giảng viên, trợ giảng, tư vấn, kế toán...); sửa vẫn cần CLASS_MANAGE.
        if ("GET".equals(method) && path.matches("^/api/students(/[^/]+)?$")) {
            return "STUDENT_READ";
        }
        // S3-04 đến S3-09: hồ sơ học viên, lớp, ghi danh, phân công, phòng học dành cho Quản lý đào tạo.
        if (path.matches("^/api/(classes|students|rooms)(/.*)?$")) {
            return "CLASS_MANAGE";
        }
        if ("POST".equals(method) && ("/api/auth/logout".equals(path) || "/api/auth/change-password".equals(path))) {
            return "@authenticated";
        }
        return null;
    }

    public static boolean allows(Collection<String> permissions, String required) {
        return permissions != null && required != null && permissions.contains(required);
    }

    public static boolean mayReplaceRoles(
            long actorId,
            long targetId,
            Collection<String> currentRoles,
            Collection<String> requestedRoles
    ) {
        return actorId != targetId
                || currentRoles == null
                || !currentRoles.contains("ADMIN")
                || requestedRoles != null && requestedRoles.contains("ADMIN");
    }
}