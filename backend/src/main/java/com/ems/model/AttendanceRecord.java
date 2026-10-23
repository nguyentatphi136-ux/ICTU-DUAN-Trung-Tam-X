package com.ems.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model bản ghi điểm danh của học viên trong một buổi học (S4-07)
 */
public class AttendanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private AttendanceStatus status = AttendanceStatus.PRESENT;
    private String notes;
    private Long markedBy;
    private String markerName;
    private LocalDateTime markedAt;
    private Long updatedBy;
    private String updaterName;
    private LocalDateTime updatedAt;

    public AttendanceRecord() {}

    public AttendanceRecord(Long sessionId, Long studentId, String studentCode, String studentName, AttendanceStatus status) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.status = status != null ? status : AttendanceStatus.PRESENT;
        this.markedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public String getStatusLabel() {
        return status != null ? status.getLabel() : AttendanceStatus.PRESENT.getLabel();
    }

    public String getStatusCode() {
        return status != null ? status.getCode() : AttendanceStatus.PRESENT.getCode();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getMarkedBy() { return markedBy; }
    public void setMarkedBy(Long markedBy) { this.markedBy = markedBy; }

    public String getMarkerName() { return markerName; }
    public void setMarkerName(String markerName) { this.markerName = markerName; }

    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdaterName() { return updaterName; }
    public void setUpdaterName(String updaterName) { this.updaterName = updaterName; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
