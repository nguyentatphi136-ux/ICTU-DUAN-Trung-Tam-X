package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model biểu diễn một ngày học bị bỏ qua do trùng ngày nghỉ lễ
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class SkippedDate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String date;        // yyyy-MM-dd
    private String reason;      // Tên ngày nghỉ lễ
    private String dayOfWeek;   // Thứ trong tuần

    public SkippedDate() {
    }

    public SkippedDate(String date, String reason) {
        this.date = date;
        this.reason = reason;
        try {
            LocalDate d = LocalDate.parse(date);
            int dow = d.getDayOfWeek().getValue();
            this.dayOfWeek = (dow == 7) ? "CN" : "T" + (dow + 1);
        } catch (Exception e) {
            this.dayOfWeek = "";
        }
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getFormattedDate() {
        if (date == null) return "";
        try {
            LocalDate d = LocalDate.parse(date);
            return d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return date;
        }
    }

    public String getShortDisplayDate() {
        return dayOfWeek + " " + getFormattedDate();
    }
}
