package com.ems.dto;

import com.ems.model.AdjustmentRecord;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO dữ liệu Biên lai thanh toán học phí (S7-03):
 * AC4: Sinh biên lai có mã duy nhất (BL-YYYY-XXXXXX), in hoặc tải PDF được.
 * AC5: Chứa lịch sử các bút toán điều chỉnh (nếu có).
 */
public class PaymentReceiptResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String receiptCode;         // BL-2026-000001
    private String studentCode;         // HV260142
    private String studentName;         // DO TUAN KIET
    private String programName;         // Lập trình web Full-stack
    private String classCode;           // WEB-K16
    private int installmentNo;          // Đợt 1
    private String installmentLabel;    // Khi nhập học
    private double amountPaid;          // Số tiền gốc đã thu
    private double netAmount;           // Số tiền thực thu sau khi trừ/cộng bút toán điều chỉnh
    private String amountInWords;       // Số tiền viết bằng chữ
    private String paymentMethod;       // 'Tiền mặt' hoặc 'Chuyển khoản'
    private String paymentDate;         // yyyy-MM-dd
    private String paymentTime;         // HH:mm
    private String collectedBy;         // Tên người thu (Kế toán)
    private String referenceCode;       // Mã GD ngân hàng nếu chuyển khoản
    private String notes;

    // Tình trạng của đợt thanh toán (AC2, AC3)
    private double installmentTotal;      // Tổng số tiền phải đóng của đợt này
    private double installmentPaidTotal;  // Tổng số tiền đã thanh toán của đợt này
    private double installmentRemaining;  // Số tiền còn nợ của đợt này
    private String installmentStatus;     // 'Chưa thanh toán', 'Thanh toán một phần', 'Đã thanh toán'

    // Thông tin trung tâm phục vụ in/xuất PDF biên lai chính thức
    private String centerName = "TRUNG TÂM ĐÀO TẠO TMS";
    private String centerAddress = "Tầng 3, cơ sở Cầu Giấy, Hà Nội · 024 0000 0000";

    // Danh sách các bút toán điều chỉnh liên quan (AC5)
    private List<AdjustmentRecord> adjustments = new ArrayList<>();

    public PaymentReceiptResponse() {}

    public String getReceiptCode() { return receiptCode; }
    public void setReceiptCode(String receiptCode) { this.receiptCode = receiptCode; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }

    public String getClassCode() { return classCode; }
    public void setClassCode(String classCode) { this.classCode = classCode; }

    public int getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }

    public String getInstallmentLabel() { return installmentLabel; }
    public void setInstallmentLabel(String installmentLabel) { this.installmentLabel = installmentLabel; }

    public double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }

    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }

    public String getAmountInWords() { return amountInWords; }
    public void setAmountInWords(String amountInWords) { this.amountInWords = amountInWords; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

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

    public double getInstallmentTotal() { return installmentTotal; }
    public void setInstallmentTotal(double installmentTotal) { this.installmentTotal = installmentTotal; }

    public double getInstallmentPaidTotal() { return installmentPaidTotal; }
    public void setInstallmentPaidTotal(double installmentPaidTotal) { this.installmentPaidTotal = installmentPaidTotal; }

    public double getInstallmentRemaining() { return installmentRemaining; }
    public void setInstallmentRemaining(double installmentRemaining) { this.installmentRemaining = installmentRemaining; }

    public String getInstallmentStatus() { return installmentStatus; }
    public void setInstallmentStatus(String installmentStatus) { this.installmentStatus = installmentStatus; }

    public String getCenterName() { return centerName; }
    public void setCenterName(String centerName) { this.centerName = centerName; }

    public String getCenterAddress() { return centerAddress; }
    public void setCenterAddress(String centerAddress) { this.centerAddress = centerAddress; }

    public List<AdjustmentRecord> getAdjustments() { return adjustments; }
    public void setAdjustments(List<AdjustmentRecord> adjustments) { this.adjustments = adjustments != null ? adjustments : new ArrayList<>(); }
}
