package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Entity đại diện bảng programs (Chương trình đào tạo)
 */
public class Program implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String programCode;
    private String programName;
    private String description;
    private int durationMonths;
    private BigDecimal standardTuition;
    private String status;
    private Date createdAt;
    private Date updatedAt;

    public Program() {
    }

    public Program(Integer id, String programCode, String programName) {
        this.id = id;
        this.programCode = programCode;
        this.programName = programName;
        this.status = "ACTIVE";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(int durationMonths) {
        this.durationMonths = durationMonths;
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
