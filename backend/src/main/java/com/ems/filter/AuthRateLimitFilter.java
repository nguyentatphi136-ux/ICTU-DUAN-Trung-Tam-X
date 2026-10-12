package com.ems.filter;

import com.ems.security.ApiResponse;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@WebFilter(urlPatterns = {"/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password"})
public class AuthRateLimitFilter implements Filter {
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;
    private static final int MAX_KEYS = 10000;
    private static final Map<String, Window> WINDOWS = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String key = httpRequest.getRemoteAddr() + ":" + path;
        long now = Instant.now().toEpochMilli();
        int limit = "/api/auth/forgot-password".equals(path) ? 5 : 30;
        synchronized (WINDOWS) {
            Window window = WINDOWS.get(key);
            if (window == null || now - window.startedAt >= WINDOW_MILLIS) {
                if (WINDOWS.size() >= MAX_KEYS) {
                    Iterator<Map.Entry<String, Window>> iterator = WINDOWS.entrySet().iterator();
                    while (iterator.hasNext()) {
                        if (now - iterator.next().getValue().startedAt >= WINDOW_MILLIS) iterator.remove();
                    }
                }
                if (WINDOWS.size() >= MAX_KEYS) {
                    rateLimited((HttpServletResponse) response);
                    return;
                }
                window = new Window(now);
                WINDOWS.put(key, window);
            }
            if (window.count >= limit) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setHeader("Retry-After", Long.toString((window.startedAt + WINDOW_MILLIS - now + 999) / 1000));
                rateLimited(httpResponse);
                return;
            }
            window.count++;
        }
        chain.doFilter(request, response);
    }

    private void rateLimited(HttpServletResponse response) throws IOException {
        ApiResponse.error(response, 429, "AUTH_RATE_LIMITED",
                "Bạn thao tác quá nhiều lần. Vui lòng thử lại sau.", "Quay lại đăng nhập", "/login.html");
    }

    @Override
    public void destroy() {}

    private static final class Window {
        private final long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}