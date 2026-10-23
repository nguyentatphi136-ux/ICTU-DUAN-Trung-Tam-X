package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model đại diện thông tin học viên (Student Profile)
 * Đáp ứng các nghiệp vụ quản lý học vụ và thay đổi tình trạng học (S4-06)
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String code; // Mã học viên, VD: HV260101
    private String name;
    private String phone;
    private String email;
    private String loginEmail; // Email đăng nhập hệ thống cấp
    private String program; // Chương trình đào tạo
    private String status; // 'Đang học', 'Bảo lưu', 'Thôi học', 'Mới nhập học', 'Đã tốt nghiệp'
    private String enrolledAt; // yyyy-MM-dd
    private String currentClass; // Mã lớp đang học, null/rỗng nếu bảo lưu hoặc thôi học
    private String birthDate;
    private String address;
    private String counselor;
    private String entryScore;
    private String level;
    private String background;
    private List<String> notes = new ArrayList<>();
    private List<PastClass> history = new ArrayList<>();
    private List<StudentStatusLog> statusChanges = new ArrayList<>();

    public Student() {
    }

    public Student(String code, String name, String phone, String email, String program,
                   String status, String enrolledAt, String currentClass) {
        this.code = code;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.program = program;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.currentClass = currentClass;
    }

    public boolean isStudying() {
        return "Đang học".equalsIgnoreCase(status);
    }

    public boolean isSuspended() {
        return "Bảo lưu".equalsIgnoreCase(status);
    }

    public boolean isDroppedOut() {
        return "Thôi học".equalsIgnoreCase(status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLoginEmail() {
        return loginEmail;
    }

    public void setLoginEmail(String loginEmail) {
        this.loginEmail = loginEmail;
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(String enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getCurrentClass() {
        return currentClass;
    }

    public void setCurrentClass(String currentClass) {
        this.currentClass = currentClass;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCounselor() {
        return counselor;
    }

    public void setCounselor(String counselor) {
        this.counselor = counselor;
    }

    public String getEntryScore() {
        return entryScore;
    }

    public void setEntryScore(String entryScore) {
        this.entryScore = entryScore;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public List<PastClass> getHistory() {
        return history;
    }

    public void setHistory(List<PastClass> history) {
        this.history = history;
    }

    public List<StudentStatusLog> getStatusChanges() {
        return statusChanges;
    }

    public void setStatusChanges(List<StudentStatusLog> statusChanges) {
        this.statusChanges = statusChanges;
    }
}
