package com.ems.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Model PasswordResetToken - Đại diện cho token đặt lại mật khẩu
 * Khớp với bảng `password_reset_tokens` trong database
 * Phục vụ: IDTTX-37, IDTTX-41, IDTTX-42
 */
public class PasswordResetToken implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String tokenHash;
    private Date expiresAt;
    private Date usedAt;
    private Date createdAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Long userId, String tokenHash, Date expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = new Date();
    }

    public boolean isExpired() {
        return expiresAt != null && new Date().after(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
