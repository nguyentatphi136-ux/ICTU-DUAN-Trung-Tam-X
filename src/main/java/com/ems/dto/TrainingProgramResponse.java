package com.ems.dto;

import com.ems.model.TrainingProgram;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;

/**
 * DTO trả về thông tin Chương trình đào tạo
 */
public class TrainingProgramResponse {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private Integer duration;
    private BigDecimal standardTuition;
    private String status;
    private String createdAt;
    private String updatedAt;

    public TrainingProgramResponse() {
    }

    public static TrainingProgramResponse fromEntity(TrainingProgram program) {
        if (program == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        TrainingProgramResponse resp = new TrainingProgramResponse();
        resp.setId(program.getId());
        resp.setCode(program.getCode());
        resp.setName(program.getName());
        resp.setDescription(program.getDescription());
        resp.setDuration(program.getDuration());
        resp.setStandardTuition(program.getStandardTuition());
        resp.setStatus(program.getStatus());
        if (program.getCreatedAt() != null) {
            resp.setCreatedAt(sdf.format(program.getCreatedAt()));
        }
        if (program.getUpdatedAt() != null) {
            resp.setUpdatedAt(sdf.format(program.getUpdatedAt()));
        }
        return resp;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
