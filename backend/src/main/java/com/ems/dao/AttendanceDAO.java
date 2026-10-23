package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.dto.ClassSessionResponse;
import com.ems.exception.ApiException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.Student;
import com.ems.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * S4-07 Điểm danh từng buổi. Giảng viên, trợ giảng của lớp điểm danh buổi hôm nay; Quản lý đào tạo sửa được mọi buổi đã
 * diễn ra. Mỗi lần lưu ghi nhật ký vào audit_logs (entity_type ATTENDANCE, entity_id là id buổi).
 * Trạng thái lưu theo mã PRESENT, LATE, EXCUSED_ABSENCE, UNEXCUSED_ABSENCE.
 */
public class AttendanceDAO {
    public static final Set<String> MARKS = Set.of("PRESENT", "LATE", "EXCUSED_ABSENCE", "UNEXCUSED_ABSENCE");
    private static final Map<String, String> MARK_LABEL = Map.of("PRESENT", "có mặt", "LATE", "đi muộn",
            "EXCUSED_ABSENCE", "vắng có phép", "UNEXCUSED_ABSENCE", "vắng không phép");
    private static final Gson GSON = new Gson();

    /** Điều kiện buổi cs thuộc lớp người dùng dạy (giảng viên buổi đó, giảng viên chính hoặc trợ giảng một môn của lớp). 3 tham số userId. */
    public static final String TEACHES = "(cs.instructor_id = ? OR EXISTS (SELECT 1 FROM class_subjects t "
            + "LEFT JOIN class_subject_assistants a ON a.class_subject_id = t.id "
            + "WHERE t.class_id = cs.class_id AND (t.primary_instructor_id = ? OR a.ta_user_id = ?)))";

    private final ClassSessionDAO sessions = new ClassSessionDAO();
    private final StudentStatusDAO students = new StudentStatusDAO();
    private final Supplier<LocalDate> today;

    public AttendanceDAO() {
        this(LocalDate::now);
    }

    /** today cho kiểm thử quy tắc "chỉ điểm danh buổi hôm nay". */
    public AttendanceDAO(Supplier<LocalDate> today) {
        this.today = today;
    }

    public static boolean isManager(User user) {
        return user != null && user.getRoles() != null
                && (user.getRoles().contains("TRAINING_MANAGER") || user.getRoles().contains("ADMIN"));
    }

    /** Buổi user được xem điểm danh trong [from, to]: lớp mình dạy, hoặc mọi lớp nếu là quản lý. Kèm bảng điểm danh đã lưu. */
    public List<Map<String, Object>> visibleSessions(User user, LocalDate from, LocalDate to) throws SQLException {
        List<Object> params = new ArrayList<>(List.of(from.toString(), to.toString()));
        String where = "WHERE cs.session_date BETWEEN ? AND ? AND cs.status <> 'CANCELLED'";
        if (!isManager(user)) {
            where += " AND " + TEACHES;
            params.addAll(List.of(user.getId(), user.getId(), user.getId()));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            for (ClassSessionResponse s : sessions.findWhere(where, params)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("session", s);
                row.put("sheet", sheet(conn, s.getId()));
                out.add(row);
            }
        }
        return out;
    }

    /** Một buổi: thông tin buổi, danh sách học viên theo ngày, bảng đã lưu, nhật ký, học viên có đơn đã duyệt, có sửa được không. */
    public Map<String, Object> get(User user, long sessionId) throws SQLException {
        ClassSessionResponse s = sessionFor(user, sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("session", s);
        out.put("roster", roster(s));
        try (Connection conn = DBConnection.getConnection()) {
            out.put("sheet", sheet(conn, sessionId));
            out.put("logs", logs(conn, sessionId));
            out.put("approved", approvedLeaves(conn, sessionId));
        }
        out.put("canEdit", editable(user, s));
        return out;
    }

    /**
     * Lưu điểm danh. marks, notes theo mã học viên; mọi học viên trong danh sách phải có trạng thái.
     * Sửa bảng đã lưu của buổi không phải hôm nay phải có lý do.
     */
    public Map<String, Object> save(User user, long sessionId, Map<String, String> marks, Map<String, String> notes, String reason)
            throws SQLException {
        ClassSessionResponse s = sessionFor(user, sessionId);
        if ("CANCELLED".equals(s.getStatus())) throw new ValidationException("Buổi đã huỷ, không điểm danh.");
        LocalDate date = LocalDate.parse(s.getSessionDate());
        if (date.isAfter(today.get())) throw new ValidationException("Chưa tới ngày học, điểm danh mở vào ngày diễn ra buổi học.");
        if (!editable(user, s)) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "ATTENDANCE_LOCKED",
                    "Giảng viên chỉ điểm danh được buổi của ngày hôm nay. Sửa buổi quá khứ cần quyền Quản lý đào tạo.");
        }
        List<Student> roster = roster(s);
        Map<String, Student> byCode = new LinkedHashMap<>();
        for (Student st : roster) byCode.put(st.getCode(), st);
        for (String code : marks.keySet()) {
            if (!byCode.containsKey(code)) throw new ValidationException("Học viên " + code + " không thuộc danh sách buổi này.");
            if (!MARKS.contains(marks.get(code))) throw new ValidationException("Trạng thái điểm danh không hợp lệ: " + marks.get(code));
        }
        long unmarked = roster.stream().filter(st -> !marks.containsKey(st.getCode())).count();
        if (unmarked > 0) throw new ValidationException("Còn " + unmarked + " học viên chưa điểm danh.");

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Map<String, String> old = new LinkedHashMap<>();
                Map<String, Long> studentIds = new LinkedHashMap<>();
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT s.student_code, a.status FROM attendance a JOIN students s ON s.id = a.student_id WHERE a.session_id = ?")) {
                    ps.setLong(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) old.put(rs.getString(1), rs.getString(2));
                    }
                }
                List<String> changed = roster.stream().map(Student::getCode).filter(c -> !marks.get(c).equals(old.get(c))).toList();
                boolean first = old.isEmpty();
                if (!first && changed.isEmpty() && sameNotes(conn, sessionId, notes)) {
                    throw new ValidationException("Chưa có thay đổi nào.");
                }
                boolean pastEdit = !first && !date.equals(today.get());
                if (pastEdit && (reason == null || reason.isBlank())) {
                    throw new ValidationException("Nhập lý do sửa điểm danh của buổi đã qua.");
                }
                for (Student st : roster) studentIds.put(st.getCode(), st.getId());
                for (Student st : roster) {
                    String code = st.getCode();
                    String note = notes.get(code) == null || notes.get(code).isBlank() ? null : notes.get(code).trim();
                    int updated;
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE attendance SET status = ?, notes = ?, updated_by = ?, "
                            + "updated_at = CURRENT_TIMESTAMP WHERE session_id = ? AND student_id = ?")) {
                        ps.setString(1, marks.get(code));
                        ps.setString(2, note);
                        ps.setLong(3, user.getId());
                        ps.setLong(4, sessionId);
                        ps.setLong(5, studentIds.get(code));
                        updated = ps.executeUpdate();
                    }
                    if (updated == 0) {
                        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO attendance (session_id, student_id, status, notes, "
                                + "marked_by, updated_by) VALUES (?, ?, ?, ?, ?, ?)")) {
                            ps.setLong(1, sessionId);
                            ps.setLong(2, studentIds.get(code));
                            ps.setString(3, marks.get(code));
                            ps.setString(4, note);
                            ps.setLong(5, user.getId());
                            ps.setLong(6, user.getId());
                            ps.executeUpdate();
                        }
                    }
                }
                String why = reason == null || reason.isBlank() ? "" : ". Lý do: " + reason.trim();
                if (first) {
                    log(conn, user, sessionId, "Lưu điểm danh lần đầu: " + summary(marks.values()) + ".");
                } else {
                    for (String code : changed) {
                        log(conn, user, sessionId, byCode.get(code).getName() + ": " + label(old.get(code)) + " → " + label(marks.get(code)) + why);
                    }
                    if (changed.isEmpty()) log(conn, user, sessionId, "Sửa ghi chú điểm danh" + why);
                }
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            }
        }
        return get(user, sessionId);
    }

    /** Giảng viên, trợ giảng xin Quản lý đào tạo sửa điểm danh buổi đã qua (sau ngày học chỉ quản lý sửa được). */
    public void requestEdit(User user, long sessionId, String reason) throws SQLException {
        ClassSessionResponse s = sessionFor(user, sessionId);
        if (editable(user, s)) throw new ValidationException("Buổi này bạn đang sửa được, không cần gửi yêu cầu.");
        if (!LocalDate.parse(s.getSessionDate()).isBefore(today.get())) throw new ValidationException("Chỉ gửi yêu cầu cho buổi đã qua.");
        String why = reason == null || reason.isBlank() ? "" : " Lý do: " + reason.trim();
        try (Connection conn = DBConnection.getConnection()) {
            NotificationDAO.send(conn, NotificationDAO.managers(conn), user.getId(), "ABSENCE",
                    "Yêu cầu sửa điểm danh " + s.getClassCode() + " buổi " + s.getSeq(),
                    user.getFullName() + " đề nghị mở sửa điểm danh buổi ngày " + viDate(s.getSessionDate()) + "." + why,
                    "/diem-danh/" + sessionId);
        }
    }

    static String viDate(String iso) {
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    /** Buổi theo id; người không dạy lớp đó (và không phải quản lý) nhận 403. */
    ClassSessionResponse sessionFor(User user, long sessionId) throws SQLException {
        ClassSessionResponse s = sessions.findById(sessionId);
        if (s == null) throw new ResourceNotFoundException("Không tìm thấy buổi học " + sessionId + ".");
        if (!isManager(user) && !teaches(user.getId(), sessionId)) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Bạn không dạy lớp của buổi học này.");
        }
        return s;
    }

    boolean teaches(long userId, long sessionId) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM class_sessions cs WHERE cs.id = ? AND " + TEACHES)) {
            ps.setLong(1, sessionId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean editable(User user, ClassSessionResponse s) {
        if ("CANCELLED".equals(s.getStatus())) return false;
        LocalDate date = LocalDate.parse(s.getSessionDate());
        if (date.isAfter(today.get())) return false;
        return isManager(user) || date.equals(today.get());
    }

    private List<Student> roster(ClassSessionResponse s) {
        return students.getRosterOnDate(s.getClassCode(), s.getSessionDate());
    }

    /** Bảng đã lưu { savedAt, savedBy, marks, notes } hoặc null nếu buổi chưa điểm danh. */
    static Map<String, Object> sheet(Connection conn, long sessionId) throws SQLException {
        Map<String, String> marks = new LinkedHashMap<>();
        Map<String, String> notes = new LinkedHashMap<>();
        Timestamp last = null;
        String by = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT s.student_code, a.status, a.notes, COALESCE(a.updated_at, a.marked_at) AS at, u.full_name "
                        + "FROM attendance a JOIN students s ON s.id = a.student_id "
                        + "LEFT JOIN users u ON u.id = COALESCE(a.updated_by, a.marked_by) WHERE a.session_id = ?")) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    marks.put(rs.getString(1), rs.getString(2));
                    if (rs.getString(3) != null) notes.put(rs.getString(1), rs.getString(3));
                    Timestamp at = rs.getTimestamp("at");
                    if (at != null && (last == null || at.after(last))) {
                        last = at;
                        by = rs.getString("full_name");
                    }
                }
            }
        }
        if (marks.isEmpty()) return null;
        Map<String, Object> sheet = new LinkedHashMap<>();
        sheet.put("savedAt", last == null ? null : last.toLocalDateTime().toString().substring(0, 16));
        sheet.put("savedBy", by);
        sheet.put("marks", marks);
        sheet.put("notes", notes);
        return sheet;
    }

    private boolean sameNotes(Connection conn, long sessionId, Map<String, String> notes) throws SQLException {
        @SuppressWarnings("unchecked")
        Map<String, String> saved = (Map<String, String>) sheet(conn, sessionId).get("notes");
        for (Map.Entry<String, String> e : notes.entrySet()) {
            String n = e.getValue() == null || e.getValue().isBlank() ? null : e.getValue().trim();
            if (!java.util.Objects.equals(n, saved.get(e.getKey()))) return false;
        }
        return true;
    }

    private static List<Map<String, String>> logs(Connection conn, long sessionId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT l.created_at, l.new_values, u.full_name FROM audit_logs l "
                + "LEFT JOIN users u ON u.id = l.user_id WHERE l.entity_type = 'ATTENDANCE' AND l.entity_id = ? ORDER BY l.id DESC")) {
            ps.setString(1, String.valueOf(sessionId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject v = GSON.fromJson(rs.getString("new_values"), JsonObject.class);
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("at", rs.getTimestamp("created_at").toLocalDateTime().toString().substring(0, 16));
                    row.put("by", rs.getString("full_name"));
                    row.put("role", v != null && v.has("role") ? v.get("role").getAsString() : "");
                    row.put("text", v != null && v.has("text") ? v.get("text").getAsString() : "");
                    out.add(row);
                }
            }
        }
        return out;
    }

    private static List<String> approvedLeaves(Connection conn, long sessionId) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT s.student_code FROM absence_requests r JOIN students s ON s.id = r.student_id "
                + "WHERE r.session_id = ? AND r.status = 'APPROVED'")) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getString(1));
            }
        }
        return out;
    }

    static void log(Connection conn, User user, long sessionId, String text) throws SQLException {
        JsonObject v = new JsonObject();
        v.addProperty("text", text);
        v.addProperty("role", roleLabel(user));
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO audit_logs (user_id, action, entity_type, entity_id, new_values) "
                + "VALUES (?, 'ATTENDANCE_SAVE', 'ATTENDANCE', ?, ?)")) {
            ps.setLong(1, user.getId());
            ps.setString(2, String.valueOf(sessionId));
            ps.setString(3, GSON.toJson(v));
            ps.executeUpdate();
        }
    }

    static String roleLabel(User user) {
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles();
        if (roles.contains("TRAINING_MANAGER")) return "Quản lý đào tạo";
        if (roles.contains("INSTRUCTOR")) return "Giảng viên";
        if (roles.contains("TA")) return "Trợ giảng";
        if (roles.contains("ADMIN")) return "Quản trị";
        return "";
    }

    private static String label(String mark) {
        return mark == null ? "chưa có" : MARK_LABEL.getOrDefault(mark, mark);
    }

    private static String summary(java.util.Collection<String> marks) {
        StringBuilder sb = new StringBuilder();
        for (String m : List.of("PRESENT", "LATE", "EXCUSED_ABSENCE", "UNEXCUSED_ABSENCE")) {
            long n = marks.stream().filter(m::equals).count();
            if (n > 0) sb.append(sb.length() > 0 ? " · " : "").append(n).append(' ').append(MARK_LABEL.get(m));
        }
        return sb.toString();
    }
}
