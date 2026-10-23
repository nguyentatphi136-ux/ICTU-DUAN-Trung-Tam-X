package com.ems.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class SubjectRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String name;
    private Integer sessions;
    private BigDecimal weight;
    private BigDecimal minPassScore;
    private String outcome;
    private String status;

    public SubjectRequest() {}

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
}
