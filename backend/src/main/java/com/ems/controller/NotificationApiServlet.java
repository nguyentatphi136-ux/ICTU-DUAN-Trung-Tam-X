package com.ems.controller;

import com.ems.dao.NotificationDAO;
import com.ems.exception.ValidationException;
import com.ems.security.ApiResponse;
import com.google.gson.JsonElement;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Thông báo của người đang đăng nhập (S6-09, S6-10), ai đăng nhập cũng dùng được.
 *
 * <pre>
 * GET  /api/me/notifications          [{ id, title, body, type, link, read, at }] 100 thông báo mới nhất
 * POST /api/me/notifications/read     { ids: [..] } đánh dấu đã đọc; không có ids là tất cả
 * </pre>
 */
@WebServlet(name = "NotificationApiServlet", urlPatterns = {"/api/me/notifications", "/api/me/notifications/read"})
public class NotificationApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;

    private final transient NotificationDAO dao = new NotificationDAO();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long me = MyScheduleApiServlet.currentUser(req).getId();
        boolean read = "/api/me/notifications/read".equals(req.getServletPath());
        if ("GET".equals(method) && !read) {
            ApiResponse.success(resp, "NOTIFICATION_LIST", "Thông báo của tôi.", dao.inbox(me));
        } else if ("POST".equals(method) && read) {
            List<Long> ids = new ArrayList<>();
            JsonElement el = body(req).get("ids");
            if (el != null && !el.isJsonNull()) {
                if (!el.isJsonArray()) throw new ValidationException("ids phải là danh sách mã thông báo.");
                for (JsonElement x : el.getAsJsonArray()) ids.add(x.getAsLong());
            }
            dao.markRead(me, ids);
            ApiResponse.success(resp, "NOTIFICATION_READ", "Đã đánh dấu đã đọc.", null);
        } else {
            notFound(resp);
        }
    }
}
