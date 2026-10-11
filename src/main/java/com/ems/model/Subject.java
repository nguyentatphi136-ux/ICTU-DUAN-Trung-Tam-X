package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity đại diện bảng subjects (Môn học)
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String subjectCode;
    private String subjectName;
    private int totalSessions;
    private BigDecimal creditWeight;
    private BigDecimal minPassScore;
    private String learningOutcomes;
    private String status;
    private int orderIndex; // Dùng khi join với program_subjects hoặc class_subjects
    private Date createdAt;
    private Date updatedAt;

    public Subject() {
    }

    public Subject(Integer id, String subjectCode, String subjectName) {
        this.id = id;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.status = "ACTIVE";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public BigDecimal getCreditWeight() {
        return creditWeight;
    }

    public void setCreditWeight(BigDecimal creditWeight) {
        this.creditWeight = creditWeight;
    }

    public BigDecimal getMinPassScore() {
        return minPassScore;
    }

    public void setMinPassScore(BigDecimal minPassScore) {
        this.minPassScore = minPassScore;
    }

    public String getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(String learningOutcomes) {
        this.learningOutcomes = learningOutcomes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
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
}
