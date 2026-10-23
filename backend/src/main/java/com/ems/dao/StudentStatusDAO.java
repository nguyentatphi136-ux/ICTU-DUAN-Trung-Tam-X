package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.ClassItem;
import com.ems.model.PastClass;
import com.ems.model.Student;
import com.ems.model.StudentStatusLog;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Data Access Object cho nghiệp vụ S4-06:
 * Xử lý Bảo lưu, Chuyển lớp, Thôi học và Danh sách điểm danh theo ngày (Roster on Date).
 * Đọc ghi students, enrollments, classes của S3 và nhật ký student_status_logs (S4TimetableMigration tạo bảng).
 */
public class StudentStatusDAO {

    public static final String SUSPEND = "Bảo lưu";
    public static final String TRANSFER = "Chuyển lớp";
    public static final String DROP_OUT = "Thôi học";

    /** Lớp đang học hiện tại của học viên: ghi danh ENROLLED mới nhất. */
    private static final String CURRENT_CLASS = "(SELECT c.id FROM enrollments e JOIN classes c ON c.id = e.class_id "
            + "WHERE e.student_id = s.id AND e.status = 'ENROLLED' ORDER BY e.enrollment_date DESC, e.id DESC LIMIT 1)";

    private static final String STUDENT_SELECT = "SELECT s.id AS student_id, s.student_code, s.enrollment_status, s.entry_level, "
            + "s.notes, s.created_at, u.full_name, u.phone, u.email, u.date_of_birth, u.address, "
            + "cc.class_code, p.program_name "
            + "FROM students s JOIN users u ON s.user_id = u.id "
            + "LEFT JOIN classes cc ON cc.id = " + CURRENT_CLASS + " "
            + "LEFT JOIN programs p ON p.id = cc.program_id ";

    /**
     * Lấy danh sách toàn bộ học viên theo bộ lọc
     */
    public List<Student> getAllStudents(String search, String status, String classCode, String program) {
        StringBuilder sql = new StringBuilder(STUDENT_SELECT).append("WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append("AND (s.student_code LIKE ? OR u.full_name LIKE ? OR u.phone LIKE ?) ");
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            // Bộ lọc trên màn hình gửi nhãn tiếng Việt, cột lưu mã STUDYING/SUSPENDED/...
            sql.append("AND s.enrollment_status = ? ");
            params.add(status.equals(status.toUpperCase()) ? status : mapStatusToDb(status));
        }
        if (classCode != null && !classCode.isBlank() && !"ALL".equalsIgnoreCase(classCode)) {
            sql.append("AND cc.class_code = ? ");
            params.add(classCode);
        }
        if (program != null && !program.isBlank() && !"ALL".equalsIgnoreCase(program)) {
            sql.append("AND p.program_name = ? ");
            params.add(program);
        }
        sql.append("ORDER BY s.student_code DESC");

        List<Student> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readStudent(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách học viên: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Tìm học viên theo mã học viên, kèm lớp đã học và nhật ký biến động
     */
    public Student getStudentByCode(String code) {
        if (code == null || code.isBlank()) return null;
        try (Connection conn = DBConnection.getConnection()) {
            Student st;
            try (PreparedStatement ps = conn.prepareStatement(STUDENT_SELECT + "WHERE s.student_code = ?")) {
                ps.setString(1, code.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    st = readStudent(rs);
                }
            }
            st.setHistory(readHistory(conn, st.getId()));
            st.setStatusChanges(readLogs(conn, st.getCode()));
            return st;
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được hồ sơ học viên " + code + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lấy các lớp hợp lệ để chuyển đến (S4-06):
     * Cùng chương trình đào tạo, đang mở/đang học và còn chỗ trống.
     */
    public List<ClassItem> getAvailableClassesForTransfer(String currentClassCode, String program) {
        List<ClassItem> available = new ArrayList<>();
        for (ClassItem c : getAllClasses()) {
            boolean isSameProgram = program == null || program.isBlank() || program.equalsIgnoreCase(c.getProgram());
            boolean isDifferentClass = currentClassCode == null || !c.getCode().equalsIgnoreCase(currentClassCode);
            if (isSameProgram && isDifferentClass && c.isOpen() && c.hasAvailableSeats()) {
                available.add(c);
            }
        }
        return available;
    }

    /**
     * Nghiệp vụ cốt lõi S4-06: Bảo lưu, Chuyển lớp, Thôi học.
     * Trong một giao dịch: đổi trạng thái ghi danh ở lớp cũ (và ghi danh lớp mới khi chuyển lớp),
     * đổi tình trạng học viên, ghi nhật ký. Dữ liệu sai báo IllegalArgumentException, không ghi gì.
     */
    public void processStatusChange(String studentCode, String actionType, String effectiveDate,
                                    String reason, String notes, String toClassCode,
                                    String actorName, Long actorId) {
        if (!SUSPEND.equals(actionType) && !TRANSFER.equals(actionType) && !DROP_OUT.equals(actionType)) {
            throw new IllegalArgumentException("Thao tác không hợp lệ: " + actionType);
        }
        LocalDate effective;
        try {
            effective = LocalDate.parse(effectiveDate);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Ngày có hiệu lực không đúng định dạng YYYY-MM-DD.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng chọn lý do thực hiện!");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long studentId;
                String enrollmentStatus;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, enrollment_status FROM students WHERE student_code = ?")) {
                    ps.setString(1, studentCode);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy học viên có mã: " + studentCode);
                        studentId = rs.getLong("id");
                        enrollmentStatus = rs.getString("enrollment_status");
                    }
                }
                if ("DROPPED_OUT".equals(enrollmentStatus)) {
                    throw new IllegalArgumentException("Học viên " + studentCode + " đã thôi học.");
                }

                // Ghi danh đang học hiện tại (id, class_id, class_code, program_id)
                Long enrollmentId = null;
                long fromClassId = 0;
                String fromClass = null;
                int fromProgram = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT e.id, c.id AS class_id, c.class_code, c.program_id FROM enrollments e JOIN classes c ON c.id = e.class_id "
                                + "WHERE e.student_id = ? AND e.status = 'ENROLLED' ORDER BY e.enrollment_date DESC, e.id DESC LIMIT 1")) {
                    ps.setLong(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            enrollmentId = rs.getLong("id");
                            fromClassId = rs.getLong("class_id");
                            fromClass = rs.getString("class_code");
                            fromProgram = rs.getInt("program_id");
                        }
                    }
                }
                if (enrollmentId == null && !DROP_OUT.equals(actionType)) {
                    throw new IllegalArgumentException("Học viên " + studentCode + " hiện không học lớp nào.");
                }

                String newStudentStatus;
                if (TRANSFER.equals(actionType)) {
                    if (toClassCode == null || toClassCode.isBlank()) {
                        throw new IllegalArgumentException("Vui lòng chọn lớp học phù hợp để chuyển đến!");
                    }
                    long toClassId = checkTransferTarget(conn, toClassCode, fromClassId, fromProgram);
                    updateEnrollment(conn, enrollmentId, "TRANSFERRED");
                    enrollTransfer(conn, toClassId, studentId, effective);
                    newStudentStatus = "STUDYING";
                } else if (SUSPEND.equals(actionType)) {
                    updateEnrollment(conn, enrollmentId, "SUSPENDED");
                    newStudentStatus = "SUSPENDED";
                } else {
                    if (enrollmentId != null) updateEnrollment(conn, enrollmentId, "DROPPED");
                    newStudentStatus = "DROPPED_OUT";
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE students SET enrollment_status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    ps.setString(1, newStudentStatus);
                    ps.setLong(2, studentId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO student_status_logs "
                        + "(student_code, action_type, from_class, to_class, effective_date, reason, notes, actor_name, actor_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, studentCode);
                    ps.setString(2, actionType);
                    ps.setString(3, fromClass);
                    ps.setString(4, TRANSFER.equals(actionType) ? toClassCode : null);
                    ps.setDate(5, java.sql.Date.valueOf(effective));
                    ps.setString(6, reason.trim());
                    ps.setString(7, notes != null && !notes.isBlank() ? notes.trim() : null);
                    ps.setString(8, actorName != null ? actorName : "Quản lý đào tạo");
                    ps.setObject(9, actorId);
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không lưu được thay đổi tình trạng học: " + e.getMessage(), e);
        }
    }

    /**
     * Quy tắc nghiệm thu S4-06 (Roster on Date):
     * Danh sách điểm danh của lớp vào một ngày cụ thể.
     * - Học viên rời lớp (do chuyển lớp, bảo lưu, thôi học) từ ngày hiệu lực (targetDate >= effectiveDate)
     *   sẽ KHÔNG còn xuất hiện trong danh sách điểm danh.
     * - Các buổi học trước ngày hiệu lực (targetDate < effectiveDate), học viên vẫn có trong danh sách và giữ nguyên điểm danh.
     * - Học viên chuyển đến lớp mới chỉ xuất hiện trong danh sách từ ngày hiệu lực (ngày ghi danh vào lớp mới).
     */
    public List<Student> getRosterOnDate(String classCode, String targetDate) {
        List<Student> roster = new ArrayList<>();
        if (classCode == null || targetDate == null) return roster;
        LocalDate date = LocalDate.parse(targetDate);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT s.id AS student_id, s.student_code, u.full_name, u.phone, u.email, e.status, e.enrollment_date, "
                             + "(SELECT MAX(l.effective_date) FROM student_status_logs l "
                             + " WHERE l.student_code = s.student_code AND l.from_class = c.class_code) AS left_date "
                             + "FROM enrollments e JOIN classes c ON c.id = e.class_id "
                             + "JOIN students s ON s.id = e.student_id JOIN users u ON u.id = s.user_id "
                             + "WHERE c.class_code = ? ORDER BY u.full_name")) {
            ps.setString(1, classCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (rs.getDate("enrollment_date").toLocalDate().isAfter(date)) continue;
                    String status = rs.getString("status");
                    java.sql.Date left = rs.getDate("left_date");
                    boolean present = "ENROLLED".equals(status) || "GRADUATED".equals(status)
                            || (left != null && date.isBefore(left.toLocalDate()));
                    if (!present) continue;
                    Student st = new Student();
                    st.setId(rs.getLong("student_id"));
                    st.setCode(rs.getString("student_code"));
                    st.setName(rs.getString("full_name"));
                    st.setPhone(rs.getString("phone"));
                    st.setEmail(rs.getString("email"));
                    st.setStatus(mapEnrollmentFromDb(status));
                    st.setCurrentClass(classCode);
                    roster.add(st);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách lớp " + classCode + ": " + e.getMessage(), e);
        }
        return roster;
    }

    /**
     * Lấy danh sách lịch sử biến động học vụ của một học viên
     */
    public List<StudentStatusLog> getStatusLogsForStudent(String studentCode) {
        try (Connection conn = DBConnection.getConnection()) {
            return readLogs(conn, studentCode);
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được nhật ký học viên " + studentCode + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lấy toàn bộ lịch sử biến động gần nhất trong hệ thống
     */
    public List<StudentStatusLog> getAllStatusLogs(int limit) {
        List<StudentStatusLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT l.*, u.full_name FROM student_status_logs l "
                             + "LEFT JOIN students s ON s.student_code = l.student_code "
                             + "LEFT JOIN users u ON u.id = s.user_id ORDER BY l.id DESC LIMIT ?")) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudentStatusLog log = readLog(rs);
                    log.setStudentName(rs.getString("full_name"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được nhật ký biến động: " + e.getMessage(), e);
        }
        return logs;
    }

    public List<ClassItem> getAllClasses() {
        List<ClassItem> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT c.id, c.class_code, c.class_name, c.max_capacity, c.status, c.start_date, p.program_name, "
                             + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id AND e.status = 'ENROLLED') AS enrolled "
                             + "FROM classes c LEFT JOIN programs p ON p.id = c.program_id ORDER BY c.class_code");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ClassItem c = new ClassItem(rs.getString("class_code"), rs.getString("class_name"),
                        rs.getString("program_name"), rs.getInt("enrolled"), rs.getInt("max_capacity"),
                        rs.getString("status"), rs.getDate("start_date").toLocalDate().toString());
                c.setId(rs.getLong("id"));
                list.add(c);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Không đọc được danh sách lớp: " + e.getMessage(), e);
        }
        return list;
    }

    /** Lớp chuyển đến phải khác lớp cũ, cùng chương trình, đang mở và còn chỗ. Trả về id lớp. */
    private long checkTransferTarget(Connection conn, String toClassCode, long fromClassId, int fromProgram) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.id, c.program_id, c.status, c.max_capacity, "
                        + "(SELECT COUNT(*) FROM enrollments e WHERE e.class_id = c.id AND e.status = 'ENROLLED') AS enrolled "
                        + "FROM classes c WHERE c.class_code = ?")) {
            ps.setString(1, toClassCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy lớp " + toClassCode + ".");
                long id = rs.getLong("id");
                String status = rs.getString("status");
                if (id == fromClassId) throw new IllegalArgumentException("Học viên đang học lớp " + toClassCode + ".");
                if (rs.getInt("program_id") != fromProgram) {
                    throw new IllegalArgumentException("Lớp " + toClassCode + " không cùng chương trình đào tạo.");
                }
                if (!"PLANNING".equals(status) && !"IN_PROGRESS".equals(status)) {
                    throw new IllegalArgumentException("Lớp " + toClassCode + " không còn mở.");
                }
                if (rs.getInt("enrolled") >= rs.getInt("max_capacity")) {
                    throw new IllegalArgumentException("Lớp " + toClassCode + " đã đủ sĩ số.");
                }
                return id;
            }
        }
    }

    private void updateEnrollment(Connection conn, long enrollmentId, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE enrollments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, enrollmentId);
            ps.executeUpdate();
        }
    }

    /** Ghi danh lớp mới từ ngày hiệu lực; học viên từng ở lớp đó (mỗi cặp lớp-học viên một dòng) thì mở lại dòng cũ. */
    private void enrollTransfer(Connection conn, long classId, long studentId, LocalDate effective) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE enrollments SET status = 'ENROLLED', enrollment_date = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE class_id = ? AND student_id = ?")) {
            ps.setDate(1, java.sql.Date.valueOf(effective));
            ps.setLong(2, classId);
            ps.setLong(3, studentId);
            if (ps.executeUpdate() > 0) return;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO enrollments (class_id, student_id, enrollment_date, status) VALUES (?, ?, ?, 'ENROLLED')")) {
            ps.setLong(1, classId);
            ps.setLong(2, studentId);
            ps.setDate(3, java.sql.Date.valueOf(effective));
            ps.executeUpdate();
        }
    }

    private Student readStudent(ResultSet rs) throws SQLException {
        Student st = new Student();
        st.setId(rs.getLong("student_id"));
        st.setCode(rs.getString("student_code"));
        st.setName(rs.getString("full_name"));
        st.setPhone(rs.getString("phone"));
        st.setEmail(rs.getString("email"));
        st.setLoginEmail(rs.getString("email"));
        java.sql.Date dob = rs.getDate("date_of_birth");
        st.setBirthDate(dob != null ? dob.toLocalDate().toString() : null);
        st.setAddress(rs.getString("address"));
        st.setLevel(rs.getString("entry_level"));
        st.setBackground(rs.getString("notes"));
        st.setProgram(rs.getString("program_name"));
        st.setStatus(mapStatusFromDb(rs.getString("enrollment_status")));
        st.setCurrentClass(rs.getString("class_code"));
        Timestamp created = rs.getTimestamp("created_at");
        st.setEnrolledAt(created != null ? created.toLocalDateTime().toLocalDate().toString() : null);
        return st;
    }

    /** Các lớp học viên đã rời hoặc đã học xong. */
    private List<PastClass> readHistory(Connection conn, long studentId) throws SQLException {
        List<PastClass> history = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT c.class_code, p.program_name, e.enrollment_date, e.updated_at, e.status "
                        + "FROM enrollments e JOIN classes c ON c.id = e.class_id LEFT JOIN programs p ON p.id = c.program_id "
                        + "WHERE e.student_id = ? AND e.status <> 'ENROLLED' ORDER BY e.enrollment_date DESC, e.id DESC")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate from = rs.getDate("enrollment_date").toLocalDate();
                    Timestamp to = rs.getTimestamp("updated_at");
                    String period = monthYear(from) + (to != null ? " – " + monthYear(to.toLocalDateTime().toLocalDate()) : "");
                    history.add(new PastClass(rs.getString("class_code"), rs.getString("program_name"), period,
                            mapEnrollmentFromDb(rs.getString("status"))));
                }
            }
        }
        return history;
    }

    private List<StudentStatusLog> readLogs(Connection conn, String studentCode) throws SQLException {
        List<StudentStatusLog> logs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM student_status_logs WHERE student_code = ? ORDER BY id DESC")) {
            ps.setString(1, studentCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(readLog(rs));
                }
            }
        }
        return logs;
    }

    private StudentStatusLog readLog(ResultSet rs) throws SQLException {
        StudentStatusLog log = new StudentStatusLog();
        log.setId(rs.getLong("id"));
        log.setStudentCode(rs.getString("student_code"));
        log.setActionType(rs.getString("action_type"));
        log.setFromClass(rs.getString("from_class"));
        log.setToClass(rs.getString("to_class"));
        log.setEffectiveDate(rs.getDate("effective_date").toLocalDate().toString());
        log.setReason(rs.getString("reason"));
        log.setNotes(rs.getString("notes"));
        log.setActorName(rs.getString("actor_name"));
        long actorId = rs.getLong("actor_id");
        log.setActorId(rs.wasNull() ? null : actorId);
        Timestamp created = rs.getTimestamp("created_at");
        log.setRecordedDate(created != null ? created.toLocalDateTime().toLocalDate().toString() : "");
        return log;
    }

    private static String monthYear(LocalDate d) {
        return String.format("%02d/%d", d.getMonthValue(), d.getYear());
    }

    private String mapEnrollmentFromDb(String status) {
        if (status == null) return "Đang học";
        return switch (status) {
            case "SUSPENDED" -> SUSPEND;
            case "TRANSFERRED" -> "Đã chuyển lớp";
            case "DROPPED" -> DROP_OUT;
            case "GRADUATED" -> "Hoàn thành";
            default -> "Đang học";
        };
    }

    private String mapStatusFromDb(String dbStatus) {
        if (dbStatus == null) return "Đang học";
        switch (dbStatus.toUpperCase()) {
            case "SUSPENDED":
                return SUSPEND;
            case "DROPPED_OUT":
            case "DROPPED":
                return DROP_OUT;
            case "GRADUATED":
                return "Đã tốt nghiệp";
            default:
                return "Đang học";
        }
    }

    private String mapStatusToDb(String vnStatus) {
        if (vnStatus == null) return "STUDYING";
        switch (vnStatus) {
            case SUSPEND:
                return "SUSPENDED";
            case DROP_OUT:
                return "DROPPED_OUT";
            case "Đã tốt nghiệp":
                return "GRADUATED";
            default:
                return "STUDYING";
        }
    }
}
