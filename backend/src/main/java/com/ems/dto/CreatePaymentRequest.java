package com.ems.dto;

import java.io.Serializable;

/**
 * Yêu cầu ghi nhận khoản thanh toán học phí (S7-03):
 * AC1: Số tiền, hình thức (tiền mặt, chuyển khoản), ngày thu, người thu, ghi chú.
 * AC2: Thu một phần hoặc toàn bộ.
 * AC3: Không cho thu vượt số tiền còn nợ của đợt.
 */
public class CreatePaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String studentCode;
    private int installmentNo;
    private double amount;
    private String method; // 'Tiền mặt' hoặc 'Chuyển khoản'
    private String paymentDate; // yyyy-MM-dd
    private String referenceCode; // Mã giao dịch ngân hàng (bắt buộc nếu chuyển khoản)
    private String notes;

    public CreatePaymentRequest() {}

    public CreatePaymentRequest(String studentCode, int installmentNo, double amount,
                                String method, String paymentDate, String referenceCode, String notes) {
        this.studentCode = studentCode;
        this.installmentNo = installmentNo;
        this.amount = amount;
        this.method = method;
        this.paymentDate = paymentDate;
        this.referenceCode = referenceCode;
        this.notes = notes;
    }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public int getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPaymentDate() { return paymentDate; }
    public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

    public String getReferenceCode() { return referenceCode; }
    public void setReferenceCode(String referenceCode) { this.referenceCode = referenceCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
