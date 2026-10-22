package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model biểu diễn lớp học trong hệ thống đào tạo (EMS)
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class Klass implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;                // classes.id

    private String code;            // Mã lớp: ví dụ WEB-K16, WEB-K15
    private String name;            // Tên lớp
    private String program;         // Chương trình đào tạo: ví dụ "Lập trình web Full-stack"
    private String start;           // Ngày khai giảng / bắt đầu: "yyyy-MM-dd"
    private int max;                // Sĩ số tối đa
    private String mode;            // Hình thức học: "Trực tiếp", "Trực tuyến", "Kết hợp"
    private String status;          // Trạng thái: "Chuẩn bị", "Đang học", "Kết thúc", "Huỷ"
    private int done;               // Số buổi đã học
    private String slot;            // Khung giờ / ca học: ví dụ "Tối 2-4-6" hoặc "Chưa xếp lịch"
    private String room;            // Phòng học mặc định: "P.401", "Lab 1", "Zoom 01"...
    private List<ClassSubject> subjects = new ArrayList<>(); // Danh sách môn học theo thứ tự

    public Klass() {
    }

    public Klass(String code, String program, String start, int max, String mode, String status, String slot, String room, List<ClassSubject> subjects) {
        this.code = code;
        this.name = "Lớp " + code;
        this.program = program;
        this.start = start;
        this.max = max;
        this.mode = mode;
        this.status = status;
        this.slot = slot;
        this.room = room;
        if (subjects != null) {
            this.subjects = subjects;
        }
    }

    public int getTotalSessions() {
        if (subjects == null) return 0;
        return subjects.stream().mapToInt(ClassSubject::getSessions).sum();
    }

    public int getMissingTeacherCount() {
        if (subjects == null) return 0;
        return (int) subjects.stream().filter(s -> s.getTeacher() == null || s.getTeacher().isBlank()).count();
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

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDone() {
        return done;
    }

    public void setDone(int done) {
        this.done = done;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public List<ClassSubject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<ClassSubject> subjects) {
        this.subjects = subjects;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
