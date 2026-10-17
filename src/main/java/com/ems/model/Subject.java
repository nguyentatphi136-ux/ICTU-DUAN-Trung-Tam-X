package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Model Entity đại diện cho Môn học (Subject / Course)
 * Hỗ trợ đồng bộ cả S2-05 và S3-05.
 */
public class Subject implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String code;
    private String name;
    private Integer sessions;
    private BigDecimal weight;
    private BigDecimal minPassScore;
    private String outcome;
    private String status; // ACTIVE, INACTIVE
    private int openedClassesCount;
    private int programsCount;
    private int orderIndex;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Subject() {
        this.sessions = 15;
        this.weight = BigDecimal.ONE;
        this.minPassScore = new BigDecimal("5.00");
        this.status = "ACTIVE";
    }

    public Subject(Integer id, String code, String name) {
        this();
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public Subject(Integer id, String code, String name, Integer sessions, BigDecimal weight, String outcome) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.sessions = sessions != null ? sessions : 15;
        this.weight = weight != null ? weight : BigDecimal.ONE;
        this.minPassScore = new BigDecimal("5.00");
        this.outcome = outcome;
        this.status = "ACTIVE";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    // Alias for S3-05
    public String getSubjectCode() {
        return code;
    }

    public void setSubjectCode(String subjectCode) {
        this.code = subjectCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Alias for S3-05
    public String getSubjectName() {
        return name;
    }

    public void setSubjectName(String subjectName) {
        this.name = subjectName;
    }

    public Integer getSessions() {
        return sessions;
    }

    public void setSessions(Integer sessions) {
        this.sessions = sessions;
    }

    // Alias for S3-05
    public int getTotalSessions() {
        return sessions != null ? sessions : 0;
    }

    public void setTotalSessions(int totalSessions) {
        this.sessions = totalSessions;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    // Alias for S3-05
    public BigDecimal getCreditWeight() {
        return weight;
    }

    public void setCreditWeight(BigDecimal creditWeight) {
        this.weight = creditWeight;
    }

    public BigDecimal getMinPassScore() {
        return minPassScore;
    }

    public void setMinPassScore(BigDecimal minPassScore) {
        this.minPassScore = minPassScore;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    // Alias for S3-05
    public String getLearningOutcomes() {
        return outcome;
    }

    public void setLearningOutcomes(String learningOutcomes) {
        this.outcome = learningOutcomes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOpenedClassesCount() {
        return openedClassesCount;
    }

    public void setOpenedClassesCount(int openedClassesCount) {
        this.openedClassesCount = openedClassesCount;
    }

    public int getProgramsCount() {
        return programsCount;
    }

    public void setProgramsCount(int programsCount) {
        this.programsCount = programsCount;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt != null ? new Timestamp(createdAt.getTime()) : null;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt != null ? new Timestamp(updatedAt.getTime()) : null;
    }
}
