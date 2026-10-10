package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Model Entity đại diện cho Chương trình đào tạo (Training Program)
 * User Story: S2-04 (EP-02)
 */
public class TrainingProgram implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer duration;
    private BigDecimal standardTuition;
    private String status; // ACTIVE, INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public TrainingProgram() {
        this.status = "ACTIVE";
        this.standardTuition = BigDecimal.ZERO;
    }

    public TrainingProgram(Integer id, String code, String name, String description,
                           Integer duration, BigDecimal standardTuition, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.standardTuition = standardTuition;
        this.status = status != null ? status : "ACTIVE";
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public BigDecimal getStandardTuition() {
        return standardTuition;
    }

    public void setStandardTuition(BigDecimal standardTuition) {
        this.standardTuition = standardTuition;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
        return "TrainingProgram{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", duration=" + duration +
                ", standardTuition=" + standardTuition +
                ", status='" + status + '\'' +
                '}';
    }
}
