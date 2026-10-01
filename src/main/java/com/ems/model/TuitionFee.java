package com.ems.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class TuitionFee implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int studentId;
    private String studentName;
    private String courseName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String status; // unpaid, partially_paid, paid
    private String receiptNo;
    private String updatedBy;

    public TuitionFee() {}

    public TuitionFee(int id, int studentId, String studentName, String courseName, BigDecimal totalAmount, BigDecimal paidAmount, String status, String receiptNo, String updatedBy) {
        this.id = id;
        this.studentId = studentId;
        this.studentName = studentName;
        this.courseName = courseName;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.receiptNo = receiptNo;
        this.updatedBy = updatedBy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
