package com.ems.exception;

/**
 * Ngoại lệ 400 Bad Request khi dữ liệu đầu vào không hợp lệ
 */
public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(400, "VALIDATION_ERROR", message);
    }
}
