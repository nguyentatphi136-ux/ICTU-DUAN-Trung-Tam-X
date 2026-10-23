package com.ems.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Model đại diện cho một bản ghi lịch sử thay đổi tình trạng học vụ (S4-06)
 * Thao tác bao gồm: Bảo lưu, Chuyển lớp, Thôi học
 */
public class StudentStatusLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentCode;
    private String studentName;
    private String actionType; // "Bảo lưu", "Chuyển lớp", "Thôi học"
    private String fromClass;
    private String toClass;
    private String effectiveDate; // yyyy-MM-dd
    private String reason;
    private String notes;
    private String actorName; // Người thực hiện (Quản lý đào tạo)
    private Long actorId;
    private String recordedDate; // Ngày ghi nhận yyyy-MM-dd hoặc dd/MM/yyyy HH:mm
    private Date createdAt;

    public StudentStatusLog() {
    }

    public StudentStatusLog(String actionType, String effectiveDate, String reason, String actorName,
                            String fromClass, String toClass, String recordedDate) {
        this.actionType = actionType;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.actorName = actorName;
        this.fromClass = fromClass;
        this.toClass = toClass;
        this.recordedDate = recordedDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getFromClass() {
        return fromClass;
    }

    public void setFromClass(String fromClass) {
        this.fromClass = fromClass;
    }

    public String getToClass() {
        return toClass;
    }

    public void setToClass(String toClass) {
        this.toClass = toClass;
    }

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getRecordedDate() {
        return recordedDate;
    }

    public void setRecordedDate(String recordedDate) {
        this.recordedDate = recordedDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
