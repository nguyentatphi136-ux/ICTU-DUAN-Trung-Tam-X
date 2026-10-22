package com.ems.dto;

import java.math.BigDecimal;

/**
 * DTO trả về thông tin Bút toán điều chỉnh
 */
public class PaymentAdjustmentResponse {
    private Long id;
    private String adjustmentCode;
    private Long paymentReceiptId;
    private String receiptCode;
    private String adjustmentType;
    private BigDecimal deltaAmount;
    private String reason;
    private Long adjustedBy;
    private String adjusterName;
    private String createdAt;

    public PaymentAdjustmentResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdjustmentCode() { return adjustmentCode; }
    public void setAdjustmentCode(String adjustmentCode) { this.adjustmentCode = adjustmentCode; }

    public Long getPaymentReceiptId() { return paymentReceiptId; }
    public void setPaymentReceiptId(Long paymentReceiptId) { this.paymentReceiptId = paymentReceiptId; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public BigDecimal getDeltaAmount() { return deltaAmount; }
    public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(Long adjustedBy) { this.adjustedBy = adjustedBy; }

    public String getAdjusterName() { return adjusterName; }
    public void setAdjusterName(String adjusterName) { this.adjusterName = adjusterName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
