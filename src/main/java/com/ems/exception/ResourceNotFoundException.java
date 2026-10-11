package com.ems.exception;

/**
 * Ngoại lệ khi không tìm thấy tài nguyên (ví dụ: chương trình đào tạo không tồn tại) -> HTTP 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {
    private final int statusCode;

    public ResourceNotFoundException(String message) {
        super(message);
        this.statusCode = 404;
    }

    public ResourceNotFoundException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
