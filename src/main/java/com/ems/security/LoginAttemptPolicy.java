package com.ems.security;

import java.time.Duration;
import java.time.Instant;

public final class LoginAttemptPolicy {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private LoginAttemptPolicy() {}

    public static State recordFailure(State current, Instant now) {
        if (current.lockedUntil() != null && current.lockedUntil().isAfter(now)) return current;
        int failures = current.lockedUntil() == null ? current.failures() : 0;
        failures++;
        Instant lockedUntil = failures >= MAX_FAILED_ATTEMPTS ? now.plus(LOCK_DURATION) : null;
        return new State(failures, lockedUntil);
    }

    public static boolean isLocked(State state, Instant now) {
        return state.lockedUntil() != null && state.lockedUntil().isAfter(now);
    }

    public record State(int failures, Instant lockedUntil) {}
}