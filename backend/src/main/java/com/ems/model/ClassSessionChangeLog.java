package com.ems.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ClassSessionChangeLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private String actionType; // UPDATE, RESCHEDULE, CANCEL
    private Long performedBy;
    private String performedByName;
    private String reason;
    private String oldValues;
    private String newValues;
    private LocalDateTime createdAt;

    public ClassSessionChangeLog() {}

    public ClassSessionChangeLog(Long sessionId, String actionType, Long performedBy,
                                 String reason, String oldValues, String newValues) {
        this.sessionId = sessionId;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.reason = reason;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByName() {
        return performedByName;
    }

    public void setPerformedByName(String performedByName) {
        this.performedByName = performedByName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getOldValues() {
        return oldValues;
    }

    public void setOldValues(String oldValues) {
        this.oldValues = oldValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
