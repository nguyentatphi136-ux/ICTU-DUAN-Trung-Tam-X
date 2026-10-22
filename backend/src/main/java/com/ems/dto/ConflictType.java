package com.ems.dto;

public enum ConflictType {
    INSTRUCTOR_CONFLICT("INSTRUCTOR_CONFLICT", "Tr├╣ng lß╗ïch giß║úng vi├¬n"),
    ROOM_CONFLICT("ROOM_CONFLICT", "Tr├╣ng ph├▓ng hß╗ìc"),
    BOTH_CONFLICT("BOTH_CONFLICT", "Tr├╣ng cß║ú giß║úng vi├¬n v├á ph├▓ng hß╗ìc");

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
