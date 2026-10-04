package com.ems.exception;

/**
 * Ngoại lệ 409 Conflict khi xảy ra xung đột dữ liệu hoặc vi phạm ràng buộc nghiệp vụ
 */
public class ConflictException extends ApiException {
    public ConflictException(String errorCode, String message) {
        super(409, errorCode, message);
    }

    public ConflictException(String message) {
        super(409, "DATA_CONFLICT", message);
    }
}
