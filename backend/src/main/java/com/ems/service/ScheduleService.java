package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.model.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Service xử lý thuật toán sinh thời khoá biểu tự động theo mẫu lặp cho lớp học
 * Tác vụ Jira: IDTTX-247 / IDTTX-263 [BE] - S4-01
 * Tiêu chí nghiệm thu:
 *   1. Khai báo mẫu: thứ trong tuần, ca học, ngày bắt đầu, số buổi (ví dụ 60 buổi)
 *   2. Tự động bỏ qua các ngày nghỉ lễ đã khai báo và sinh đủ số lượng buổi
 *   3. Gắn từng buổi với bài học tương ứng theo thứ tự môn trong chương trình đào tạo của lớp
 *   4. Lấy đúng giảng viên được phân công cho từng môn
 *   5. Xem trước danh sách buổi và xác nhận lưu thời khoá biểu (ghi vào class_sessions)
 *
 * Người thực hiện: Nguyễn Trung Kiên (NK) - dtc245200736@ictu.edu.vn
 */
public class ScheduleService {

    private static class LessonSlot {
        String subjectCode;
        int lesson;
        String topic;
        String teacher;

        LessonSlot(String subjectCode, int lesson, String topic, String teacher) {
            this.subjectCode = subjectCode;
            this.lesson = lesson;
            this.topic = topic;
            this.teacher = teacher;
        }
    }

    /**
     * Sinh thời khoá biểu tự động theo mẫu lặp, tự động bỏ qua ngày nghỉ lễ
     */
    public ScheduleGenerationResult generateSchedule(SchedulePattern pattern) {
        validatePattern(pattern);

        Klass klass = ClassStore.findByCode(pattern.getClassCode());
        if (klass == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin lớp học có mã: " + pattern.getClassCode());
        }
        if (klass.getSubjects() == null || klass.getSubjects().isEmpty()) {
            throw new IllegalArgumentException("Lớp " + klass.getCode() + " chưa có môn học, hãy mở lớp từ chương trình đào tạo trước.");
        }

        // 1. Chuẩn bị danh sách bài học tuần tự từ các môn của lớp
        List<LessonSlot> slots = new ArrayList<>();
        for (ClassSubject s : klass.getSubjects()) {
            Map<Integer, String> topics = ClassStore.getLessonTopics(s.getSubjectId());
            String teacher = s.getTeacher() != null ? s.getTeacher() : "Chưa phân công";
            for (int i = 1; i <= s.getSessions(); i++) {
                slots.add(new LessonSlot(s.getCode(), i, topics.getOrDefault(i, "Bài học số " + i), teacher));
            }
        }
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("Các môn của lớp " + klass.getCode() + " chưa có số buổi.");
        }

        // 2. Thuật toán duyệt ngày và sinh lịch tự động
        List<Holiday> holidays = HolidayStore.getAllHolidays();
        LocalDate current = LocalDate.parse(pattern.getFrom());
        List<PlannedSession> sessions = new ArrayList<>();
        List<SkippedDate> skipped = new ArrayList<>();

        int guard = 0;
        int targetCount = pattern.getCount();

        while (sessions.size() < targetCount && guard++ < 3000) {
            // Trong Java: Thứ 2 = 1, ..., Thứ 7 = 6, Chủ nhật = 7. Quy đổi: Chủ nhật = 0
            int dow = current.getDayOfWeek().getValue() % 7;

            // Kiểm tra xem ngày hiện tại có phải thứ được chọn hay không
            if (!pattern.hasDay(dow)) {
                current = current.plusDays(1);
                continue;
            }

            // Kiểm tra ngày nghỉ lễ
            Holiday holiday = HolidayStore.findHolidayCovering(holidays, current);
            if (holiday != null) {
                // Tự động bỏ qua ngày nghỉ lễ, ghi nhận lý do và không tính vào số buổi đã sinh
                skipped.add(new SkippedDate(current.toString(), holiday.getName()));
                current = current.plusDays(1);
                continue;
            }

            // Sinh buổi học hợp lệ
            int sessionIndex = sessions.size();
            LessonSlot slot = (sessionIndex < slots.size()) ? slots.get(sessionIndex) : slots.get(slots.size() - 1);
            int sessionNo = sessionIndex + 1;
            String sessionId = pattern.getClassCode() + "-" + sessionNo;

            PlannedSession session = new PlannedSession(
                    sessionId,
                    pattern.getClassCode(),
                    sessionNo,
                    current.toString(),
                    pattern.getStart(),
                    pattern.getEnd(),
                    slot.subjectCode,
                    slot.lesson,
                    slot.topic,
                    slot.teacher,
                    pattern.getRoom()
            );

            sessions.add(session);
            current = current.plusDays(1);
        }

        return new ScheduleGenerationResult(klass, pattern, sessions, skipped);
    }

    /**
     * Lưu thời khoá biểu chính thức vào class_sessions sau khi Quản lý đào tạo xem trước và xác nhận.
     * Lưu lại thì thay lịch cũ, trừ khi lớp đã có buổi học xong, huỷ hoặc dời (khi đó sửa từng buổi ở S4-03).
     */
    public void saveSchedule(String classCode, List<PlannedSession> sessions, SchedulePattern pattern) {
        if (classCode == null || classCode.isBlank()) {
            throw new IllegalArgumentException("Mã lớp không được để trống khi lưu.");
        }
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("Danh sách buổi học không được rỗng.");
        }
        Klass klass = ClassStore.findByCode(classCode);
        if (klass == null) {
            throw new IllegalArgumentException("Không tìm thấy thông tin lớp học có mã: " + classCode);
        }

        Map<String, ClassSubject> subjectsByCode = new HashMap<>();
        for (ClassSubject s : klass.getSubjects()) {
            subjectsByCode.put(s.getCode(), s);
        }
        for (PlannedSession ps : sessions) {
            ClassSubject s = subjectsByCode.get(ps.getSubject());
            if (s == null) {
                throw new IllegalArgumentException("Môn " + ps.getSubject() + " không thuộc lớp " + klass.getCode() + ".");
            }
            if (s.getInstructorId() == null) {
                throw new IllegalArgumentException("Môn " + s.getName() + " chưa phân công giảng viên, hãy phân công trước khi lưu lịch.");
            }
        }

        String roomCode = pattern != null && pattern.getRoom() != null && !pattern.getRoom().isBlank()
                ? pattern.getRoom().trim() : klass.getRoom();
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng học cho lớp " + klass.getCode() + ".");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long roomId = findRoomId(conn, roomCode);
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM class_sessions WHERE class_id = ? AND status <> 'SCHEDULED'")) {
                    ps.setLong(1, klass.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            throw new IllegalArgumentException("Lớp " + klass.getCode()
                                    + " đã có buổi học xong, huỷ hoặc dời. Hãy sửa từng buổi thay vì sinh lại cả lịch.");
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM class_sessions WHERE class_id = ?")) {
                    ps.setLong(1, klass.getId());
                    ps.executeUpdate();
                }
                Map<Long, Map<Integer, Long>> lessonIds = new HashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO class_sessions (class_id, class_subject_id, subject_lesson_id, session_number, "
                                + "session_date, start_time, end_time, classroom_id, instructor_id, status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SCHEDULED')")) {
                    for (PlannedSession s : sessions) {
                        ClassSubject subject = subjectsByCode.get(s.getSubject());
                        Long lessonId = lessonIds.computeIfAbsent(subject.getSubjectId(), id -> findLessonIds(conn, id))
                                .get(s.getLesson());
                        ps.setLong(1, klass.getId());
                        ps.setLong(2, subject.getClassSubjectId());
                        ps.setObject(3, lessonId);
                        ps.setInt(4, s.getLesson());
                        ps.setDate(5, Date.valueOf(LocalDate.parse(s.getDate())));
                        ps.setTime(6, Time.valueOf(LocalTime.parse(s.getStart())));
                        ps.setTime(7, Time.valueOf(LocalTime.parse(s.getEnd())));
                        ps.setLong(8, roomId);
                        ps.setLong(9, subject.getInstructorId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không lưu được thời khoá biểu lớp " + klass.getCode() + ": " + e.getMessage(), e);
        }
    }

    public List<PlannedSession> getSessionsByClass(String classCode) {
        List<PlannedSession> list = new ArrayList<>();
        if (classCode == null || classCode.isBlank()) return list;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT x.session_number, x.session_date, x.start_time, x.end_time, x.status, "
                             + "s.subject_code, sl.topic, u.full_name, r.room_code "
                             + "FROM class_sessions x JOIN classes c ON c.id = x.class_id "
                             + "LEFT JOIN class_subjects cs ON cs.id = x.class_subject_id "
                             + "LEFT JOIN subjects s ON s.id = cs.subject_id "
                             + "LEFT JOIN subject_lessons sl ON sl.id = x.subject_lesson_id "
                             + "LEFT JOIN users u ON u.id = x.instructor_id "
                             + "LEFT JOIN classrooms r ON r.id = x.classroom_id "
                             + "WHERE c.class_code = ? ORDER BY x.session_date, x.start_time, x.id")) {
            String code = classCode.trim().toUpperCase();
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int no = list.size() + 1;
                    int lesson = rs.getInt("session_number");
                    String topic = rs.getString("topic");
                    PlannedSession s = new PlannedSession(code + "-" + no, code, no,
                            rs.getDate("session_date").toLocalDate().toString(),
                            rs.getTime("start_time").toLocalTime().toString(),
                            rs.getTime("end_time").toLocalTime().toString(),
                            rs.getString("subject_code"), lesson,
                            topic != null ? topic : "Bài học số " + lesson,
                            rs.getString("full_name"), rs.getString("room_code"));
                    s.setStatus(rs.getString("status"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được lịch học lớp " + classCode + ": " + e.getMessage(), e);
        }
        return list;
    }

    public boolean hasSchedule(String classCode) {
        return !getSessionsByClass(classCode).isEmpty();
    }

    static String shiftName(LocalTime t) {
        if (t.isBefore(LocalTime.of(12, 0))) return "Sáng";
        if (t.isBefore(LocalTime.of(17, 0))) return "Chiều";
        return "Tối";
    }

    private static long findRoomId(Connection conn, String roomCode) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM classrooms WHERE room_code = ?")) {
            ps.setString(1, roomCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new IllegalArgumentException("Không tìm thấy phòng học " + roomCode + ".");
    }

    private static Map<Integer, Long> findLessonIds(Connection conn, long subjectId) {
        Map<Integer, Long> ids = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, session_order FROM subject_lessons WHERE subject_id = ?")) {
            ps.setLong(1, subjectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.put(rs.getInt("session_order"), rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được giáo trình môn: " + e.getMessage(), e);
        }
        return ids;
    }

    private void validatePattern(SchedulePattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Thông tin mẫu lặp không được để trống.");
        }
        if (pattern.getClassCode() == null || pattern.getClassCode().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn lớp học cần sinh thời khoá biểu.");
        }
        if (pattern.getDays() == null || pattern.getDays().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một thứ trong tuần.");
        }
        if (pattern.getFrom() == null || pattern.getFrom().isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu khai giảng.");
        }
        try {
            LocalDate.parse(pattern.getFrom());
        } catch (Exception e) {
            throw new IllegalArgumentException("Ngày bắt đầu không đúng định dạng YYYY-MM-DD.");
        }
        if (pattern.getStart() == null || pattern.getStart().isBlank() ||
                pattern.getEnd() == null || pattern.getEnd().isBlank()) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc ca học không được để trống.");
        }
        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(pattern.getStart());
            endTime = LocalTime.parse(pattern.getEnd());
        } catch (Exception e) {
            throw new IllegalArgumentException("Giờ học không đúng định dạng HH:mm.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Giờ kết thúc ca học (" + pattern.getEnd() + ") phải sau giờ bắt đầu (" + pattern.getStart() + ").");
        }
        if (pattern.getCount() < 1 || pattern.getCount() > 300) {
            throw new IllegalArgumentException("Số buổi của lớp phải nằm trong khoảng từ 1 đến 300 buổi.");
        }
    }
}
