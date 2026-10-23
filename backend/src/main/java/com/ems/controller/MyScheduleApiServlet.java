package com.ems.controller;

import com.ems.config.DBConnection;
import com.ems.dao.AttendanceDAO;
import com.ems.dao.ClassSessionDAO;
import com.ems.exception.ApiException;
import com.ems.model.User;
import com.ems.security.ApiResponse;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S4-05 Lịch học của tôi, quyền SCHEDULE_READ.
 *
 * <pre>
 * GET /api/me/schedule   học viên: { cls, className, program, sessions } của lớp đang học (kể cả lớp sắp rời, chỉ các buổi trước ngày rời);
 *                        giảng viên, trợ giảng: { sessions } các buổi của lớp mình dạy
 * </pre>
 */
@WebServlet(name = "MyScheduleApiServlet", urlPatterns = {"/api/me/schedule"})
public class MyScheduleApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;

    private final transient ClassSessionDAO sessions = new ClassSessionDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (!"GET".equals(method) || !path.isEmpty()) {
            notFound(resp);
            return;
        }
        User user = currentUser(req);
        Map<String, Object> out = new LinkedHashMap<>();
        // Cùng quy tắc với StudentStatusDAO.getRosterOnDate: bảo lưu, chuyển lớp, thôi học có hiệu lực từ ngày ghi trong
        // student_status_logs, trước ngày đó học viên vẫn học lớp cũ. Ưu tiên lớp đang học hôm nay, sau đó lớp sắp vào học.
        LocalDate today = LocalDate.now();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT c.id, c.class_code, c.class_name, p.program_name, e.status, "
                     + "e.enrollment_date, (SELECT MAX(l.effective_date) FROM student_status_logs l "
                     + " WHERE l.student_code = s.student_code AND l.from_class = c.class_code) AS left_date "
                     + "FROM students s JOIN enrollments e ON e.student_id = s.id "
                     + "JOIN classes c ON c.id = e.class_id LEFT JOIN programs p ON p.id = c.program_id "
                     + "WHERE s.user_id = ? ORDER BY e.enrollment_date DESC, e.id DESC")) {
            ps.setLong(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Object> upcoming = null;
                while (rs.next()) {
                    java.sql.Date leftAt = rs.getDate("left_date");
                    LocalDate left = leftAt == null ? null : leftAt.toLocalDate();
                    boolean enrolled = "ENROLLED".equals(rs.getString("status"));
                    if (!enrolled && (left == null || !today.isBefore(left))) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("cls", rs.getString("class_code"));
                    row.put("className", rs.getString("class_name"));
                    row.put("program", rs.getString("program_name"));
                    row.put("sessions", enrolled
                            ? sessions.findWhere("WHERE cs.class_id = ?", List.of(rs.getLong("id")))
                            : sessions.findWhere("WHERE cs.class_id = ? AND cs.session_date < ?", List.of(rs.getLong("id"), java.sql.Date.valueOf(left))));
                    if (rs.getDate("enrollment_date").toLocalDate().isAfter(today)) {
                        if (upcoming == null) upcoming = row;
                    } else {
                        out.putAll(row);
                        break;
                    }
                }
                if (out.isEmpty() && upcoming != null) out.putAll(upcoming);
            }
        }
        if (!out.containsKey("sessions")) {
            out.put("sessions", sessions.findWhere("WHERE " + AttendanceDAO.TEACHES, List.of(user.getId(), user.getId(), user.getId())));
        }
        ApiResponse.success(resp, "MY_SCHEDULE_SUCCESS", "Lịch học của tôi.", out);
    }

    static User currentUser(HttpServletRequest req) {
        if (req.getSession(false) != null && req.getSession(false).getAttribute("currentUser") instanceof User u) return u;
        throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, "AUTH_UNAUTHORIZED", "Vui lòng đăng nhập lại.");
    }
}
