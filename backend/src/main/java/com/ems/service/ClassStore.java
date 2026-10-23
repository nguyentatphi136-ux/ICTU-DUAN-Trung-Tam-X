package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.model.ClassSubject;
import com.ems.model.Klass;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Đọc lớp, môn của lớp (thứ tự, giảng viên, trợ giảng), giáo trình từng buổi và phòng học từ các bảng của S3
 * (classes, class_subjects, subjects, subject_lessons, classrooms) để S4-01 sinh lịch trên dữ liệu thật.
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ClassStore {

    private static final String CLASS_SELECT = "SELECT c.id, c.class_code, c.class_name, c.start_date, c.max_capacity, "
            + "c.study_mode, c.status, p.program_name, r.room_code "
            + "FROM classes c LEFT JOIN programs p ON p.id = c.program_id "
            + "LEFT JOIN classrooms r ON r.id = c.default_classroom_id ";

    /** Lớp còn xếp lịch được: đang chuẩn bị hoặc đang học. */
    public static List<Klass> getAllClasses() {
        List<Klass> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CLASS_SELECT
                     + "WHERE c.status IN ('PLANNING', 'IN_PROGRESS') ORDER BY c.class_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(readClass(conn, rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách lớp: " + e.getMessage(), e);
        }
        return list;
    }

    public static Klass findByCode(String code) {
        if (code == null || code.isBlank()) return null;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(CLASS_SELECT + "WHERE c.class_code = ?")) {
            ps.setString(1, code.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readClass(conn, rs) : null;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được lớp " + code + ": " + e.getMessage(), e);
        }
    }

    /** Chủ đề từng buổi của môn theo giáo trình (subject_lessons.session_order -> topic). */
    public static Map<Integer, String> getLessonTopics(long subjectId) {
        Map<Integer, String> topics = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT session_order, topic FROM subject_lessons WHERE subject_id = ?")) {
            ps.setLong(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topics.put(rs.getInt("session_order"), rs.getString("topic"));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được giáo trình môn: " + e.getMessage(), e);
        }
        return topics;
    }

    public static List<String> getAvailableRooms() {
        List<String> rooms = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT room_code FROM classrooms WHERE status = 'AVAILABLE' ORDER BY room_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rooms.add(rs.getString("room_code"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách phòng học: " + e.getMessage(), e);
        }
        return rooms;
    }

    private static Klass readClass(Connection conn, ResultSet rs) throws SQLException {
        long classId = rs.getLong("id");
        Klass klass = new Klass(rs.getString("class_code"), rs.getString("program_name"),
                rs.getDate("start_date").toLocalDate().toString(), rs.getInt("max_capacity"),
                modeLabel(rs.getString("study_mode")), statusLabel(rs.getString("status")),
                "Chưa xếp lịch", rs.getString("room_code"), readSubjects(conn, classId));
        klass.setId(classId);
        klass.setName(rs.getString("class_name"));
        readSessionSummary(conn, klass);
        return klass;
    }

    private static List<ClassSubject> readSubjects(Connection conn, long classId) throws SQLException {
        List<ClassSubject> subjects = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT cs.id, cs.subject_id, cs.primary_instructor_id, s.subject_code, s.subject_name, s.total_sessions, "
                        + "u.full_name FROM class_subjects cs JOIN subjects s ON s.id = cs.subject_id "
                        + "LEFT JOIN users u ON u.id = cs.primary_instructor_id "
                        + "WHERE cs.class_id = ? ORDER BY cs.order_index, cs.id")) {
            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassSubject s = new ClassSubject(rs.getString("subject_code"), rs.getString("subject_name"),
                            rs.getInt("total_sessions"), rs.getString("full_name"));
                    s.setClassSubjectId(rs.getLong("id"));
                    s.setSubjectId(rs.getLong("subject_id"));
                    long instructorId = rs.getLong("primary_instructor_id");
                    s.setInstructorId(rs.wasNull() ? null : instructorId);
                    subjects.add(s);
                }
            }
        }
        for (ClassSubject s : subjects) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT u.full_name FROM class_subject_assistants a JOIN users u ON u.id = a.ta_user_id "
                            + "WHERE a.class_subject_id = ? ORDER BY u.full_name")) {
                ps.setLong(1, s.getClassSubjectId());
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> tas = new ArrayList<>();
                    while (rs.next()) tas.add(rs.getString(1));
                    s.setTas(tas);
                }
            }
        }
        return subjects;
    }

    /** Số buổi đã học và khung giờ (ví dụ "Tối 2-4-6") suy ra từ lịch đã lưu của lớp. */
    private static void readSessionSummary(Connection conn, Klass klass) throws SQLException {
        TreeSet<Integer> days = new TreeSet<>();
        LocalTime firstStart = null;
        int done = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT session_date, start_time, status FROM class_sessions WHERE class_id = ? AND status <> 'CANCELLED' "
                        + "ORDER BY session_date, start_time")) {
            ps.setLong(1, klass.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("session_date").toLocalDate();
                    days.add(date.getDayOfWeek().getValue()); // Thứ 2 = 1 ... Chủ nhật = 7
                    if (firstStart == null) firstStart = rs.getTime("start_time").toLocalTime();
                    if ("COMPLETED".equals(rs.getString("status"))) done++;
                }
            }
        }
        klass.setDone(done);
        if (firstStart != null) {
            klass.setSlot(ScheduleService.shiftName(firstStart) + " "
                    + days.stream().map(d -> d == 7 ? "CN" : String.valueOf(d + 1)).collect(Collectors.joining("-")));
        }
    }

    private static String modeLabel(String mode) {
        if ("ONLINE".equals(mode)) return "Trực tuyến";
        if ("HYBRID".equals(mode)) return "Kết hợp";
        return "Trực tiếp";
    }

    private static String statusLabel(String status) {
        if (status == null) return "Chuẩn bị";
        return switch (status) {
            case "IN_PROGRESS" -> "Đang học";
            case "FINISHED" -> "Kết thúc";
            case "CANCELLED" -> "Huỷ";
            default -> "Chuẩn bị";
        };
    }
}
