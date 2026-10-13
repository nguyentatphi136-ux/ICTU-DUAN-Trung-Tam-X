package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model Entity đại diện cho Môn học (Subject / Course)
 * User Story: S2-05 (EP-02)
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
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Subject() {
        this.sessions = 15;
        this.weight = BigDecimal.ONE;
        this.minPassScore = new BigDecimal("5.00");
        this.status = "ACTIVE";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSessions() {
        return sessions;
    }

    public void setSessions(Integer sessions) {
        this.sessions = sessions;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
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
}
