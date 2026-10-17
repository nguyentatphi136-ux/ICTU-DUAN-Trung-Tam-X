package com.ems.exception;

/**
 * Ngoại lệ khi dữ liệu bị trùng lặp (ví dụ: mã lớp đã tồn tại) -> HTTP 409 Conflict
 */
public class DuplicateResourceException extends RuntimeException {
    private final int statusCode;

    public DuplicateResourceException(String message) {
        super(message);
        this.statusCode = 409;
    }

    public DuplicateResourceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
