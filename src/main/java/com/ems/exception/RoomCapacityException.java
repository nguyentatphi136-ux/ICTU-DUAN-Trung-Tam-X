package com.ems.exception;

import com.ems.dto.RoomCapacityValidationResult;

/**
 * Ngoại lệ khi sĩ số lớp học vượt quá sức chứa của phòng
 */
public class RoomCapacityException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final RoomCapacityValidationResult validationResult;

    public RoomCapacityException(String message, RoomCapacityValidationResult validationResult) {
        super(message);
        this.code = validationResult != null && validationResult.getCode() != null
                ? validationResult.getCode() : "ROOM_CAPACITY_EXCEEDED";
        this.validationResult = validationResult;
    }

    public RoomCapacityException(String code, String message, RoomCapacityValidationResult validationResult) {
        super(message);
        this.code = code;
        this.validationResult = validationResult;
    }

    public String getCode() {
        return code;
    }

    public RoomCapacityValidationResult getValidationResult() {
        return validationResult;
    }
}
