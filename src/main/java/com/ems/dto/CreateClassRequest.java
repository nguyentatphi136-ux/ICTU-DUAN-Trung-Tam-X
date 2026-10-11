package com.ems.dto;

import java.io.Serializable;

/**
 * DTO nhận dữ liệu yêu cầu mở lớp học từ Client (User Story S3-05)
 */
public class CreateClassRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classCode;
    private String className;
    private Integer programId;
    private String expectedStartDate; // Format: YYYY-MM-DD
    private Integer maxStudents;
    private String studyMode;          // OFFLINE, ONLINE, HYBRID

    public CreateClassRequest() {
    }

    public CreateClassRequest(String classCode, Integer programId, String expectedStartDate, Integer maxStudents, String studyMode) {
        this.classCode = classCode;
        this.programId = programId;
        this.expectedStartDate = expectedStartDate;
        this.maxStudents = maxStudents;
        this.studyMode = studyMode;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Integer getProgramId() {
        return programId;
    }

    public void setProgramId(Integer programId) {
        this.programId = programId;
    }

    public String getExpectedStartDate() {
        return expectedStartDate;
    }

    public void setExpectedStartDate(String expectedStartDate) {
        this.expectedStartDate = expectedStartDate;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public String getStudyMode() {
        return studyMode;
    }

    public void setStudyMode(String studyMode) {
        this.studyMode = studyMode;
    }
}
