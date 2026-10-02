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
            Map.entry("GET /api/admin/menus", "ROLE_PERMISSION_READ")
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