package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model biểu diễn một buổi học trong thời khoá biểu lớp (Class Session)
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class PlannedSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;              // Ví dụ: WEB-K16-1
    private String cls;             // Mã lớp: WEB-K16
    private int no;                 // Số thứ tự buổi: 1, 2, ...
    private String date;            // yyyy-MM-dd
    private String start;           // HH:mm, ví dụ 18:00
    private String end;             // HH:mm, ví dụ 20:30
    private String subject;         // Mã môn: GIT-01
    private int lesson;             // Bài số mấy trong môn: 1, 2...
    private String topic;           // Chủ đề bài học
    private String teacher;         // Giảng viên đứng lớp
    private String room;            // Phòng học
    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED

    public PlannedSession() {
    }

    public PlannedSession(String id, String cls, int no, String date, String start, String end,
                        String subject, int lesson, String topic, String teacher, String room) {
        this.id = id;
        this.cls = cls;
        this.no = no;
        this.date = date;
        this.start = start;
        this.end = end;
        this.subject = subject;
        this.lesson = lesson;
        this.topic = topic;
        this.teacher = teacher;
        this.room = room;
        this.status = "SCHEDULED";
    }

    /**
     * Lấy tên thứ rút gọn trong tuần (T2, T3, T4, T5, T6, T7, CN)
     */
    public String getShortDayOfWeek() {
        if (date == null || date.isBlank()) return "";
        try {
            LocalDate d = LocalDate.parse(date);
            int dow = d.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
            if (dow == 7) return "CN";
            return "T" + (dow + 1);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Lấy tên thứ đầy đủ trong tuần (Thứ Hai, Thứ Ba... Chủ nhật)
     */
    public String getFullDayOfWeek() {
        if (date == null || date.isBlank()) return "";
        try {
            LocalDate d = LocalDate.parse(date);
            int dow = d.getDayOfWeek().getValue();
            switch (dow) {
                case 1: return "Thứ Hai";
                case 2: return "Thứ Ba";
                case 3: return "Thứ Tư";
                case 4: return "Thứ Năm";
                case 5: return "Thứ Sáu";
                case 6: return "Thứ Bảy";
                case 7: return "Chủ nhật";
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Định dạng ngày dd/MM/yyyy
     */
    public String getFormattedDate() {
        if (date == null || date.isBlank()) return "";
        try {
            LocalDate d = LocalDate.parse(date);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return date;
        }
    }

    /**
     * Chuỗi ngày hiển thị: T4 21/10/2026
     */
    public String getShortDisplayDate() {
        return getShortDayOfWeek() + " " + getFormattedDate();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCls() {
        return cls;
    }

    public void setCls(String cls) {
        this.cls = cls;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public int getLesson() {
        return lesson;
    }

    public void setLesson(int lesson) {
        this.lesson = lesson;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
