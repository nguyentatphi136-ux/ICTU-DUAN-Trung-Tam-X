package com.ems.dto;

import java.io.Serializable;

/**
 * Yêu cầu lập bút toán điều chỉnh khoản thu học phí (S7-03 AC5):
 * Đã ghi nhận thì không xoá, chỉ lập bút toán điều chỉnh kèm lý do bắt buộc.
 */
public class CreateAdjustmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paymentId; // Mã biên lai gốc: BL-2026-000001
    private String kind;      // 'Giảm khoản thu', 'Tăng khoản thu', 'Huỷ toàn bộ'
    private double amount;    // Số tiền điều chỉnh (nếu 'Huỷ toàn bộ' thì lấy toàn bộ số tiền thực thu còn lại)
    private String reason;    // Lý do điều chỉnh (bắt buộc)

    public CreateAdjustmentRequest() {}

    public CreateAdjustmentRequest(String paymentId, String kind, double amount, String reason) {
        this.paymentId = paymentId;
        this.kind = kind;
        this.amount = amount;
        this.reason = reason;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
