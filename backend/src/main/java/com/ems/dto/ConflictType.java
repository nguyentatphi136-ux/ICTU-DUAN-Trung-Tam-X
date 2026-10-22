package com.ems.dto;

public enum ConflictType {
    INSTRUCTOR_CONFLICT("INSTRUCTOR_CONFLICT", "Trùng lịch giảng viên"),
    ROOM_CONFLICT("ROOM_CONFLICT", "Trùng phòng học"),
    BOTH_CONFLICT("BOTH_CONFLICT", "Trùng cả giảng viên và phòng học");

    private final String code;
    private final String label;

    ConflictType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
