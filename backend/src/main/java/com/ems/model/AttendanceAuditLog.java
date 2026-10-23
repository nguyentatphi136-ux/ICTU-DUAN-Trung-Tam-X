package com.ems.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Model nhật ký sửa điểm danh (S4-07 AC4):
 * Ghi lại đầy đủ: buổi học, học viên, ai sửa, vai trò, trạng thái cũ, trạng thái mới, lý do sửa và thời điểm.
 */
public class AttendanceAuditLog implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String oldStatus;
    private String newStatus;
    private String reason;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AttendanceAuditLog() {}

    public AttendanceAuditLog(Long sessionId, Long studentId, String studentCode, String studentName,
                              Long actorId, String actorName, String actorRole,
                              String oldStatus, String newStatus, String reason) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.studentCode = studentCode;
        this.studentName = studentName;
        this.actorId = actorId;
        this.actorName = actorName;
        this.actorRole = actorRole;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
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

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }

    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedCreatedAt() {
        return createdAt != null ? createdAt.format(ISO_FORMATTER) : null;
    }

    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        if (studentName != null) {
            sb.append(studentName).append(": ");
        }
        sb.append(oldStatus != null ? oldStatus : "chưa có").append(" → ").append(newStatus);
        if (reason != null && !reason.trim().isEmpty()) {
            sb.append(". Lý do: ").append(reason.trim());
        }
        return sb.toString();
    }
}
