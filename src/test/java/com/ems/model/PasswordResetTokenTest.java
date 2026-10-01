package com.ems.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.junit.jupiter.api.Test;

class PasswordResetTokenTest {
    @Test
    void tokenIsValidUntilItsThirtyMinuteExpirationAndCanOnlyBeMarkedUsed() {
        Date expires = new Date(System.currentTimeMillis() + 30 * 60 * 1000L);
        PasswordResetToken token = new PasswordResetToken(7L, "token-hash", expires);
        assertFalse(token.isUsed());
        assertFalse(token.isExpired());

        token.setUsedAt(new Date());
        assertTrue(token.isUsed());

        PasswordResetToken expired = new PasswordResetToken(8L, "expired-hash", new Date(System.currentTimeMillis() - 1000));
        assertTrue(expired.isExpired());
    }
}