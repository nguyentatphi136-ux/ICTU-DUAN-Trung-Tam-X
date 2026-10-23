package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.dto.ClassSessionResponse;
import com.ems.exception.ApiException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.User;

import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * S4-08 Đơn xin phép vắng (bảng absence_requests). Học viên gửi đơn cho buổi sắp tới của lớp mình đang học; giảng viên,
 * trợ giảng của lớp hoặc Quản lý đào tạo duyệt, từ chối (bắt buộc ghi lý do). Duyệt thì buổi đó của học viên là Vắng có
 * phép: buổi đã điểm danh thì sửa ngay, chưa điểm danh thì AttendanceDAO điền sẵn khi mở.
 * Tệp minh chứng lưu trong CSDL (proof_file, tối đa 5 MB), tên gốc ở proof_file_url. Gửi và duyệt đơn đều có thông báo.
 */
public class LeaveRequestDAO {
    private static final String STATUS_PENDING = "PENDING";
    public static final int MAX_PROOF = 5 * 1024 * 1024;

    private final ClassSessionDAO sessions = new ClassSessionDAO();
    private final Supplier<LocalDate> today;

    public LeaveRequestDAO() {
        this(LocalDate::now);
    }

    public LeaveRequestDAO(Supplier<LocalDate> today) {
        this.today = today;
    }

    /** Đơn của chính học viên đang đăng nhập. */
    public List<Map<String, Object>> mine(User user) throws SQLException {
        long studentId = studentIdOf(user);
        return list("WHERE r.student_id = ?", List.of(studentId));
    }

    /** Đơn của các lớp người duyệt dạy; Quản lý đào tạo thấy mọi đơn. */
    public List<Map<String, Object>> forReviewer(User user) throws SQLException {
        if (AttendanceDAO.isManager(user)) return list("", List.of());
        return list("WHERE " + AttendanceDAO.TEACHES, List.of(user.getId(), user.getId(), user.getId()));
    }

    /** Tệp minh chứng đã tải lên: tên gốc, kiểu MIME, nội dung. */
    public record Proof(String name, String type, byte[] data) {}

    /** proof có thể null (không đính kèm). */
    public Map<String, Object> create(User user, long sessionId, String reason, Proof proof) throws SQLException {
        long studentId = studentIdOf(user);
        if (reason == null || reason.trim().length() < 10) throw new ValidationException("Ghi rõ lý do, ít nhất 10 ký tự.");
        if (proof != null) {
            if (proof.name() == null || !proof.name().toLowerCase().matches(".*\\.(jpe?g|png|pdf)$")) {
                throw new ValidationException("Chỉ nhận tệp JPG, PNG hoặc PDF.");
            }
            if (proof.data().length > MAX_PROOF) throw new ValidationException("Tệp minh chứng tối đa 5 MB.");
        }
        ClassSessionResponse s = sessions.findById(sessionId);
        if (s == null) throw new ResourceNotFoundException("Không tìm thấy buổi học " + sessionId + ".");
        if ("CANCELLED".equals(s.getStatus())) throw new ValidationException("Buổi này đã huỷ.");
        if (LocalDate.parse(s.getSessionDate()).isBefore(today.get())) throw new ValidationException("Chỉ xin phép được buổi sắp tới.");

        // Học viên có trong danh sách lớp vào ngày học đó (đã tính bảo lưu, chuyển lớp, thôi học theo ngày hiệu lực).
        if (new StudentStatusDAO().getRosterOnDate(s.getClassCode(), s.getSessionDate()).stream().noneMatch(st -> st.getId() == studentId)) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Buổi này không thuộc lớp bạn đang học.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM absence_requests WHERE session_id = ? AND student_id = ? AND status <> 'REJECTED'")) {
                ps.setLong(1, sessionId);
                ps.setLong(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new ConflictException("LEAVE_DUPLICATE", "Bạn đã gửi đơn cho buổi này.");
                }
            }
            long id;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO absence_requests (student_id, session_id, reason, "
                    + "proof_file_url, proof_file, proof_file_type, status) VALUES (?, ?, ?, ?, ?, ?, 'PENDING')", Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, studentId);
                ps.setLong(2, sessionId);
                ps.setString(3, reason.trim());
                ps.setString(4, proof == null ? null : proof.name());
                ps.setBytes(5, proof == null ? null : proof.data());
                // Kiểu tệp theo đuôi đã kiểm tra, không tin kiểu trình duyệt gửi lên (tránh tệp HTML mở ra chạy script).
                String name = proof == null ? "" : proof.name().toLowerCase();
                ps.setString(6, proof == null ? null : name.endsWith(".pdf") ? "application/pdf" : name.endsWith(".png") ? "image/png" : "image/jpeg");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
            }
            NotificationDAO.send(conn, NotificationDAO.teachersOf(conn, sessionId), user.getId(), "ABSENCE",
                    user.getFullName() + " xin phép vắng",
                    "Buổi " + s.getSeq() + " lớp " + s.getClassCode() + " ngày " + AttendanceDAO.viDate(s.getSessionDate())
                            + ". Lý do: " + reason.trim(), "/duyet-phep");
            return list("WHERE r.id = ?", List.of(id)).get(0);
        }
    }

    /** Tệp minh chứng của đơn: học viên gửi đơn, người dạy lớp hoặc Quản lý đào tạo xem được. */
    public Proof proof(User user, long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT r.session_id, r.proof_file_url, r.proof_file_type, r.proof_file, s.user_id "
                     + "FROM absence_requests r JOIN students s ON s.id = r.student_id WHERE r.id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getBytes("proof_file") == null) throw new ResourceNotFoundException("Đơn " + id + " không có tệp minh chứng.");
                if (rs.getLong("user_id") != user.getId() && !AttendanceDAO.isManager(user)
                        && !new AttendanceDAO().teaches(user.getId(), rs.getLong("session_id"))) {
                    throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Bạn không xem được tệp của đơn này.");
                }
                return new Proof(rs.getString("proof_file_url"), rs.getString("proof_file_type"), rs.getBytes("proof_file"));
            }
        }
    }

    /** approve = true duyệt, false từ chối (note bắt buộc). */
    public Map<String, Object> decide(User user, long id, boolean approve, String note) throws SQLException {
        if (!approve && (note == null || note.isBlank())) throw new ValidationException("Nhập lý do từ chối để học viên biết.");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long sessionId;
                long studentId;
                try (PreparedStatement ps = conn.prepareStatement("SELECT session_id, student_id, status FROM absence_requests WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ResourceNotFoundException("Không tìm thấy đơn " + id + ".");
                        if (!STATUS_PENDING.equals(rs.getString("status"))) {
                            throw new ConflictException("LEAVE_DECIDED", "Đơn này đã được xử lý.");
                        }
                        sessionId = rs.getLong("session_id");
                        studentId = rs.getLong("student_id");
                    }
                }
                if (!AttendanceDAO.isManager(user) && !new AttendanceDAO().teaches(user.getId(), sessionId)) {
                    throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Bạn không dạy lớp của buổi học này.");
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE absence_requests SET status = ?, reviewed_by = ?, "
                        + "review_note = ?, reviewed_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    ps.setString(1, approve ? "APPROVED" : "REJECTED");
                    ps.setLong(2, user.getId());
                    ps.setString(3, note == null || note.isBlank() ? null : note.trim());
                    ps.setLong(4, id);
                    ps.executeUpdate();
                }
                if (approve) excuse(conn, user, sessionId, studentId);
                ClassSessionResponse s = sessions.findById(sessionId);
                String what = "Buổi " + s.getSeq() + " lớp " + s.getClassCode() + " ngày " + AttendanceDAO.viDate(s.getSessionDate()) + ".";
                NotificationDAO.send(conn, List.of(studentUserId(conn, studentId)), user.getId(), "ABSENCE",
                        approve ? "Đơn xin phép đã được duyệt" : "Đơn xin phép bị từ chối",
                        what + (approve ? " Buổi này ghi Vắng có phép." : " Lý do: " + note.trim()), "/xin-phep");
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                throw e;
            }
            return list("WHERE r.id = ?", List.of(id)).get(0);
        }
    }

    /** Buổi đã có bảng điểm danh: ghi Vắng có phép cho học viên và ghi nhật ký. */
    private void excuse(Connection conn, User user, long sessionId, long studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM attendance WHERE session_id = ?")) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) return;
            }
        }
        int updated;
        try (PreparedStatement ps = conn.prepareStatement("UPDATE attendance SET status = 'EXCUSED_ABSENCE', notes = 'Đơn xin phép đã duyệt', "
                + "updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE session_id = ? AND student_id = ?")) {
            ps.setLong(1, user.getId());
            ps.setLong(2, sessionId);
            ps.setLong(3, studentId);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO attendance (session_id, student_id, status, notes, marked_by, updated_by) "
                    + "VALUES (?, ?, 'EXCUSED_ABSENCE', 'Đơn xin phép đã duyệt', ?, ?)")) {
                ps.setLong(1, sessionId);
                ps.setLong(2, studentId);
                ps.setLong(3, user.getId());
                ps.setLong(4, user.getId());
                ps.executeUpdate();
            }
        }
        String name = "";
        try (PreparedStatement ps = conn.prepareStatement("SELECT u.full_name FROM students s JOIN users u ON u.id = s.user_id WHERE s.id = ?")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) name = rs.getString(1);
            }
        }
        AttendanceDAO.log(conn, user, sessionId, name + ": vắng có phép theo đơn xin phép đã duyệt");
    }

    private List<Map<String, Object>> list(String where, List<Object> params) throws SQLException {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Long> sessionIds = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT r.id, r.session_id, r.reason, r.proof_file_url, OCTET_LENGTH(r.proof_file) AS file_size, r.status, r.review_note, r.reviewed_at, r.created_at, "
                             + "s.id AS student_id, s.student_code, u.full_name, rv.full_name AS reviewer, cs.class_id, "
                             + "(SELECT COUNT(*) FROM attendance a JOIN class_sessions x ON x.id = a.session_id "
                             + " WHERE a.student_id = s.id AND x.class_id = cs.class_id AND a.status = 'EXCUSED_ABSENCE') AS excused, "
                             + "(SELECT COUNT(*) FROM attendance a JOIN class_sessions x ON x.id = a.session_id "
                             + " WHERE a.student_id = s.id AND x.class_id = cs.class_id AND a.status = 'UNEXCUSED_ABSENCE') AS unexcused "
                             + "FROM absence_requests r JOIN class_sessions cs ON cs.id = r.session_id "
                             + "JOIN students s ON s.id = r.student_id JOIN users u ON u.id = s.user_id "
                             + "LEFT JOIN users rv ON rv.id = r.reviewed_by " + where + " ORDER BY r.created_at DESC, r.id DESC")) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("sessionId", rs.getLong("session_id"));
                    row.put("studentCode", rs.getString("student_code"));
                    row.put("studentName", rs.getString("full_name"));
                    row.put("reason", rs.getString("reason"));
                    row.put("fileName", rs.getString("proof_file_url"));
                    row.put("fileSize", rs.getLong("file_size"));
                    row.put("sentAt", minute(rs.getTimestamp("created_at")));
                    row.put("status", rs.getString("status"));
                    row.put("decidedBy", rs.getString("reviewer"));
                    row.put("decidedAt", minute(rs.getTimestamp("reviewed_at")));
                    row.put("note", rs.getString("review_note"));
                    row.put("excused", rs.getInt("excused"));
                    row.put("unexcused", rs.getInt("unexcused"));
                    out.add(row);
                    sessionIds.add(rs.getLong("session_id"));
                }
            }
        }
        if (!out.isEmpty()) {
            Map<Long, ClassSessionResponse> byId = new HashMap<>();
            String in = String.join(",", sessionIds.stream().distinct().map(x -> "?").toList());
            for (ClassSessionResponse s : sessions.findWhere("WHERE cs.id IN (" + in + ")", new ArrayList<>(sessionIds.stream().distinct().toList()))) {
                byId.put(s.getId(), s);
            }
            for (Map<String, Object> row : out) row.put("session", byId.get((Long) row.get("sessionId")));
        }
        return out;
    }

    private static long studentUserId(Connection conn, long studentId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM students WHERE id = ?")) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long studentIdOf(User user) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM students WHERE user_id = ?")) {
            ps.setLong(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Tài khoản này không phải học viên.");
    }

    private static String minute(Timestamp t) {
        return t == null ? null : t.toLocalDateTime().toString().substring(0, 16);
    }
}
