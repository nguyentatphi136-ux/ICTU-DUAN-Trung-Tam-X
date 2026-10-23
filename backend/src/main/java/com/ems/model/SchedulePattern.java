package com.ems.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model chứa tham số khai báo mẫu lặp để sinh thời khoá biểu tự động
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class SchedulePattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private String classCode;
    /**
     * Danh sách các thứ trong tuần (0: Chủ nhật, 1: Thứ Hai, 2: Thứ Ba, ..., 6: Thứ Bảy)
     */
    private List<Integer> days = new ArrayList<>();
    private String start;           // Giờ bắt đầu (HH:mm)
    private String end;             // Giờ kết thúc (HH:mm)
    private String from;            // Ngày bắt đầu (yyyy-MM-dd)
    private int count;              // Số buổi cần sinh (ví dụ: 60)
    private String room;            // Phòng học mặc định

    public SchedulePattern() {
    }

    public SchedulePattern(String classCode, List<Integer> days, String start, String end, String from, int count, String room) {
        this.classCode = classCode;
        if (days != null) {
            this.days = days;
        }
        this.start = start;
        this.end = end;
        this.from = from;
        this.count = count;
        this.room = room;
    }

    public boolean hasDay(int dayOfWeek) {
        return days != null && days.contains(dayOfWeek);
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public List<Integer> getDays() {
        return days;
    }

    public void setDays(List<Integer> days) {
        this.days = days;
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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }
}
