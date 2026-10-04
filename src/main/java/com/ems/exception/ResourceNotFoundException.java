package com.ems.exception;

/**
 * Ngoại lệ 404 Not Found khi không tìm thấy tài nguyên
 */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(404, "RESOURCE_NOT_FOUND", message);
    }
}
