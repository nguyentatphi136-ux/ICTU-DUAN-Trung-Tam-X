package com.ems.service;

import com.ems.dao.ClassSessionDAO;
import com.ems.dto.ClassSessionResponse;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service xử lý nghiệp vụ thời khóa biểu
 * EP-04: Xem thời khóa biểu dạng lịch tuần và tháng – S4-04
 */
public class ClassSessionService {

    private final ClassSessionDAO classSessionDAO;

    public ClassSessionService() {
        this.classSessionDAO = new ClassSessionDAO();
    }

    /** Constructor cho kiểm thử (dependency injection). */
    public ClassSessionService(ClassSessionDAO classSessionDAO) {
        this.classSessionDAO = classSessionDAO;
    }

    // -------------------------------------------------------------------------
    // A. Thời khóa biểu theo tuần
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách buổi học trong tuần chứa ngày {@code dateStr}.
     * Tuần bắt đầu từ Thứ Hai (ISO 8601).
     * Có thể kết hợp với bộ lọc classId / lecturerId / roomId (null = không lọc).
     */
    public List<ClassSessionResponse> getWeeklySchedule(
            String dateStr, Long classId, Long lecturerId, Integer roomId) throws SQLException {

        LocalDate pivotDate = parseDate(dateStr);
        LocalDate weekStart = pivotDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd   = pivotDate.with(DayOfWeek.SUNDAY);

        List<ClassSessionResponse> sessions = classSessionDAO.findByDateRange(
                weekStart, weekEnd, classId, lecturerId, roomId);

        // Đánh dấu xung đột
        Set<Long> conflictIds = classSessionDAO.findConflictingSessionIds(weekStart, weekEnd);
        sessions.forEach(s -> s.setHasConflict(conflictIds.contains(s.getId())));
        return sessions;
    }

    // -------------------------------------------------------------------------
    // B. Thời khóa biểu theo tháng
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách buổi học trong tháng {@code month} năm {@code year}.
     */
    public List<ClassSessionResponse> getMonthlySchedule(
            int year, int month, Long classId, Long lecturerId, Integer roomId) throws SQLException {

        if (month < 1 || month > 12) {
            throw new ValidationException("Tháng không hợp lệ: phải nằm trong khoảng 1-12.");
        }
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Năm không hợp lệ: phải nằm trong khoảng 2000-2100.");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd   = ym.atEndOfMonth();

        List<ClassSessionResponse> sessions = classSessionDAO.findByDateRange(
                monthStart, monthEnd, classId, lecturerId, roomId);

        Set<Long> conflictIds = classSessionDAO.findConflictingSessionIds(monthStart, monthEnd);
        sessions.forEach(s -> s.setHasConflict(conflictIds.contains(s.getId())));
        return sessions;
    }

    // -------------------------------------------------------------------------
    // C. Lọc theo khoảng ngày tùy chỉnh
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách buổi học trong khoảng [startDate, endDate].
     * Kết hợp với bộ lọc classId / lecturerId / roomId.
     */
    public List<ClassSessionResponse> getScheduleByRange(
            String startDateStr, String endDateStr,
            Long classId, Long lecturerId, Integer roomId) throws SQLException {

        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate   = parseDate(endDateStr);

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("endDate không được trước startDate.");
        }
        if (startDate.until(endDate).toTotalMonths() > 12) {
            throw new ValidationException("Khoảng thời gian truy vấn không được vượt quá 12 tháng.");
        }

        List<ClassSessionResponse> sessions = classSessionDAO.findByDateRange(
                startDate, endDate, classId, lecturerId, roomId);

        Set<Long> conflictIds = classSessionDAO.findConflictingSessionIds(startDate, endDate);
        sessions.forEach(s -> s.setHasConflict(conflictIds.contains(s.getId())));
        return sessions;
    }

    // -------------------------------------------------------------------------
    // D. Chi tiết buổi học
    // -------------------------------------------------------------------------

    /**
     * Lấy chi tiết buổi học theo ID; ném ResourceNotFoundException nếu không có.
     */
    public ClassSessionResponse getSessionById(long id) throws SQLException {
        ClassSessionResponse session = classSessionDAO.findById(id);
        if (session == null) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + id);
        }
        return session;
    }

    // -------------------------------------------------------------------------
    // E. Phát hiện xung đột lịch học
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách các cặp buổi học bị trùng trong khoảng [startDate, endDate].
     */
    public List<Map<String, Object>> detectConflicts(
            String startDateStr, String endDateStr) throws SQLException {

        LocalDate startDate = parseDate(startDateStr);
        LocalDate endDate   = parseDate(endDateStr);

        if (endDate.isBefore(startDate)) {
            throw new ValidationException("endDate không được trước startDate.");
        }
        return classSessionDAO.findConflicts(startDate, endDate);
    }

    // -------------------------------------------------------------------------
    // F. Danh sách bộ lọc (lớp, giảng viên, phòng học)
    // -------------------------------------------------------------------------

    public List<Map<String, Object>> getFilterClasses() throws SQLException {
        return classSessionDAO.listClasses();
    }

    public List<Map<String, Object>> getFilterInstructors() throws SQLException {
        return classSessionDAO.listInstructors();
    }

    public List<Map<String, Object>> getFilterClassrooms() throws SQLException {
        return classSessionDAO.listClassrooms();
    }

    // -------------------------------------------------------------------------
    // G. Tiện ích nội bộ
    // -------------------------------------------------------------------------

    /** Parse chuỗi ngày yyyy-MM-dd; ném ValidationException nếu sai định dạng. */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new ValidationException("Tham số ngày không được để trống.");
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Định dạng ngày không hợp lệ: '" + dateStr + "'. Vui lòng dùng định dạng yyyy-MM-dd.");
        }
    }
}
