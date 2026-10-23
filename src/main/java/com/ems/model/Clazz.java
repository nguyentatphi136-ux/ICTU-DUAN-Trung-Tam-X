package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Entity đại diện bảng classes (Lớp học)
 * Tên lớp là Clazz để tránh trùng từ khóa reserved `java.lang.Class` của Java
 */
public class Clazz implements Serializable {
    private static final long serialVersionUID = 1L;

    // Trạng thái vòng đời lớp học theo User Story S3-05
    public static final String STATUS_PLANNING = "PLANNING";         // Chuẩn bị
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";   // Đang học
    public static final String STATUS_FINISHED = "FINISHED";         // Kết thúc
    public static final String STATUS_CANCELLED = "CANCELLED";       // Hủy

    // Hình thức học
    public static final String MODE_OFFLINE = "OFFLINE";
    public static final String MODE_ONLINE = "ONLINE";
    public static final String MODE_HYBRID = "HYBRID";

    private Long id;
    private String classCode;
    private String className;
    private Integer programId;
    private String programName; // Dữ liệu join
    private String startDate;   // ExpectedStartDate (YYYY-MM-DD)
    private String endDate;
    private int maxCapacity;    // MaxStudents
    private Integer defaultClassroomId;
    private String studyMode;   // OFFLINE, ONLINE, HYBRID
    private String status;      // PLANNING, IN_PROGRESS, FINISHED, CANCELLED
    private Date createdAt;
    private Date updatedAt;
    private List<ClassSubject> subjects = new ArrayList<>();

    public Clazz() {
        this.status = STATUS_PLANNING;
        this.studyMode = MODE_OFFLINE;
    }

    public static String getStatusLabel(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case STATUS_PLANNING -> "Chuẩn bị";
            case STATUS_IN_PROGRESS -> "Đang học";
            case STATUS_FINISHED -> "Kết thúc";
            case STATUS_CANCELLED -> "Hủy";
            default -> status;
        };
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getDefaultClassroomId() {
        return defaultClassroomId;
    }

    public void setDefaultClassroomId(Integer defaultClassroomId) {
        this.defaultClassroomId = defaultClassroomId;
    }

    public String getStudyMode() {
        return studyMode;
    }

    public void setStudyMode(String studyMode) {
        this.studyMode = studyMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<ClassSubject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<ClassSubject> subjects) {
        this.subjects = subjects;
    }
}
