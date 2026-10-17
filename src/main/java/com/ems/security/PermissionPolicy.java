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
            Map.entry("GET /api/leads", "LEAD_VIEW"),
            Map.entry("POST /api/leads", "LEAD_CREATE"),
            Map.entry("GET /api/leads/check-phone", "LEAD_VIEW"),
            Map.entry("POST /api/classes", "CLASS_MANAGE"),
            Map.entry("GET /api/classes", "CLASS_MANAGE"),
            Map.entry("GET /api/programs", "PROGRAM_MANAGE"),
            Map.entry("POST /api/programs", "PROGRAM_MANAGE"),
            Map.entry("GET /api/rooms", "ROOM_VIEW"),
            Map.entry("POST /api/rooms", "ROOM_MANAGE")
    );

    private PermissionPolicy() {}

    public static String requiredPermission(String method, String path) {
        String exact = API_PERMISSIONS.get(method + " " + path);
        if (exact != null) return exact;
        if (path.matches("^/api/classes(/\\d+)?$")) {
            if ("POST".equals(method) || "GET".equals(method)) return "CLASS_MANAGE";
        }
        if (path.matches("^/api/programs(/.*)?$")) {
            return "PROGRAM_MANAGE";
        }
        if ("GET".equals(method) && path.matches("^/api/rooms/\\d+$")) {
            return "ROOM_VIEW";
        }
        if ("GET".equals(method) && path.matches("^/api/rooms/\\d+/validate-capacity$")) {
            return "ROOM_VIEW";
        }
        if ("POST".equals(method) && path.matches("^/api/rooms/\\d+/validate-capacity$")) {
            return "ROOM_VIEW";
        }
        if ("PUT".equals(method) && path.matches("^/api/rooms/\\d+$")) {
            return "ROOM_MANAGE";
        }
        if ("DELETE".equals(method) && path.matches("^/api/rooms/\\d+$")) {
            return "ROOM_MANAGE";
        }
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
        if (path.startsWith("/api/training-programs") || path.startsWith("/api/subjects") || path.matches("^/api/programs/\\d+/subjects.*")) {
            return "PROGRAM_MANAGE";
        }
        if ("GET".equals(method) && path.matches("^/api/leads/\\d+$")) {
            return "LEAD_VIEW";
        }
        if ("PUT".equals(method) && path.matches("^/api/leads/\\d+$")) {
            return "LEAD_UPDATE";
        }
        if ("DELETE".equals(method) && path.matches("^/api/leads/\\d+$")) {
            return "LEAD_DELETE";
        }
        if ("POST".equals(method) && ("/api/auth/logout".equals(path) || "/api/auth/change-password".equals(path))) {
            return "@authenticated";
        }
        return null;
    }

    public static boolean allows(Collection<String> permissions, String required) {
        if (permissions == null || required == null) return false;
        if (permissions.contains(required)) return true;
        if (("ROOM_VIEW".equals(required) || "ROOM_MANAGE".equals(required))
                && (permissions.contains("CLASS_MANAGE") || permissions.contains("ADMIN"))) {
            return true;
        }
        return false;
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