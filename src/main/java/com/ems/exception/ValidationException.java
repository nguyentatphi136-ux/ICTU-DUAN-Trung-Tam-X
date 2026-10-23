package com.ems.exception;

/**
 * Ngoại lệ dữ liệu đầu vào không hợp lệ -> HTTP 400 Bad Request
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(400, "VALIDATION_ERROR", message);
    }

    public ValidationException(String message, int statusCode) {
        super(statusCode, "VALIDATION_ERROR", message);
    }
}
