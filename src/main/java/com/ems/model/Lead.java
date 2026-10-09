package com.ems.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Model Entity đại diện cho Khách hàng tiềm năng (Lead)
 * Thuộc Sprint 2 - User Story S2-09 [IDTTX-45]
 */
public class Lead implements Serializable {
    private static final long serialVersionUID = 1L;

    // Trạng thái chuẩn của Lead
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_CONTACTED = "CONTACTED";
    public static final String STATUS_CONSULTING = "CONSULTING";
    public static final String STATUS_TRIAL = "TRIAL";
    public static final String STATUS_WON = "WON";
    public static final String STATUS_LOST = "LOST";
    public static final String STATUS_ENROLLED = "ENROLLED";

    public static final List<String> VALID_STATUSES = Arrays.asList(
            STATUS_NEW, STATUS_CONTACTED, STATUS_CONSULTING, STATUS_TRIAL,
            STATUS_WON, STATUS_LOST, STATUS_ENROLLED, "TRIAL_SCHEDULED", "REJECTED"
    );

    // Nguồn khách hàng tiềm năng
    public static final String SOURCE_WEBSITE = "WEBSITE";
    public static final String SOURCE_FACEBOOK = "FACEBOOK";
    public static final String SOURCE_REFERRAL = "REFERRAL";
    public static final String SOURCE_DIRECT = "DIRECT";
    public static final String SOURCE_ADS = "ADS";
    public static final String SOURCE_EVENT = "EVENT";
    public static final String SOURCE_OTHER = "OTHER";

    public static final List<String> VALID_SOURCES = Arrays.asList(
            SOURCE_WEBSITE, SOURCE_FACEBOOK, SOURCE_REFERRAL, SOURCE_DIRECT,
            SOURCE_ADS, SOURCE_EVENT, SOURCE_OTHER
    );

    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String source;
    private Integer programId;
    private String programInterest;
    private String status;
    private String notes;
    private Long assignedTo;
    private String assignedToName;
    private Long createdBy;
    private String createdByName;
    private Long updatedBy;
    private String updatedByName;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Lead() {
        this.source = SOURCE_WEBSITE;
        this.status = STATUS_NEW;
    }

    public Lead(String fullName, String phone, String email, String programInterest) {
        this();
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.programInterest = programInterest;
    }

    public Lead(Long id, String fullName, String phone, String email, String source,
                Integer programId, String programInterest, String status, String notes,
                Long assignedTo, Long createdBy, Long updatedBy) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.source = (source != null && !source.isBlank()) ? source : SOURCE_WEBSITE;
        this.programId = programId;
        this.programInterest = programInterest;
        this.status = (status != null && !status.isBlank()) ? status : STATUS_NEW;
        this.notes = notes;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
    }

    public static boolean isValidStatus(String status) {
        if (status == null || status.isBlank()) return false;
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(status.trim())) return true;
        }
        return false;
    }

    public static boolean isValidSource(String source) {
        if (source == null || source.isBlank()) return false;
        for (String valid : VALID_SOURCES) {
            if (valid.equalsIgnoreCase(source.trim())) return true;
        }
        return false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public String getProgramInterest() {
        return programInterest;
    }

    public void setProgramInterest(String programInterest) {
        this.programInterest = programInterest;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Lead{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", source='" + source + '\'' +
                ", programInterest='" + programInterest + '\'' +
                ", status='" + status + '\'' +
                ", assignedTo=" + assignedTo +
                '}';
    }
}
