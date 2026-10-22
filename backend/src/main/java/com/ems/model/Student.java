package com.ems.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Model đại diện cho Học viên trong hệ thống EMS
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String studentCode;
    private String entryLevel;
    private String enrollmentStatus;
    private Date createdAt;
    private Date updatedAt;

    // Các trường hiển thị mở rộng (join từ bảng users, classes)
    private String fullName;
    private String email;
    private String phone;
    private String className;
    private String programName;

    public Student() {}

    public Student(Long id, Long userId, String studentCode, String entryLevel, String enrollmentStatus) {
        this.id = id;
        this.userId = userId;
        this.studentCode = studentCode;
        this.entryLevel = entryLevel;
        this.enrollmentStatus = enrollmentStatus;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }

    public String getEntryLevel() { return entryLevel; }
    public void setEntryLevel(String entryLevel) { this.entryLevel = entryLevel; }

    public String getEnrollmentStatus() { return enrollmentStatus; }
    public void setEnrollmentStatus(String enrollmentStatus) { this.enrollmentStatus = enrollmentStatus; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getProgramName() { return programName; }
    public void setProgramName(String programName) { this.programName = programName; }
}
