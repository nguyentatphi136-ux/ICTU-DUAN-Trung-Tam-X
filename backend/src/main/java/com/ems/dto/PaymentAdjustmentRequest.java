package com.ems.dto;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu lập bút toán điều chỉnh từ Client
 */
public class PaymentAdjustmentRequest {
    private Long paymentReceiptId;
    private String receiptCode;
    private String adjustmentType; // "REDUCE", "INCREASE", "CANCEL", "Giảm khoản thu", "Tăng khoản thu", "Huỷ toàn bộ"
    private BigDecimal amount;     // Số tiền điều chỉnh (dương)
    private String reason;         // Lý do điều chỉnh (bắt buộc)

    public PaymentAdjustmentRequest() {}

    public Long getPaymentReceiptId() { return paymentReceiptId; }
    public void setPaymentReceiptId(Long paymentReceiptId) { this.paymentReceiptId = paymentReceiptId; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
