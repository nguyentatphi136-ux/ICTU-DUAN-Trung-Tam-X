package com.ems.dto;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu tạo mới hoặc cập nhật Chương trình đào tạo
 */
public class TrainingProgramRequest {
    private String code;
    private String name;
    private String description;
    private Integer duration;
    private BigDecimal standardTuition;
    private String status;

    public TrainingProgramRequest() {
    }

    public TrainingProgramRequest(String code, String name, String description,
                                  Integer duration, BigDecimal standardTuition, String status) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.standardTuition = standardTuition;
        this.status = status;
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
}
