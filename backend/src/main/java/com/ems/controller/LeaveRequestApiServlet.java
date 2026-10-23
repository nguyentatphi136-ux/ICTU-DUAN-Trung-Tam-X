package com.ems.controller;

import com.ems.dao.LeaveRequestDAO;
import com.ems.exception.ApiException;
import com.ems.exception.ValidationException;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S4-08 Đơn xin phép vắng. Học viên gửi và xem đơn của mình (ABSENCE_REQUEST); giảng viên, trợ giảng, Quản lý đào tạo
 * xem và duyệt đơn của lớp mình (ATTENDANCE_MANAGE). Trạng thái: PENDING, APPROVED, REJECTED.
 *
 * <pre>
 * GET   /api/leave-requests?scope=mine   đơn của tôi (học viên)
 * GET   /api/leave-requests              đơn của các lớp tôi dạy (quản lý: tất cả)
 * POST  /api/leave-requests              { sessionId, reason, fileName }
 * PATCH /api/leave-requests/{id}         { decision: APPROVED | REJECTED, note }
 * </pre>
 */
@WebServlet(name = "LeaveRequestApiServlet", urlPatterns = {"/api/leave-requests", "/api/leave-requests/*"})
public class LeaveRequestApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern ITEM = Pattern.compile("^/(\\d+)$");

    private final transient LeaveRequestDAO dao = new LeaveRequestDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        User user = MyScheduleApiServlet.currentUser(req);
        Matcher m = ITEM.matcher(path);
        if ("GET".equals(method) && path.isEmpty()) {
            if ("mine".equals(req.getParameter("scope"))) {
                ApiResponse.success(resp, "LEAVE_LIST", "Đơn xin phép của tôi.", dao.mine(user));
            } else {
                requireReviewer(user);
                ApiResponse.success(resp, "LEAVE_LIST", "Đơn xin phép cần duyệt.", dao.forReviewer(user));
            }
        } else if ("POST".equals(method) && path.isEmpty()) {
            JsonObject b = body(req);
            String sessionId = str(b, "sessionId");
            if (sessionId == null || !sessionId.matches("\\d+")) throw new ValidationException("Chọn buổi học muốn xin vắng.");
            ApiResponse.created(resp, "LEAVE_CREATED", "Đã gửi đơn xin phép.",
                    dao.create(user, Long.parseLong(sessionId), str(b, "reason"), str(b, "fileName")));
        } else if ("PATCH".equals(method) && m.matches()) {
            requireReviewer(user);
            JsonObject b = body(req);
            String decision = str(b, "decision");
            if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) throw new ValidationException("decision phải là APPROVED hoặc REJECTED.");
            ApiResponse.success(resp, "LEAVE_DECIDED", "APPROVED".equals(decision) ? "Đã duyệt đơn." : "Đã từ chối đơn.",
                    dao.decide(user, Long.parseLong(m.group(1)), "APPROVED".equals(decision), str(b, "note")));
        } else {
            notFound(resp);
        }
    }

    /** GET danh sách chung qua bộ lọc với ABSENCE_REQUEST hoặc ATTENDANCE_MANAGE; học viên chỉ xem đơn của mình. */
    private static void requireReviewer(User user) {
        java.util.List<String> roles = user.getRoles() == null ? java.util.List.of() : user.getRoles();
        if (roles.stream().noneMatch(r -> r.equals("INSTRUCTOR") || r.equals("TA") || r.equals("TRAINING_MANAGER") || r.equals("ADMIN"))) {
            throw new ApiException(HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN", "Chỉ giảng viên, trợ giảng hoặc Quản lý đào tạo duyệt đơn.");
        }
    }
}
