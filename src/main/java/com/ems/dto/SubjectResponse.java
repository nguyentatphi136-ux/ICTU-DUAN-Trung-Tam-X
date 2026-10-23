package com.ems.dto;

import com.ems.model.Subject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

public class SubjectResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String code;
    private String name;
    private Integer sessions;
    private BigDecimal weight;
    private BigDecimal minPassScore;
    private String outcome;
    private String status;
    private int openedClassesCount;
    private int programsCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public static SubjectResponse fromEntity(Subject subject) {
        if (subject == null) return null;
        SubjectResponse res = new SubjectResponse();
        res.id = subject.getId();
        res.code = subject.getCode();
        res.name = subject.getName();
        res.sessions = subject.getSessions();
        res.weight = subject.getWeight();
        res.minPassScore = subject.getMinPassScore();
        res.outcome = subject.getOutcome();
        res.status = subject.getStatus();
        res.openedClassesCount = subject.getOpenedClassesCount();
        res.programsCount = subject.getProgramsCount();
        res.createdAt = subject.getCreatedAt();
        res.updatedAt = subject.getUpdatedAt();
        return res;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Integer getSessions() {
        return sessions;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public BigDecimal getMinPassScore() {
        return minPassScore;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getStatus() {
        return status;
    }

    public int getOpenedClassesCount() {
        return openedClassesCount;
    }

    public int getProgramsCount() {
        return programsCount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
