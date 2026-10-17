package com.ems.exception;

/**
 * Ngoại lệ khi không tìm thấy tài nguyên -> HTTP 404 Not Found
 */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(404, "RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String message, int statusCode) {
        super(statusCode, "RESOURCE_NOT_FOUND", message);
    }
}
