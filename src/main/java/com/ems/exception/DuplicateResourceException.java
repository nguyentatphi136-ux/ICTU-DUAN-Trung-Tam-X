package com.ems.exception;

/**
 * Ngoại lệ khi dữ liệu bị trùng lặp (ví dụ: mã lớp, mã phòng đã tồn tại) -> HTTP 409 Conflict
 */
public class DuplicateResourceException extends ApiException {
    private static final long serialVersionUID = 1L;

    public DuplicateResourceException(String message) {
        super(409, "DUPLICATE_RESOURCE", message);
    }

    public DuplicateResourceException(String message, int statusCode) {
        super(statusCode, "DUPLICATE_RESOURCE", message);
    }
}
