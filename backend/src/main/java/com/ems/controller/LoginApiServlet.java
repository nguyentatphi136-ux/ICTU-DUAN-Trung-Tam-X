package com.ems.controller;

import com.ems.dao.UserDAO;
import com.ems.model.User;
import com.google.gson.Gson;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import com.ems.security.IpLoginAttemptService;
import com.ems.security.LoginAttemptPolicy;

@WebServlet(urlPatterns = {"/login", "/auth/login", "/api/auth/login"})
public class LoginApiServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng.";
    /** Thuộc tính phiên: còn đặt thì SessionAuthFilter chỉ cho gọi đổi mật khẩu và đăng xuất. */
    public static final String MUST_CHANGE_PASSWORD = "mustChangePassword";
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard.jsp");
            return;
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        boolean apiRequest = isApiRequest(request);
        String email;
        String password;
        try {
            if (apiRequest && request.getContentType() != null
                    && request.getContentType().contains("application/json")) {
                com.google.gson.JsonObject body = GSON.fromJson(request.getReader(), com.google.gson.JsonObject.class);
                email = body == null || !body.has("email") ? null : body.get("email").getAsString();
                password = body == null || !body.has("password") ? null : body.get("password").getAsString();
            } else {
                email = request.getParameter("email");
                password = request.getParameter("password");
            }
        } catch (RuntimeException exception) {
            sendApiError(response, 400, "REQUEST_INVALID_JSON", "Dữ liệu đăng nhập không hợp lệ.");
            return;
        }

        String clientIp = IpLoginAttemptService.getClientIp(request);

        // 1. Kiểm tra xem ĐỊA CHỈ IP MÁY này có đang bị khóa 15 phút do nhập sai 5 lần hay không
        if (IpLoginAttemptService.isIpLocked(clientIp)) {
            long remainingSeconds = IpLoginAttemptService.getRemainingLockSeconds(clientIp);
            long minutes = Math.max(1, (remainingSeconds + 59) / 60);
            String lockMsg = "Địa chỉ IP máy của bạn đã bị tạm khóa 15 phút do nhập sai thông tin 5 lần liên tiếp. "
                    + "Vui lòng thử lại sau " + minutes + " phút. "
                    + "(Tài khoản vẫn có thể đăng nhập bình thường từ thiết bị khác).";
            if (apiRequest) {
                response.setHeader("Retry-After", String.valueOf(remainingSeconds));
                sendApiError(response, 429, "AUTH_IP_LOCKED", lockMsg);
            } else {
                request.setAttribute("errorMessage", lockMsg);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
            return;
        }

        User user = null;
        boolean databaseDown = false;
        try {
            user = userDAO.authenticate(email, password);
        } catch (SQLException exception) {
            databaseDown = true;
            getServletContext().log("Không thể kết nối MySQL, chuyển sang xác thực dự phòng.", exception);
        }
        // Còn dùng mật khẩu mặc định thì phải đổi trước khi làm việc. Tài khoản dự phòng không có CSDL để đổi nên bỏ qua.
        // Mật khẩu mặc định, hoặc tài khoản mới còn dùng mật khẩu tạm (S1-08): phải đổi ở lần đăng nhập này.
        boolean mustChangePassword = user != null && (UserDAO.DEFAULT_PASSWORD.equals(password) || user.isMustChangePassword());

        // Dự phòng xác thực in-memory UserStore (IDTTX-20, IDTTX-24) chỉ khi DB chưa sẵn sàng. CSDL trả về "sai mật khẩu"
        // thì không thử tiếp, nếu không mật khẩu mặc định công khai luôn đăng nhập được tài khoản quản trị id 1.
        if (user == null && databaseDown) {
            user = com.ems.service.UserStore.authenticate(email, password);
        }

        if (user == null) {
            // Ghi nhận lần nhập sai cho ĐỊA CHỈ IP MÁY
            LoginAttemptPolicy.State ipState = IpLoginAttemptService.recordFailure(clientIp);
            if (LoginAttemptPolicy.isLocked(ipState, Instant.now())) {
                long remainingSeconds = IpLoginAttemptService.getRemainingLockSeconds(clientIp);
                String lockMsg = "Địa chỉ IP máy của bạn đã bị tạm khóa 15 phút do nhập sai thông tin 5 lần liên tiếp. "
                        + "Vui lòng thử lại sau 15 phút. "
                        + "(Tài khoản của bạn không bị khóa và vẫn có thể đăng nhập bình thường từ thiết bị khác).";
                if (apiRequest) {
                    response.setHeader("Retry-After", String.valueOf(remainingSeconds));
                    sendApiError(response, 429, "AUTH_IP_LOCKED", lockMsg);
                } else {
                    request.setAttribute("errorMessage", lockMsg);
                    request.getRequestDispatcher("/login.jsp").forward(request, response);
                }
                return;
            }

            int remainingAttempts = Math.max(0, LoginAttemptPolicy.MAX_FAILED_ATTEMPTS - ipState.failures());
            String errorMsg = "Email hoặc mật khẩu không đúng. IP máy này còn " + remainingAttempts 
                    + " lần thử trước khi bị tạm khóa 15 phút (khóa theo IP máy, không khóa tài khoản).";
            if (apiRequest) {
                sendApiError(response, 401, "AUTH_CREDENTIALS_INVALID", errorMsg);
            } else {
                request.setAttribute("errorMessage", errorMsg);
                request.getRequestDispatcher("/login.jsp").forward(request, response);
            }
            return;
        }

        // Đăng nhập thành công -> Xóa bộ đếm lỗi cho IP máy này
        IpLoginAttemptService.recordSuccess(clientIp);

        HttpSession previousSession = request.getSession(false);
        if (previousSession != null) previousSession.invalidate();
        HttpSession session = request.getSession(true);
        request.changeSessionId();
        session.setAttribute("currentUser", user);
        if (mustChangePassword) session.setAttribute(MUST_CHANGE_PASSWORD, true);
        session.setMaxInactiveInterval(8 * 60 * 60);
        com.ems.config.SessionBlacklist.registerSession(user.getId(), session.getId());

        String role = roleSlug(user.getPrimaryRole());
        String redirectUrl = roleHome(role);
        if (!apiRequest) {
            // Chuyển hướng nội bộ theo JSP & Servlet
            String jspRedirect = mustChangePassword ? "/auth/change-password" : jspHome(role);
            response.sendRedirect(request.getContextPath() + jspRedirect);
            return;
        }

        Map<String, Object> userData = new LinkedHashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", role);
        userData.put("roles", user.getRoles().stream().map(LoginApiServlet::roleSlug).toList());
        userData.put("mustChangePassword", mustChangePassword);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(Map.of(
                "success", true,
                "code", "AUTH_LOGIN_SUCCESS",
                "message", "Đăng nhập thành công.",
                "data", Map.of("user", userData, "redirectUrl", redirectUrl)
        )));
    }

    private static boolean isApiRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getRequestURI().endsWith("/api/auth/login")
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (accept != null && accept.contains("application/json"));
    }

    private static String roleSlug(String role) {
        return role == null ? "student" : role.toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private static String roleHome(String role) {
        return switch (role) {
            case "admin" -> "/admin.html";
            case "training-manager" -> "/training-manager.html";
            case "admissions" -> "/admissions.html";
            case "instructor" -> "/instructor.html";
            case "ta" -> "/ta.html";
            case "accountant" -> "/accountant.html";
            case "student" -> "/student.html";
            default -> "/index.html";
        };
    }

    private static String jspHome(String role) {
        return switch (role) {
            case "admin" -> "/admin/users/roles";
            case "instructor" -> "/grade/list";
            case "accountant" -> "/tuition/list";
            case "student" -> "/grade/list";
            case "training-manager" -> "/training-programs";
            default -> "/dashboard.jsp";
        };
    }

    private void sendApiError(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(GSON.toJson(Map.of(
                "success", false,
                "error", Map.of(
                        "code", code,
                        "message", message,
                        "action", Map.of("label", "Quay lại đăng nhập", "href", "/login.html")
                )
        )));
    }
}