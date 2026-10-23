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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S4-05 Lịch học của tôi, quyền SCHEDULE_READ.
 *
 * <pre>
 * GET /api/me/schedule   học viên: { cls, className, program, sessions } của lớp đang học (ghi danh ENROLLED mới nhất);
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
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT c.id, c.class_code, c.class_name, p.program_name "
                     + "FROM students s JOIN enrollments e ON e.student_id = s.id AND e.status = 'ENROLLED' "
                     + "JOIN classes c ON c.id = e.class_id LEFT JOIN programs p ON p.id = c.program_id "
                     + "WHERE s.user_id = ? ORDER BY e.enrollment_date DESC, e.id DESC LIMIT 1")) {
            ps.setLong(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.put("cls", rs.getString("class_code"));
                    out.put("className", rs.getString("class_name"));
                    out.put("program", rs.getString("program_name"));
                    out.put("sessions", sessions.findWhere("WHERE cs.class_id = ?", List.of(rs.getLong("id"))));
                }
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
