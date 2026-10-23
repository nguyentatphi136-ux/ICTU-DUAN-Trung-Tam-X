package com.ems.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Model biểu diễn ngày nghỉ lễ trong hệ thống đào tạo
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class Holiday implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private LocalDate fromDate;
    private LocalDate toDate; // Có thể null nếu nghỉ 1 ngày

    public Holiday() {
    }

    public Holiday(Long id, String name, LocalDate fromDate, LocalDate toDate) {
        this.id = id;
        this.name = name;
        this.fromDate = fromDate;
        this.toDate = toDate != null ? toDate : fromDate;
    }

    public Holiday(String name, String fromDateStr, String toDateStr) {
        this.name = name;
        this.fromDate = LocalDate.parse(fromDateStr);
        this.toDate = (toDateStr != null && !toDateStr.isBlank()) ? LocalDate.parse(toDateStr) : this.fromDate;
    }

    public Holiday(String name, String fromDateStr) {
        this(name, fromDateStr, null);
    }

    /**
     * Kiểm tra xem một ngày cụ thể có rơi vào đợt nghỉ lễ này hay không
     */
    public boolean covers(LocalDate date) {
        if (date == null || fromDate == null) return false;
        LocalDate effectiveTo = (toDate != null) ? toDate : fromDate;
        return !date.isBefore(fromDate) && !date.isAfter(effectiveTo);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getFormattedRange() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (toDate == null || toDate.equals(fromDate)) {
            return fromDate.format(dtf);
        }
        return fromDate.format(DateTimeFormatter.ofPattern("dd/MM")) + " – " + toDate.format(dtf);
    }

    @Override
    public String toString() {
        return "Holiday{" +
                "name='" + name + '\'' +
                ", fromDate=" + fromDate +
                ", toDate=" + toDate +
                '}';
    }
}
