package com.ems.bootstrap;

import com.ems.config.DBConnection;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * Chạy backend bằng Tomcat nhúng khi phát triển: `npm run start:dev` trong thư mục backend
 * (gọi `mvnw compile exec:exec`). Không cần cài Tomcat; servlet, filter, listener được nạp theo annotation.
 * Cổng lấy từ biến PORT (mặc định 8080), CSDL lấy từ DB_URL, DB_USER, DB_PASSWORD như DBConnection.
 */
public class TomcatBootstrap {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(DBConnection.setting("PORT", "8080"));

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        // Ngữ cảnh gốc "/" để frontend (Vite) chuyển tiếp /api sang thẳng đây.
        Context ctx = tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                new File("target/classes").getAbsolutePath(), "/"));
        ctx.setResources(resources);
        // Cookie phiên JSESSIONID kèm SameSite=Lax để trang web khác không gửi POST mang phiên của người dùng (CSRF).
        Rfc6265CookieProcessor cookies = new Rfc6265CookieProcessor();
        cookies.setSameSiteCookies("Lax");
        ctx.setCookieProcessor(cookies);

        tomcat.start();
        if (tomcat.getConnector().getState() != LifecycleState.STARTED) {
            System.err.println("Không mở được cổng " + port + ". Cổng đang bị chương trình khác dùng? Đặt PORT khác trong backend/.env.");
            tomcat.stop();
            System.exit(1);
        }
        System.out.println("Backend EMS chạy tại http://localhost:" + port + " (Ctrl+C để dừng)");
        tomcat.getServer().await();
    }
}
