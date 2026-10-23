package com.ems.controller;

import com.ems.dao.StudentStatusDAO;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.StudentStatusLog;
import com.ems.model.User;
import com.ems.security.ApiResponse;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S4-06 Bảo lưu, chuyển lớp, thôi học cho trang hồ sơ học viên (React). Xem cần STUDENT_READ, thao tác cần CLASS_MANAGE.
 * Hồ sơ, lớp đang học, lịch sử lớp vẫn đọc ở GET /api/students/{code} (S3-04).
 *
 * <pre>
 * GET  /api/student-status/{code}   { changes: [{ type, at, reason, by, from, to, recorded }] }
 * POST /api/student-status/{code}   { type: Bảo lưu | Chuyển lớp | Thôi học, at, reason, note, to }
 * </pre>
 */
@WebServlet(name = "StudentStatusApiServlet", urlPatterns = {"/api/student-status/*"})
public class StudentStatusApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern ITEM = Pattern.compile("^/([A-Za-z0-9_-]+)$");

    private final transient StudentStatusDAO dao = new StudentStatusDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Matcher m = ITEM.matcher(path);
        if (!m.matches()) {
            notFound(resp);
            return;
        }
        String code = m.group(1);
        try {
            if ("GET".equals(method)) {
                ApiResponse.success(resp, "STATUS_CHANGES", "Lịch sử bảo lưu, chuyển lớp, thôi học.", Map.of("changes", changes(code)));
            } else if ("POST".equals(method)) {
                User user = MyScheduleApiServlet.currentUser(req);
                JsonObject b = body(req);
                String reason = str(b, "reason");
                String note = str(b, "note");
                if ("Lý do khác".equals(reason) && (note == null || note.isBlank())) throw new ValidationException("Ghi rõ lý do vào ô ghi chú.");
                if (dao.getStudentByCode(code) == null) throw new ResourceNotFoundException("Không tìm thấy học viên " + code + ".");
                dao.processStatusChange(code, str(b, "type"), str(b, "at"), reason, note, str(b, "to"), user.getName(), user.getId());
                ApiResponse.success(resp, "STATUS_CHANGED", "Đã ghi nhận thay đổi tình trạng học.", Map.of("changes", changes(code)));
            } else {
                notFound(resp);
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException(e.getMessage());
        } catch (IllegalStateException e) {
            // StudentStatusDAO báo lỗi CSDL bằng IllegalStateException; JsonApiServlet coi đó là JSON sai nên đổi lại.
            throw new SQLException(e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> changes(String code) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StudentStatusLog l : dao.getStatusLogsForStudent(code)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("type", l.getActionType());
            row.put("at", l.getEffectiveDate());
            row.put("reason", l.getNotes() == null ? l.getReason() : l.getReason() + ". " + l.getNotes());
            row.put("by", l.getActorName());
            row.put("from", l.getFromClass());
            row.put("to", l.getToClass());
            row.put("recorded", l.getRecordedDate());
            out.add(row);
        }
        return out;
    }
}
