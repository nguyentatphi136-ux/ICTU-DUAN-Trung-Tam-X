package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * Model Phiếu thu / Biên lai thanh toán (Payment Receipt)
 * Lưu ý: Bảng biên lai thu tiền không được xóa (chỉ lập bút toán điều chỉnh)
 */
public class PaymentReceipt implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String receiptCode;
    private Long installmentBillId;
    private BigDecimal amountPaid;
    private String paymentMethod; // CASH, BANK_TRANSFER, VNPAY, OTHER
    private LocalDate paymentDate;
    private String referenceCode;
    private String notes;
    private Long collectedBy;
    private Date createdAt;

    // Các trường hiển thị mở rộng
    private String collectorName;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String programName;
    private String className;
    private Integer installmentOrder;
    private BigDecimal expectedBillAmount;
    private BigDecimal netAmount; // Sau khi tính các bút toán điều chỉnh
    private BigDecimal billRemainingDebtAfter;
    private List<PaymentAdjustment> adjustments;

    public PaymentReceipt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public Long getInstallmentBillId() { return installmentBillId; }
    public void setInstallmentBillId(Long installmentBillId) { this.installmentBillId = installmentBillId; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getCollectedBy() { return collectedBy; }
    public void setCollectedBy(Long collectedBy) { this.collectedBy = collectedBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Integer getInstallmentOrder() { return installmentOrder; }
    public void setInstallmentOrder(Integer installmentOrder) { this.installmentOrder = installmentOrder; }

    public BigDecimal getExpectedBillAmount() { return expectedBillAmount; }
    public void setExpectedBillAmount(BigDecimal expectedBillAmount) { this.expectedBillAmount = expectedBillAmount; }

    public BigDecimal getNetAmount() { return netAmount != null ? netAmount : amountPaid; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public BigDecimal getBillRemainingDebtAfter() { return billRemainingDebtAfter; }
    public void setBillRemainingDebtAfter(BigDecimal billRemainingDebtAfter) { this.billRemainingDebtAfter = billRemainingDebtAfter; }

    public List<PaymentAdjustment> getAdjustments() { return adjustments; }
    public void setAdjustments(List<PaymentAdjustment> adjustments) { this.adjustments = adjustments; }
}
