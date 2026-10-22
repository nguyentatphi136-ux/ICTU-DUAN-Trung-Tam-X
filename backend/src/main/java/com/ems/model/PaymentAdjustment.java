package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Model Bút toán điều chỉnh khoản thu (Payment Adjustment - S7-03)
 */
public class PaymentAdjustment implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String adjustmentCode;
    private Long paymentReceiptId;
    private String adjustmentType; // REDUCE, INCREASE, CANCEL
    private BigDecimal deltaAmount; // Số tiền điều chỉnh: dương là tăng thu, âm là giảm/hủy
    private String reason;
    private Long adjustedBy;
    private Date createdAt;

    // Trường hiển thị mở rộng
    private String adjusterName;
    private String receiptCode;

    public PaymentAdjustment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdjustmentCode() { return adjustmentCode; }
    public void setAdjustmentCode(String adjustmentCode) { this.adjustmentCode = adjustmentCode; }

    public Long getPaymentReceiptId() { return paymentReceiptId; }
    public void setPaymentReceiptId(Long paymentReceiptId) { this.paymentReceiptId = paymentReceiptId; }

    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }

    public BigDecimal getDeltaAmount() { return deltaAmount; }
    public void setDeltaAmount(BigDecimal deltaAmount) { this.deltaAmount = deltaAmount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Long getAdjustedBy() { return adjustedBy; }
    public void setAdjustedBy(Long adjustedBy) { this.adjustedBy = adjustedBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getAdjusterName() { return adjusterName; }
    public void setAdjusterName(String adjusterName) { this.adjusterName = adjusterName; }

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }
}
