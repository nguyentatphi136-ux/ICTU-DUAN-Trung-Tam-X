package com.ems.dto;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConflictDetail implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;
    private Long sessionId;
    private String className;
    private String resourceName;
    private String startTime;
    private String endTime;
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
        if (conflictingSchedule != null) {
            Object sId = conflictingSchedule.get("scheduleId");
            if (sId instanceof Number num) {
                this.sessionId = num.longValue();
            }
            Object cName = conflictingSchedule.get("className");
            if (cName != null) {
                this.className = cName.toString();
            } else {
                Object cCode = conflictingSchedule.get("classCode");
                if (cCode != null) this.className = cCode.toString();
            }
            if (ConflictType.INSTRUCTOR_CONFLICT.getCode().equals(type)) {
                Object iName = conflictingSchedule.get("instructorName");
                if (iName != null) this.resourceName = iName.toString();
            } else {
                Object rName = conflictingSchedule.get("roomName");
                if (rName != null) this.resourceName = rName.toString();
                else {
                    Object rCode = conflictingSchedule.get("roomCode");
                    if (rCode != null) this.resourceName = rCode.toString();
                }
            }
            Object sTime = conflictingSchedule.get("startDateTime");
            if (sTime != null) this.startTime = sTime.toString();
            Object eTime = conflictingSchedule.get("endDateTime");
            if (eTime != null) this.endTime = eTime.toString();
        }
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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
