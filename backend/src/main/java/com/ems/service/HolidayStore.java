package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.model.Holiday;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Danh mục ngày nghỉ lễ của trung tâm, lưu ở bảng holidays (S4TimetableMigration tạo bảng).
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class HolidayStore {

    public static List<Holiday> getAllHolidays() {
        List<Holiday> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, from_date, to_date FROM holidays ORDER BY from_date, id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Holiday(rs.getLong("id"), rs.getString("name"),
                        rs.getDate("from_date").toLocalDate(), rs.getDate("to_date").toLocalDate()));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách ngày nghỉ lễ: " + e.getMessage(), e);
        }
        return list;
    }

    public static void addHoliday(String name, String fromStr, String toStr) {
        if (name == null || name.isBlank() || fromStr == null || fromStr.isBlank()) {
            throw new IllegalArgumentException("Tên và ngày bắt đầu nghỉ lễ không được để trống.");
        }
        LocalDate fromDate = LocalDate.parse(fromStr);
        LocalDate toDate = (toStr != null && !toStr.isBlank()) ? LocalDate.parse(toStr) : fromDate;
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("Ngày kết thúc nghỉ lễ phải bằng hoặc sau ngày bắt đầu.");
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO holidays (name, from_date, to_date) VALUES (?, ?, ?)")) {
            ps.setString(1, name.trim());
            ps.setDate(2, Date.valueOf(fromDate));
            ps.setDate(3, Date.valueOf(toDate));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Không lưu được ngày nghỉ lễ: " + e.getMessage(), e);
        }
    }

    /** Ngày nghỉ lễ chứa date trong danh sách đã đọc sẵn (sinh lịch đọc một lần rồi dò từng ngày). */
    public static Holiday findHolidayCovering(List<Holiday> holidays, LocalDate date) {
        if (date == null) return null;
        for (Holiday h : holidays) {
            if (h.covers(date)) {
                return h;
            }
        }
        return null;
    }
}
