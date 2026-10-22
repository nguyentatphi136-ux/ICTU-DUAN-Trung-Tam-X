package com.ems.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về thông tin chi tiết Biên lai / Phiếu thu
 */
public class PaymentReceiptResponse {
    private Long id;
    private String receiptCode;
    private Long installmentBillId;
    private Integer installmentOrder;
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String programName;
    private String className;
    private BigDecimal amountPaid;
    private BigDecimal netAmount; // Số tiền thực tế sau điều chỉnh
    private String amountInWords; // Số tiền viết bằng chữ
    private String paymentMethod;
    private String paymentDate;
    private String referenceCode;
    private String notes;
    private Long collectedBy;
    private String collectorName;
    private String createdAt;

    private BigDecimal billExpectedAmount;
    private BigDecimal billRemainingDebt;
    private String billStatus;

    private List<PaymentAdjustmentResponse> adjustments;

    public PaymentReceiptResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public Long getInstallmentBillId() { return installmentBillId; }
    public void setInstallmentBillId(Long installmentBillId) { this.installmentBillId = installmentBillId; }

    public Integer getInstallmentOrder() { return installmentOrder; }
    public void setInstallmentOrder(Integer installmentOrder) { this.installmentOrder = installmentOrder; }

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

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getCollectedBy() { return collectedBy; }
    public void setCollectedBy(Long collectedBy) { this.collectedBy = collectedBy; }

    public String getCollectorName() { return collectorName; }
    public void setCollectorName(String collectorName) { this.collectorName = collectorName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public BigDecimal getBillExpectedAmount() { return billExpectedAmount; }
    public void setBillExpectedAmount(BigDecimal billExpectedAmount) { this.billExpectedAmount = billExpectedAmount; }

    public BigDecimal getBillRemainingDebt() { return billRemainingDebt; }
    public void setBillRemainingDebt(BigDecimal billRemainingDebt) { this.billRemainingDebt = billRemainingDebt; }

    public String getBillStatus() { return billStatus; }
    public void setBillStatus(String billStatus) { this.billStatus = billStatus; }

    public List<PaymentAdjustmentResponse> getAdjustments() { return adjustments; }
    public void setAdjustments(List<PaymentAdjustmentResponse> adjustments) { this.adjustments = adjustments; }
}
