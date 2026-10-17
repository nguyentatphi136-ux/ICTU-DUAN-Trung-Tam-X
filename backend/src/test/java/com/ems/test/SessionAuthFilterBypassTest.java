package com.ems.test;

import com.ems.filter.SessionAuthFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/** "/api/admin/users;.json": Tomcat vẫn chọn AdminApiServlet nên bộ lọc không được coi là tệp tĩnh công khai. */
class SessionAuthFilterBypassTest {

    @ParameterizedTest
    @ValueSource(strings = {";.json", ";x.js", ";.css", ";.html", ""})
    @DisplayName("Khách gọi /api/admin/users kèm tham số ;... vẫn bị chặn 401")
    void apiWithPathParameterNeedsLogin(String suffix) throws Exception {
        int[] status = {0};
        boolean[] passed = {false};
        StringWriter body = new StringWriter();

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletRequest.class}, (p, m, a) -> switch (m.getName()) {
                    case "getContextPath" -> "";
                    case "getServletPath" -> "/api/admin";
                    case "getPathInfo" -> "/users";
                    case "getRequestURI" -> "/api/admin/users" + suffix;
                    case "getMethod" -> "GET";
                    default -> null;
                });
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {HttpServletResponse.class}, (p, m, a) -> switch (m.getName()) {
                    case "setStatus" -> { status[0] = (int) a[0]; yield null; }
                    case "getWriter" -> new PrintWriter(body);
                    default -> null;
                });

        new SessionAuthFilter().doFilter(req, res, (rq, rs) -> passed[0] = true);

        assertFalse(passed[0], "Không được cho đi tiếp tới servlet");
        assertEquals(401, status[0]);
    }
}
