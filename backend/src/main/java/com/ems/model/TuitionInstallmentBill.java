package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * Model Đợt thanh toán học phí cụ thể (Tuition Installment Bill)
 */
public class TuitionInstallmentBill implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tuitionPlanId;
    private Integer installmentOrder;
    private LocalDate dueDate;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private String status; // UNPAID, PARTIAL, PAID, OVERDUE
    private Date lastReminderSentAt;

    public TuitionInstallmentBill() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTuitionPlanId() { return tuitionPlanId; }
    public void setTuitionPlanId(Long tuitionPlanId) { this.tuitionPlanId = tuitionPlanId; }

    public Integer getInstallmentOrder() { return installmentOrder; }
    public void setInstallmentOrder(Integer installmentOrder) { this.installmentOrder = installmentOrder; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getLastReminderSentAt() { return lastReminderSentAt; }
    public void setLastReminderSentAt(Date lastReminderSentAt) { this.lastReminderSentAt = lastReminderSentAt; }

    public BigDecimal getRemainingDebt() {
        if (expectedAmount == null) return BigDecimal.ZERO;
        BigDecimal paid = (paidAmount != null) ? paidAmount : BigDecimal.ZERO;
        BigDecimal rem = expectedAmount.subtract(paid);
        return rem.compareTo(BigDecimal.ZERO) > 0 ? rem : BigDecimal.ZERO;
    }
}
