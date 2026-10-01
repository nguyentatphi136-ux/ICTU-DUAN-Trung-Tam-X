package com.ems.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý danh sách đen Session/Token (Session Blacklist)
 * Phục vụ: IDTTX-33, IDTTX-35, IDTTX-55
 * 
 * Mục đích: 
 * Khi người dùng thực hiện Logout (hoặc đổi mật khẩu, bị khóa tài khoản),
 * sessionId sẽ được lập tức đưa vào Blacklist để vô hiệu hóa ngay lập tức ở phía server.
 * Dù client có cố tình giữ lại cookie JSESSIONID thì Filter cũng chặn đứng truy cập.
 */
public class SessionBlacklist {

    // Map lưu sessionId -> thời điểm hết hạn (Timestamp millis)
    // Dùng ConcurrentHashMap đảm bảo Thread-safe hiệu năng cao
    private static final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    // Thời gian lưu trữ trong Blacklist mặc định: 8 giờ (bằng thời gian sống tối đa của phiên)
    private static final long DEFAULT_TTL_MILLIS = 8L * 60 * 60 * 1000;

    /**
     * Thêm sessionId vào danh sách đen khi Logout (IDTTX-35, IDTTX-55)
     * @param sessionId Mã phiên cần thu hồi
     */
    public static void add(String sessionId) {
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            long expireAt = System.currentTimeMillis() + DEFAULT_TTL_MILLIS;
            blacklist.put(sessionId, expireAt);
            cleanExpiredSessions(); // Dọn dẹp các session đã quá hạn tự nhiên
        }
    }

    /**
     * Kiểm tra xem sessionId có nằm trong danh sách đen hay không (IDTTX-55)
     * @param sessionId Mã phiên cần kiểm tra
     * @return true nếu phiên đã bị thu hồi
     */
    public static boolean isBlacklisted(String sessionId) {
        if (sessionId == null) return false;
        Long expireAt = blacklist.get(sessionId);
        if (expireAt == null) {
            return false;
        }
        // Nếu đã quá hạn lưu trữ thì tự động gỡ bỏ
        if (System.currentTimeMillis() > expireAt) {
            blacklist.remove(sessionId);
            return false;
        }
        return true;
    }

    /**
     * Xóa session khỏi danh sách đen thủ công (nếu cần)
     */
    public static void remove(String sessionId) {
        if (sessionId != null) {
            blacklist.remove(sessionId);
        }
    }

    /**
     * Dọn dẹp định kỳ để tránh tràn bộ nhớ RAM (Memory Leak)
     */
    private static void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> now > entry.getValue());
    }

    /**
     * Lấy kích thước hiện tại của danh sách đen (phục vụ giám sát, debug)
     */
    public static int size() {
        cleanExpiredSessions();
        return blacklist.size();
    }
}