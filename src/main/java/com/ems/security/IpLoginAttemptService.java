package com.ems.security;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dịch vụ kiểm soát số lần đăng nhập sai và khóa tạm theo ĐỊA CHỈ IP MÁY (Client IP).
 * 
 * Mục tiêu bảo mật:
 * 1. Khóa tạm 15 phút sau 5 lần nhập sai liên tiếp từ CÙNG MỘT ĐỊA CHỈ IP.
 * 2. KHÔNG khóa tài khoản trong cơ sở dữ liệu: Tránh tấn công từ chối dịch vụ tài khoản (Account DoS).
 *    Nếu kẻ tấn công cố tình nhập sai tài khoản admin@tms.vn 5 lần từ máy của họ,
 *    chỉ IP của kẻ tấn công bị chặn 15 phút, tài khoản admin thực sự vẫn đăng nhập bình thường
 *    từ thiết bị hoặc mạng khác.
 */
public final class IpLoginAttemptService {
    public static final int MAX_CACHE_ENTRIES = 10000;
    private static final Map<String, LoginAttemptPolicy.State> IP_ATTEMPTS = new ConcurrentHashMap<>();

    private IpLoginAttemptService() {}

    /**
     * Giới hạn tối đa số lượng IP được lưu vết trong bộ nhớ để bảo vệ RAM
     */
    public static int getMaxCacheEntries() {
        return MAX_CACHE_ENTRIES;
    }

    /**
     * Lấy địa chỉ IP của client gửi yêu cầu (hỗ trợ cả trường hợp qua Proxy / Reverse Proxy)
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip.trim())) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip.trim())) {
            ip = request.getRemoteAddr();
        } else {
            // Trường hợp X-Forwarded-For chứa danh sách: "client, proxy1, proxy2" -> lấy IP client đầu tiên
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        // Chuẩn hóa localhost IPv6 về IPv4 để đồng nhất
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }

    /**
     * Kiểm tra địa chỉ IP có đang trong thời gian bị khóa 15 phút hay không
     */
    public static boolean isIpLocked(String ip) {
        if (ip == null || ip.isBlank()) return false;
        LoginAttemptPolicy.State state = IP_ATTEMPTS.get(ip);
        if (state == null) return false;

        Instant now = Instant.now();
        if (LoginAttemptPolicy.isLocked(state, now)) {
            return true;
        }
        // Nếu đã hết thời gian khóa, dọn dẹp entry
        if (state.lockedUntil() != null && !state.lockedUntil().isAfter(now)) {
            IP_ATTEMPTS.remove(ip);
        }
        return false;
    }

    /**
     * Lấy số giây còn lại cho đến khi IP được mở khóa
     */
    public static long getRemainingLockSeconds(String ip) {
        if (ip == null || ip.isBlank()) return 0;
        LoginAttemptPolicy.State state = IP_ATTEMPTS.get(ip);
        if (state == null || state.lockedUntil() == null) return 0;

        Instant now = Instant.now();
        if (!state.lockedUntil().isAfter(now)) {
            IP_ATTEMPTS.remove(ip);
            return 0;
        }
        return Math.max(0, Duration.between(now, state.lockedUntil()).getSeconds());
    }

    /**
     * Ghi nhận một lần đăng nhập thất bại cho địa chỉ IP.
     * Nếu đạt đủ 5 lần, IP sẽ bị khóa 15 phút.
     */
    public static LoginAttemptPolicy.State recordFailure(String ip) {
        if (ip == null || ip.isBlank()) ip = "unknown";
        Instant now = Instant.now();
        pruneExpiredIfNeeded(now);

        return IP_ATTEMPTS.compute(ip, (key, current) -> {
            if (current == null) {
                return LoginAttemptPolicy.recordFailure(new LoginAttemptPolicy.State(0, null), now);
            }
            return LoginAttemptPolicy.recordFailure(current, now);
        });
    }

    /**
     * Ghi nhận đăng nhập thành công từ IP này: Xóa bộ đếm lỗi của IP
     */
    public static void recordSuccess(String ip) {
        if (ip != null) {
            IP_ATTEMPTS.remove(ip);
        }
    }

    /**
     * Lấy số lần thử còn lại của IP trước khi bị khóa
     */
    public static int getRemainingAttempts(String ip) {
        if (ip == null || ip.isBlank()) return LoginAttemptPolicy.MAX_FAILED_ATTEMPTS;
        LoginAttemptPolicy.State state = IP_ATTEMPTS.get(ip);
        if (state == null) return LoginAttemptPolicy.MAX_FAILED_ATTEMPTS;
        return Math.max(0, LoginAttemptPolicy.MAX_FAILED_ATTEMPTS - state.failures());
    }

    /**
     * Xóa sạch toàn bộ cache IP (phục vụ test hoặc admin mở khóa thủ công)
     */
    public static void resetAll() {
        IP_ATTEMPTS.clear();
    }

    /**
     * Lấy tổng số lượng IP đang được lưu vết trong bộ nhớ (dùng cho giám sát)
     */
    public static int getActiveTrackedIpsCount() {
        return IP_ATTEMPTS.size();
    }

    /**
     * Dọn dẹp các entry IP đã hết hạn khi bộ nhớ đạt ngưỡng
     */
    private static void pruneExpiredIfNeeded(Instant now) {
        if (IP_ATTEMPTS.size() < MAX_CACHE_ENTRIES) return;
        Iterator<Map.Entry<String, LoginAttemptPolicy.State>> it = IP_ATTEMPTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, LoginAttemptPolicy.State> entry = it.next();
            LoginAttemptPolicy.State state = entry.getValue();
            if (state.lockedUntil() != null && !state.lockedUntil().isAfter(now)) {
                it.remove();
            }
        }
    }
}
