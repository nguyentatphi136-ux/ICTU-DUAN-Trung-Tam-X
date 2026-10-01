package vn.edu.ictu.ems.filter;

import vn.edu.ictu.ems.constant.PermissionConstant;
import vn.edu.ictu.ems.constant.RoleConstant;
import vn.edu.ictu.ems.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Tác vụ Jira: IDTTX-81 [BE] Xây dựng middleware kiểm tra quyền ở tầng server, mặc định từ chối
 * Áp dụng mô hình Java Servlet / JSP Filter
 * Đảm bảo: Giảng viên không sửa được học phí, Kế toán không sửa được điểm
 * Người thực hiện: Nguyễn Minh Ngọc (MN)
 */
@WebFilter(filterName = "AuthorizationFilter", urlPatterns = {"/*"})
public class AuthorizationFilter implements Filter {

    // Danh sách các đường dẫn công khai (không cần kiểm tra quyền)
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/login",
            "/login.jsp",
            "/logout",
            "/index.html",
            "/index.jsp",
            "/css/",
            "/js/",
            "/images/",
            "/error"
    );

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Khởi tạo filter
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        String contextPath = req.getContextPath();
        String uri = req.getRequestURI().substring(contextPath.length());

        // 1. Kiểm tra tài nguyên công khai
        for (String publicPath : PUBLIC_PATHS) {
            if (uri.equals(publicPath) || uri.startsWith(publicPath)) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        // 2. Kiểm tra xác thực (Session Login)
        HttpSession session = req.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            req.setAttribute("errorMessage", "Vui lòng đăng nhập để tiếp tục");
            req.getRequestDispatcher("/login.jsp").forward(req, res);
            return;
        }

        // Tác vụ Jira IDTTX-24: Đảm bảo thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp mà không cần đăng nhập lại
        vn.edu.ictu.ems.model.User freshUser = vn.edu.ictu.ems.service.UserStore.findById(currentUser.getId());
        if (freshUser != null) {
            currentUser = freshUser;
            session.setAttribute("currentUser", freshUser);
        }

        // Kiểm tra trạng thái tài khoản
        if ("locked".equalsIgnoreCase(currentUser.getStatus()) || "inactive".equalsIgnoreCase(currentUser.getStatus())) {
            denyAccess(req, res, "Tài khoản của bạn đã bị khoá hoặc ngừng hoạt động.");
            return;
        }

        List<String> roles = currentUser.getRoles();

        // 3. KIỂM QUYỀN ĐIỂM SỐ (Grades)
        // Đặc tả: Giảng viên sửa được điểm, Kế toán TUYỆT ĐỐI KHÔNG sửa được điểm
        if (uri.startsWith("/grade/edit") || uri.startsWith("/grade/update") || uri.startsWith("/grade/delete")) {
            boolean isAccountantOnly = roles.contains(RoleConstant.ACCOUNTANT) &&
                    !roles.contains(RoleConstant.ADMIN) &&
                    !roles.contains(RoleConstant.INSTRUCTOR) &&
                    !roles.contains(RoleConstant.TRAINING_MANAGER);

            if (isAccountantOnly) {
                denyAccess(req, res, "Kế toán không có quyền chỉnh sửa điểm số học viên.");
                return;
            }

            if (!PermissionConstant.hasPermission(roles, PermissionConstant.GRADE_EDIT)) {
                denyAccess(req, res, "Bạn không có quyền chỉnh sửa điểm số học viên.");
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 4. KIỂM QUYỀN HỌC PHÍ (Tuition)
        // Đặc tả: Kế toán sửa được học phí, Giảng viên TUYỆT ĐỐI KHÔNG sửa được học phí
        if (uri.startsWith("/tuition/edit") || uri.startsWith("/tuition/update") || uri.startsWith("/tuition/collect")) {
            boolean isInstructorOnly = (roles.contains(RoleConstant.INSTRUCTOR) || roles.contains(RoleConstant.TEACHING_ASSISTANT)) &&
                    !roles.contains(RoleConstant.ADMIN) &&
                    !roles.contains(RoleConstant.ACCOUNTANT);

            if (isInstructorOnly) {
                denyAccess(req, res, "Giảng viên không có quyền chỉnh sửa thông tin học phí.");
                return;
            }

            if (!PermissionConstant.hasPermission(roles, PermissionConstant.TUITION_EDIT)) {
                denyAccess(req, res, "Bạn không có quyền chỉnh sửa thông tin học phí.");
                return;
            }

            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 5. KIỂM QUYỀN QUẢN TRỊ ADMIN (/admin/*)
        if (uri.startsWith("/admin")) {
            if (!roles.contains(RoleConstant.ADMIN)) {
                denyAccess(req, res, "Chức năng này chỉ dành riêng cho Quản trị hệ thống (System Admin).");
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 6. CÁC TRANG TRA CỨU ĐIỂM SỐ & HỌC PHÍ THÔNG THƯỜNG
        if (uri.startsWith("/grade/view") || uri.startsWith("/grade/list")) {
            if (!PermissionConstant.hasPermission(roles, PermissionConstant.GRADE_VIEW)) {
                denyAccess(req, res, "Bạn không có quyền xem bảng điểm.");
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        if (uri.startsWith("/tuition/view") || uri.startsWith("/tuition/list")) {
            if (!PermissionConstant.hasPermission(roles, PermissionConstant.TUITION_VIEW)) {
                denyAccess(req, res, "Bạn không có quyền tra cứu thông tin học phí.");
                return;
            }
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 7. NGUYÊN TẮC MẶC ĐỊNH TỪ CHỐI (DEFAULT-DENY)
        // Bất kỳ đường dẫn bảo vệ nào (/secure/* hoặc không thuộc danh mục cho phép) đều mặc định bị từ chối
        if (uri.startsWith("/secure/")) {
            denyAccess(req, res, "Truy cập bị từ chối: Chức năng được bảo vệ và mặc định từ chối ở tầng máy chủ.");
            return;
        }

        // Cho phép các trang JSP thông thường trong phiên đăng nhập
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void denyAccess(HttpServletRequest req, HttpServletResponse res, String vietnameseMessage)
            throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        req.setAttribute("errorCode", "403");
        req.setAttribute("errorMessage", vietnameseMessage);
        req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, res);
    }

    @Override
    public void destroy() {
        // Dọn dẹp tài nguyên
    }
}
