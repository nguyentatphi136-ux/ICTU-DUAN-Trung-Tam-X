package com.ems.model;

import java.io.Serializable;

/**
 * Bút toán điều chỉnh khoản thu học phí (S7-03 AC5):
 * Đã ghi nhận thì không xoá, nếu ghi sai chỉ lập bút toán điều chỉnh
 * (Giảm khoản thu, Tăng khoản thu, Huỷ toàn bộ) kèm lý do bắt buộc.
 */
public class AdjustmentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;        // Mã bút toán: ĐC-2026-000001
    private String paymentId; // Mã biên lai gốc: BL-2026-000001
    private String kind;      // 'Giảm khoản thu', 'Tăng khoản thu', 'Huỷ toàn bộ'
    private double delta;     // Số tiền điều chỉnh (âm nếu giảm/huỷ, dương nếu tăng)
    private String reason;    // Lý do điều chỉnh (bắt buộc)
    private String by;        // Người lập bút toán
    private String at;        // Thời điểm lập (yyyy-MM-ddTHH:mm:ss)

    public AdjustmentRecord() {}

    public AdjustmentRecord(String id, String paymentId, String kind, double delta, String reason, String by, String at) {
        this.id = id;
        this.paymentId = paymentId;
        this.kind = kind;
        this.delta = delta;
        this.reason = reason;
        this.by = by;
        this.at = at;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }

    public double getDelta() { return delta; }
    public void setDelta(double delta) { this.delta = delta; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }

    public String getAt() { return at; }
    public void setAt(String at) { this.at = at; }
}
