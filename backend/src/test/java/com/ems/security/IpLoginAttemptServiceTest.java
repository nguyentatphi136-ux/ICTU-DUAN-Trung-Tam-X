package com.ems.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpLoginAttemptServiceTest {

    @BeforeEach
    void setUp() {
        IpLoginAttemptService.resetAll();
    }

    @Test
    void testInitialStateNotLocked() {
        String ip = "192.168.1.100";
        assertFalse(IpLoginAttemptService.isIpLocked(ip));
        assertEquals(5, IpLoginAttemptService.getRemainingAttempts(ip));
        assertEquals(0, IpLoginAttemptService.getRemainingLockSeconds(ip));
    }

    @Test
    void testLockoutAfterFiveFailuresOnSameIp() {
        String attackingIp = "192.168.1.50";

        for (int i = 1; i <= 4; i++) {
            LoginAttemptPolicy.State state = IpLoginAttemptService.recordFailure(attackingIp);
            assertEquals(i, state.failures());
            assertFalse(IpLoginAttemptService.isIpLocked(attackingIp));
            assertEquals(5 - i, IpLoginAttemptService.getRemainingAttempts(attackingIp));
        }

        // 5th attempt locks the attacking IP for 15 minutes
        LoginAttemptPolicy.State fifth = IpLoginAttemptService.recordFailure(attackingIp);
        assertEquals(5, fifth.failures());
        assertTrue(IpLoginAttemptService.isIpLocked(attackingIp));
        assertTrue(IpLoginAttemptService.getRemainingLockSeconds(attackingIp) > 0);
        assertTrue(IpLoginAttemptService.getRemainingLockSeconds(attackingIp) <= 15 * 60);
    }

    @Test
    void testDifferentIpsAreIsolated() {
        String attackingIp = "1.2.3.4";
        String legitimateUserIp = "5.6.7.8";

        // Attacking IP fails 5 times and gets locked
        for (int i = 0; i < 5; i++) {
            IpLoginAttemptService.recordFailure(attackingIp);
        }
        assertTrue(IpLoginAttemptService.isIpLocked(attackingIp), "Attacking IP should be locked");

        // Legitimate user's IP is unaffected and can log in normally!
        assertFalse(IpLoginAttemptService.isIpLocked(legitimateUserIp), "Legitimate IP must NOT be locked");
        assertEquals(5, IpLoginAttemptService.getRemainingAttempts(legitimateUserIp));
    }

    @Test
    void testSuccessfulLoginResetsCounter() {
        String ip = "192.168.1.77";
        IpLoginAttemptService.recordFailure(ip);
        IpLoginAttemptService.recordFailure(ip);
        assertEquals(3, IpLoginAttemptService.getRemainingAttempts(ip));

        // Successful login
        IpLoginAttemptService.recordSuccess(ip);
        assertFalse(IpLoginAttemptService.isIpLocked(ip));
        assertEquals(5, IpLoginAttemptService.getRemainingAttempts(ip));
    }
}
