package com.ems.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConflictDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Map<String, Object> requestedSchedule = new LinkedHashMap<>();
    private Map<String, Object> conflictingSchedule = new LinkedHashMap<>();
    private String message;

    public ConflictDetail() {}

    public ConflictDetail(String type, Map<String, Object> requestedSchedule,
                          Map<String, Object> conflictingSchedule, String message) {
        this.type = type;
        this.requestedSchedule = requestedSchedule;
        this.conflictingSchedule = conflictingSchedule;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getRequestedSchedule() {
        return requestedSchedule;
    }

    public void setRequestedSchedule(Map<String, Object> requestedSchedule) {
        this.requestedSchedule = requestedSchedule;
    }

    public Map<String, Object> getConflictingSchedule() {
        return conflictingSchedule;
    }

    public void setConflictingSchedule(Map<String, Object> conflictingSchedule) {
        this.conflictingSchedule = conflictingSchedule;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
