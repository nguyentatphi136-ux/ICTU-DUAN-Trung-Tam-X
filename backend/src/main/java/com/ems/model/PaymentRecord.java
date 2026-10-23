package com.ems.model;

import java.io.Serializable;

/**
 * Bản ghi thanh toán học phí (S7-03):
 * AC1: Ghi số tiền, hình thức (tiền mặt, chuyển khoản), ngày thu, người thu, ghi chú.
 * AC4: Sinh biên lai có mã duy nhất (BL-YYYY-XXXXXX).
 * AC5: Đã ghi nhận thì không xoá, chỉ lập bút toán điều chỉnh.
 */
public class PaymentRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;              // Mã biên lai: BL-2026-000001
    private String studentCode;     // Mã học viên: HV260142
    private String studentName;     // Tên học viên
    private int installmentNo;      // Đợt thanh toán (1, 2, 3,...)
    private double amount;          // Số tiền thu
    private String method;          // 'Tiền mặt' hoặc 'Chuyển khoản'
    private String paymentDate;     // yyyy-MM-dd
    private String paymentTime;     // HH:mm
    private String collectedBy;     // Người thu (Kế toán)
    private String referenceCode;   // Mã giao dịch ngân hàng nếu chuyển khoản
    private String notes;           // Ghi chú thu tiền

    public PaymentRecord() {}

    public PaymentRecord(String id, String studentCode, int installmentNo, double amount,
                         String method, String paymentDate, String paymentTime,
                         String collectedBy, String referenceCode, String notes) {
        this.id = id;
        this.studentCode = studentCode;
        this.installmentNo = installmentNo;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
        this.paymentTime = paymentTime;
        this.collectedBy = collectedBy;
        this.referenceCode = referenceCode;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public int getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentTime() { return paymentTime; }
    public void setPaymentTime(String paymentTime) { this.paymentTime = paymentTime; }

    public String getCollectedBy() { return collectedBy; }
    public void setCollectedBy(String collectedBy) { this.collectedBy = collectedBy; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
