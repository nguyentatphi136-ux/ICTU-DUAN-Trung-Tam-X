package com.ems.model;

import java.io.Serializable;

/**
 * Entity đại diện bảng class_subjects (Môn học thuộc lớp)
 */
public class ClassSubject implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private Long id;
    private Long classId;
    private Integer subjectId;
    private String subjectCode; // Join từ bảng subjects
    private String subjectName; // Join từ bảng subjects
    private int orderIndex;
    private Long primaryInstructorId;
    private String primaryInstructorName; // Join từ bảng users
    private String status;

    public ClassSubject() {
        this.status = STATUS_NOT_STARTED;
        this.orderIndex = 1;
    }

    public ClassSubject(Long classId, Integer subjectId, int orderIndex) {
        this.classId = classId;
        this.subjectId = subjectId;
        this.orderIndex = orderIndex;
        this.status = STATUS_NOT_STARTED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Long getPrimaryInstructorId() {
        return primaryInstructorId;
    }

    public void setPrimaryInstructorId(Long primaryInstructorId) {
        this.primaryInstructorId = primaryInstructorId;
    }

    public String getPrimaryInstructorName() {
        return primaryInstructorName;
    }

    public void setPrimaryInstructorName(String primaryInstructorName) {
        this.primaryInstructorName = primaryInstructorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
