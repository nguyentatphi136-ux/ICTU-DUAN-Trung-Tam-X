package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Model Kế hoạch học phí cá nhân của học viên (Student Tuition Plan)
 */
public class StudentTuitionPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long enrollmentId;
    private Long studentId;
    private BigDecimal originalTuition;
    private String discountType; // NONE, AMOUNT, PERCENT
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private String discountReason;
    private Long discountApprovedBy;
    private BigDecimal finalPayableAmount;
    private BigDecimal paidAmount;
    private BigDecimal debtAmount;
    private String status; // UNPAID, PARTIAL, COMPLETED, OVERDUE
    private Date createdAt;
    private Date updatedAt;

    public StudentTuitionPlan() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public BigDecimal getOriginalTuition() { return originalTuition; }
    public void setOriginalTuition(BigDecimal originalTuition) { this.originalTuition = originalTuition; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public String getDiscountReason() { return discountReason; }
    public void setDiscountReason(String discountReason) { this.discountReason = discountReason; }

    public Long getDiscountApprovedBy() { return discountApprovedBy; }
    public void setDiscountApprovedBy(Long discountApprovedBy) { this.discountApprovedBy = discountApprovedBy; }

    public BigDecimal getFinalPayableAmount() { return finalPayableAmount; }
    public void setFinalPayableAmount(BigDecimal finalPayableAmount) { this.finalPayableAmount = finalPayableAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getDebtAmount() { return debtAmount; }
    public void setDebtAmount(BigDecimal debtAmount) { this.debtAmount = debtAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
