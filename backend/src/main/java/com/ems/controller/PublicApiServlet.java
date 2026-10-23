package com.ems.controller;

import com.ems.security.ApiResponse;
import com.ems.service.LeadService;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * S2-08. API công khai, không cần đăng nhập (SessionAuthFilter cho qua /api/public/).
 * POST /api/public/consultations bị giới hạn 5 lần mỗi 15 phút cho một địa chỉ IP (AuthRateLimitFilter).
 *
 * <pre>
 * GET  /api/public/programs        [{ code, name }] chương trình đang tuyển sinh
 * POST /api/public/consultations   { name, phone, email, programCode, message } tạo lead Mới, chưa phân công
 * </pre>
 *
 * Ô "Tôi không phải người máy" trên giao diện chưa là CAPTCHA thật; trường ẩn website dùng làm bẫy bot:
 * có giá trị thì vẫn trả thành công nhưng không lưu.
 */
@WebServlet(name = "PublicApiServlet", urlPatterns = {"/api/public/*"})
public class PublicApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;

    private transient LeadService service = new LeadService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if ("GET".equals(method) && "/programs".equals(path)) {
            ApiResponse.success(resp, "PUBLIC_PROGRAMS", "Chương trình đang tuyển sinh.", service.publicPrograms());
        } else if ("POST".equals(method) && "/consultations".equals(path)) {
            JsonObject b = body(req);
            String trap = str(b, "website");
            if (trap == null || trap.isEmpty()) {
                service.submitConsultation(str(b, "name"), str(b, "phone"), str(b, "email"), str(b, "programCode"), str(b, "message"));
            }
            ApiResponse.created(resp, "CONSULTATION_RECEIVED", "Cảm ơn bạn. Tư vấn viên sẽ gọi lại trong 24 giờ làm việc.", null);
        } else {
            notFound(resp);
        }
    }
}
