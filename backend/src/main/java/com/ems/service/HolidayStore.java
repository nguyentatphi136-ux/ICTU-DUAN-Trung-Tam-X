package com.ems.service;

import com.ems.model.Holiday;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Quản lý danh mục ngày nghỉ lễ của hệ thống (Thread-safe)
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class HolidayStore {

    private static final List<Holiday> HOLIDAYS = new CopyOnWriteArrayList<>();

    static {
        resetToDefault();
    }

    public static synchronized void resetToDefault() {
        HOLIDAYS.clear();
        long id = 1;
        HOLIDAYS.add(new Holiday(id++, "Ngày Nhà giáo Việt Nam", LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20)));
        HOLIDAYS.add(new Holiday(id++, "Tết Dương lịch", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1)));
        HOLIDAYS.add(new Holiday(id++, "Nghỉ Tết Nguyên đán", LocalDate.of(2027, 2, 5), LocalDate.of(2027, 2, 14)));
        HOLIDAYS.add(new Holiday(id++, "Giỗ Tổ Hùng Vương", LocalDate.of(2027, 4, 26), LocalDate.of(2027, 4, 26)));
        HOLIDAYS.add(new Holiday(id++, "Giải phóng miền Nam, Quốc tế Lao động", LocalDate.of(2027, 4, 30), LocalDate.of(2027, 5, 1)));
        sortHolidays();
    }

    public static List<Holiday> getAllHolidays() {
        return Collections.unmodifiableList(new ArrayList<>(HOLIDAYS));
    }

    public static synchronized void addHoliday(String name, String fromStr, String toStr) {
        if (name == null || name.isBlank() || fromStr == null || fromStr.isBlank()) {
            throw new IllegalArgumentException("Tên và ngày bắt đầu nghỉ lễ không được để trống.");
        }
        LocalDate fromDate = LocalDate.parse(fromStr);
        LocalDate toDate = (toStr != null && !toStr.isBlank()) ? LocalDate.parse(toStr) : fromDate;
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Ngày kết thúc nghỉ lễ phải bằng hoặc sau ngày bắt đầu.");
        }

        long maxId = HOLIDAYS.stream().mapToLong(h -> h.getId() != null ? h.getId() : 0).max().orElse(0);
        HOLIDAYS.add(new Holiday(maxId + 1, name.trim(), fromDate, toDate));
        sortHolidays();
    }

    public static Holiday findHolidayCovering(LocalDate date) {
        if (date == null) return null;
        for (Holiday h : HOLIDAYS) {
            if (h.covers(date)) {
                return h;
            }
        }
        return null;
    }

    public static boolean isHoliday(LocalDate date) {
        return findHolidayCovering(date) != null;
    }

    private static void sortHolidays() {
        List<Holiday> list = new ArrayList<>(HOLIDAYS);
        list.sort(Comparator.comparing(Holiday::getFromDate));
        HOLIDAYS.clear();
        HOLIDAYS.addAll(list);
    }
}
