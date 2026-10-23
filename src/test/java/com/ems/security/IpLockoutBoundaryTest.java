package com.ems.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử biên cho chính sách khóa IP đăng nhập
 */
class IpLockoutBoundaryTest {

    @BeforeEach
    void setUp() {
        IpLoginAttemptService.resetAll();
    }

    @Test
    void testNullAndEmptyIpHandling() {
        assertFalse(IpLoginAttemptService.isIpLocked(null));
        assertFalse(IpLoginAttemptService.isIpLocked(""));
        assertFalse(IpLoginAttemptService.isIpLocked("   "));
        assertEquals(0, IpLoginAttemptService.getRemainingLockSeconds(null));
        assertEquals(0, IpLoginAttemptService.getRemainingLockSeconds(""));
        assertEquals(5, IpLoginAttemptService.getRemainingAttempts(null));
    }

    @Test
    void testBoundaryAttemptCounts() {
        String testIp = "10.0.0.99";

        // Thử 4 lần: Vẫn chưa bị khóa
        for (int i = 1; i <= 4; i++) {
            IpLoginAttemptService.recordFailure(testIp);
            assertFalse(IpLoginAttemptService.isIpLocked(testIp));
            assertEquals(5 - i, IpLoginAttemptService.getRemainingAttempts(testIp));
        }

        // Lần thứ 5: Khóa ngay lập tức
        IpLoginAttemptService.recordFailure(testIp);
        assertTrue(IpLoginAttemptService.isIpLocked(testIp));
        assertEquals(0, IpLoginAttemptService.getRemainingAttempts(testIp));
        assertTrue(IpLoginAttemptService.getRemainingLockSeconds(testIp) > 0);
    }
}
