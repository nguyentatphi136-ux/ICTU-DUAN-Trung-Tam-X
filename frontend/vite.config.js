import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { defineConfig } from "vite";

const pages = [
  "index.html",
  "login.html",
  "ForgotPasswordForm.html",
  "ResetPasswordForm.html",
  "student.html",
  "instructor.html",
  "ta.html",
  "training-manager.html",
  "admissions.html",
  "accountant.html",
  "admin.html",
];

const blacklistedSessions = new Set();

const authMockPlugin = () => ({
  name: "auth-mock-plugin",
  configureServer(server) {
    server.middlewares.use((req, res, next) => {
      // 1. API Đăng nhập tạo Session (IDTTX-33)
      if (req.url === "/api/auth/login" && req.method === "POST") {
        const mockSessionId = "EMS_SESS_" + Math.random().toString(36).substring(2, 10).toUpperCase();
        res.setHeader("Content-Type", "application/json;charset=utf-8");
        res.setHeader("Set-Cookie", `JSESSIONID=${mockSessionId}; Path=/; HttpOnly`);
        res.statusCode = 200;
        res.end(
          JSON.stringify({
            status: 200,
            sessionId: mockSessionId,
            message: "Đăng nhập thành công, phiên làm việc đã được tạo (IDTTX-33).",
            user: { email: "admin@edumanager.vn", fullName: "Quản trị viên", role: "admin" }
          })
        );
        return;
      }

      // 2. API Đăng xuất thu hồi Session (IDTTX-34, IDTTX-35, IDTTX-55)
      if (req.url === "/auth/logout" || req.url === "/api/auth/logout") {
        // Lấy session ID từ cookie nếu có và đưa vào Blacklist
        const cookieHeader = req.headers.cookie || "";
        const match = cookieHeader.match(/JSESSIONID=([^;]+)/);
        const currentSid = match ? match[1] : "EMS_SESS_CURRENT";
        blacklistedSessions.add(currentSid);

        res.setHeader("Content-Type", "application/json;charset=utf-8");
        res.setHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly");
        res.statusCode = 200;
        res.end(
          JSON.stringify({
            status: 200,
            message: "Đăng xuất thành công. Phiên đã bị thu hồi ngay phía server (Session Blacklist).",
            revokedSessionId: currentSid,
            sessionIdRevoked: true,
          }),
        );
        return;
      }

      // 3. API Kiểm tra phiên, Gia hạn (IDTTX-36), Kiểm tra Hết hạn (IDTTX-54) & Blacklist (IDTTX-55)
      if (req.url === "/api/auth/check-session") {
        const cookieHeader = req.headers.cookie || "";
        const match = cookieHeader.match(/JSESSIONID=([^;]+)/);
        const currentSid = match ? match[1] : null;

        res.setHeader("Content-Type", "application/json;charset=utf-8");

        // Trường hợp 1: Không có session -> Hết hạn 401 (IDTTX-54)
        if (!currentSid) {
          res.statusCode = 401;
          res.end(
            JSON.stringify({
              status: 401,
              error: "session_expired",
              message: "Phiên đăng nhập đã hết hạn hoặc không tồn tại. Vui lòng đăng nhập lại (IDTTX-54).",
              redirectUrl: "/login.html?error=session_expired"
            })
          );
          return;
        }

        // Trường hợp 2: Session nằm trong Blacklist -> Bị thu hồi 401 (IDTTX-55)
        if (blacklistedSessions.has(currentSid)) {
          res.statusCode = 401;
          res.end(
            JSON.stringify({
              status: 401,
              error: "session_revoked",
              message: `Phiên ${currentSid} đã bị thu hồi do đăng xuất. Không thể tái sử dụng (IDTTX-55)!`,
              isBlacklisted: true
            })
          );
          return;
        }

        // Trường hợp 3: Phiên hợp lệ -> Tự động gia hạn 8h (IDTTX-36 - Sliding Window)
        res.statusCode = 200;
        res.end(
          JSON.stringify({
            status: 200,
            sessionId: currentSid,
            message: "Phiên hợp lệ. Hệ thống đã tự động gia hạn thêm 8 giờ (Sliding Window - IDTTX-36).",
            timeoutSeconds: 28800,
          }),
        );
        return;
      }

      // 4. API Quên mật khẩu - Sinh token và gửi mail (IDTTX-41)
      if (
        req.url === "/auth/forgot-password" ||
        req.url === "/api/auth/forgot-password"
      ) {
        let body = "";
        req.on("data", (chunk) => {
          body += chunk;
        });
        req.on("end", () => {
          const mockToken =
            "RESET_TOKEN_" +
            Math.random().toString(36).substring(2, 10).toUpperCase();
          res.setHeader("Content-Type", "application/json;charset=utf-8");
          res.statusCode = 200;
          res.end(
            JSON.stringify({
              status: 200,
              message:
                "Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu qua email trong vài phút (IDTTX-41).",
              devToken: mockToken,
              devResetLink: `/ResetPasswordForm.html?token=${mockToken}`,
              expiresInMinutes: 30,
            }),
          );
        });
        return;
      }

      // 5. API Đặt lại mật khẩu mới - Kiểm tra token (IDTTX-42)
      if (
        req.url === "/auth/reset-password" ||
        req.url === "/api/auth/reset-password"
      ) {
        let body = "";
        req.on("data", (chunk) => {
          body += chunk;
        });
        req.on("end", () => {
          res.setHeader("Content-Type", "application/json;charset=utf-8");
          res.statusCode = 200;
          res.end(
            JSON.stringify({
              status: 200,
              message:
                "Đặt lại mật khẩu thành công! Token đã được sử dụng và vô hiệu hóa (IDTTX-42).",
              redirectUrl: "/login.html?message=password_reset_success",
            }),
          );
        });
        return;
      }

      next();
    });
  },
});



export default defineConfig({
  plugins: process.env.VITE_AUTH_MOCK === "true" ? [authMockPlugin()] : [],
  server: {
    proxy: {
      "/api": {
        target: process.env.BACKEND_URL || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  css: {
    modules: {
      generateScopedName: (localName, filename) => {
        const safeName = localName.replace(/[^a-zA-Z0-9_-]/g, "_");
        const hash = createHash("sha1")
          .update(`${filename}:${localName}`)
          .digest("hex")
          .slice(0, 6);

        return `u_${safeName}_${hash}`;
      },
    },
  },
  build: {
    rollupOptions: {
      input: Object.fromEntries(
        pages.map((page) => [
          page.replace(/\.html$/, ""),
          resolve(process.cwd(), page),
        ]),
      ),
    },
  },
});

