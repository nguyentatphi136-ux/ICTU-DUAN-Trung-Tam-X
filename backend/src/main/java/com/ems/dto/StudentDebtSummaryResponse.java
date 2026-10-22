package com.ems.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO trả về tổng hợp học phí và công nợ của học viên
 */
public class StudentDebtSummaryResponse {
    private Long studentId;
    private String studentCode;
    private String studentName;
    private String programName;
    private String className;

    private Long tuitionPlanId;
    private BigDecimal originalTuition;
    private String discountLabel;
    private BigDecimal discountAmount;
    private BigDecimal finalPayableAmount; // Tổng phải thu
    private BigDecimal totalPaidAmount;    // Tổng đã thu
    private BigDecimal totalDebtAmount;    // Còn nợ
    private Integer overdueDays;           // Số ngày quá hạn
    private String planStatus;             // UNPAID, PARTIAL, COMPLETED, OVERDUE

    private List<InstallmentBillResponse> bills;
    private List<PaymentReceiptResponse> receipts;

    public StudentDebtSummaryResponse() {}

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

    public Long getTuitionPlanId() { return tuitionPlanId; }
    public void setTuitionPlanId(Long tuitionPlanId) { this.tuitionPlanId = tuitionPlanId; }

    public BigDecimal getOriginalTuition() { return originalTuition; }
    public void setOriginalTuition(BigDecimal originalTuition) { this.originalTuition = originalTuition; }

    public String getDiscountLabel() { return discountLabel; }
    public void setDiscountLabel(String discountLabel) { this.discountLabel = discountLabel; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getFinalPayableAmount() { return finalPayableAmount; }
    public void setFinalPayableAmount(BigDecimal finalPayableAmount) { this.finalPayableAmount = finalPayableAmount; }

    public BigDecimal getTotalPaidAmount() { return totalPaidAmount; }
    public void setTotalPaidAmount(BigDecimal totalPaidAmount) { this.totalPaidAmount = totalPaidAmount; }

    public BigDecimal getTotalDebtAmount() { return totalDebtAmount; }
    public void setTotalDebtAmount(BigDecimal totalDebtAmount) { this.totalDebtAmount = totalDebtAmount; }

    public Integer getOverdueDays() { return overdueDays; }
    public void setOverdueDays(Integer overdueDays) { this.overdueDays = overdueDays; }

    public String getPlanStatus() { return planStatus; }
    public void setPlanStatus(String planStatus) { this.planStatus = planStatus; }

    public List<InstallmentBillResponse> getBills() { return bills; }
    public void setBills(List<InstallmentBillResponse> bills) { this.bills = bills; }

    public List<PaymentReceiptResponse> getReceipts() { return receipts; }
    public void setReceipts(List<PaymentReceiptResponse> receipts) { this.receipts = receipts; }
}
