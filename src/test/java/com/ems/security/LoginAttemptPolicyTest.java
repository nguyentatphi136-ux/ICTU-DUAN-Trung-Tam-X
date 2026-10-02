package com.ems.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginAttemptPolicyTest {
    private static final Instant NOW = Instant.parse("2026-10-02T10:00:00Z");

    @Test
    void locksForFifteenMinutesOnTheFifthConsecutiveFailure() {
        LoginAttemptPolicy.State state = new LoginAttemptPolicy.State(0, null);
        for (int attempt = 1; attempt < 5; attempt++) {
            state = LoginAttemptPolicy.recordFailure(state, NOW);
            assertEquals(attempt, state.failures());
            assertFalse(LoginAttemptPolicy.isLocked(state, NOW));
        }

        state = LoginAttemptPolicy.recordFailure(state, NOW);
        assertEquals(5, state.failures());
        assertEquals(NOW.plusSeconds(15 * 60), state.lockedUntil());
        assertTrue(LoginAttemptPolicy.isLocked(state, NOW));
        assertFalse(LoginAttemptPolicy.isLocked(state, state.lockedUntil()));
    }

    @Test
    void failuresDuringLockDoNotExtendItAndExpiredLockStartsOver() {
        LoginAttemptPolicy.State locked = new LoginAttemptPolicy.State(5, NOW.plusSeconds(15 * 60));
        assertEquals(locked, LoginAttemptPolicy.recordFailure(locked, NOW.plusSeconds(60)));
        assertEquals(new LoginAttemptPolicy.State(1, null),
                LoginAttemptPolicy.recordFailure(locked, locked.lockedUntil()));
    }
}