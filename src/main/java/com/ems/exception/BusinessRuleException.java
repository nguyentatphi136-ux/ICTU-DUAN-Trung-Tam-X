package com.ems.exception;

/**
 * Ngoại lệ vi phạm quy tắc nghiệp vụ (ví dụ: chương trình chưa có môn học) -> HTTP 400 Bad Request
 */
public class BusinessRuleException extends RuntimeException {
    private final int statusCode;

    public BusinessRuleException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public BusinessRuleException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
