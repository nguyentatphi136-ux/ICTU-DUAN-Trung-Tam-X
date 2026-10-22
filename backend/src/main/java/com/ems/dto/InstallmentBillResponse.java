package com.ems.dto;

import java.math.BigDecimal;

/**
 * DTO trả về thông tin đợt thanh toán kèm công nợ
 */
public class InstallmentBillResponse {
    private Long id;
    private Long tuitionPlanId;
    private Integer installmentOrder;
    private String installmentName;
    private String dueDate;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingDebt;
    private String status; // UNPAID, PARTIAL, PAID, OVERDUE
    private java.util.List<PaymentReceiptResponse> receipts;

    public InstallmentBillResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTuitionPlanId() { return tuitionPlanId; }
    public void setTuitionPlanId(Long tuitionPlanId) { this.tuitionPlanId = tuitionPlanId; }

    public Integer getInstallmentOrder() { return installmentOrder; }
    public void setInstallmentOrder(Integer installmentOrder) { this.installmentOrder = installmentOrder; }

    public String getInstallmentName() { return installmentName; }
    public void setInstallmentName(String installmentName) { this.installmentName = installmentName; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getRemainingDebt() { return remainingDebt; }
    public void setRemainingDebt(BigDecimal remainingDebt) { this.remainingDebt = remainingDebt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.util.List<PaymentReceiptResponse> getReceipts() { return receipts; }
    public void setReceipts(java.util.List<PaymentReceiptResponse> receipts) { this.receipts = receipts; }
}
