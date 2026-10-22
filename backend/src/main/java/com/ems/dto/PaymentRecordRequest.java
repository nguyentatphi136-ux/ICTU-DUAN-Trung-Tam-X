package com.ems.dto;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu ghi nhận thanh toán từ Client
 */
public class PaymentRecordRequest {
    private Long studentId;
    private String studentCode;
    private Long installmentBillId;
    private Integer installmentOrder;
    private BigDecimal amount;
    private String paymentMethod; // "CASH", "BANK_TRANSFER", "Tiền mặt", "Chuyển khoản"
    private String paymentDate;   // "YYYY-MM-DD"
    private String referenceCode; // Mã GD ngân hàng nếu chuyển khoản
    private String notes;         // Ghi chú nếu có

    public PaymentRecordRequest() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public Long getInstallmentBillId() { return installmentBillId; }
    public void setInstallmentBillId(Long installmentBillId) { this.installmentBillId = installmentBillId; }

    public Integer getInstallmentOrder() { return installmentOrder; }
    public void setInstallmentOrder(Integer installmentOrder) { this.installmentOrder = installmentOrder; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
