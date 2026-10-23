package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;

public class SchoolClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String classCode;
    private String className;
    private Integer programId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxCapacity = 30;
    private Integer defaultClassroomId;
    private String studyMode = "OFFLINE";
    private String status = "PLANNING";

    public SchoolClass() {}

    public SchoolClass(Long id, String classCode, String className, Integer programId, String status) {
        this.id = id;
        this.classCode = classCode;
        this.className = className;
        this.programId = programId;
        this.status = status;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
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
}
