package com.ems.filter;

import com.ems.security.ApiResponse;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Backend không còn giao diện JSP cũ. Servlet cũ chuyển tiếp sang .jsp (đăng nhập, trang lỗi 403/404/500...)
 * thì trả JSON thay vì vẽ trang; truy cập thẳng .jsp đã bị SessionAuthFilter chặn.
 */
@WebFilter(urlPatterns = "*.jsp", dispatcherTypes = {DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR})
public class JspBlockFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        HttpServletResponse resp = (HttpServletResponse) response;
        int status = resp.getStatus();
        resp.resetBuffer();
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            ApiResponse.error(resp, status, "AUTH_FORBIDDEN", "Bạn không có quyền truy cập chức năng này.");
        } else if (status >= 500) {
            ApiResponse.error(resp, status, "INTERNAL_SERVER_ERROR", "Lỗi máy chủ ngoài dự kiến.");
        } else {
            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "ENDPOINT_NOT_FOUND",
                    "Backend chỉ cung cấp API tại /api. Giao diện chạy ở frontend.");
        }
    }
}
