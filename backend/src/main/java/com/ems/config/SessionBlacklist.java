package com.ems.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mục 11 & Task S1-02 (IDTTX-55, IDTTX-35) & S1-04: Danh sách đen Session
 */
public class SessionBlacklist {
    private static final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    private static final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();
    private static final long DEFAULT_TTL_MILLIS = 8L * 60 * 60 * 1000;

    public static void add(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            blacklist.put(sessionId, System.currentTimeMillis() + DEFAULT_TTL_MILLIS);
            cleanExpiredSessions();
        }
    }

    public static boolean isBlacklisted(String sessionId) {
        if (sessionId == null) return false;
        Long expireAt = blacklist.get(sessionId);
        if (expireAt == null) return false;
        if (System.currentTimeMillis() > expireAt) {
            blacklist.remove(sessionId);
            return false;
        }
        return true;
    }

    public static void remove(String sessionId) {
        if (sessionId != null) {
            blacklist.remove(sessionId);
        }
    }

    public static void registerSession(long userId, String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    public static int revokeOtherSessions(long userId, String currentSessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        int revokedCount = 0;
        for (String sId : sessions) {
            if (currentSessionId == null || !currentSessionId.equals(sId)) {
                add(sId);
                sessions.remove(sId);
                revokedCount++;
            }
        }
        return revokedCount;
    }

    private static void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> now > entry.getValue());
    }

    public static int size() {
        cleanExpiredSessions();
        return blacklist.size();
    }
}
