import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";
import { execFile } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function devEmailPlugin() {
  return {
    name: "dev-email-service",
    configureServer(server) {
      server.middlewares.use("/api/send-activation-email", (req, res) => {
        if (req.method !== "POST") {
          res.statusCode = 405;
          return res.end(JSON.stringify({ success: false, error: "Method not allowed" }));
        }
        let body = "";
        req.on("data", (chunk) => {
          body += chunk;
        });
        req.on("end", () => {
          try {
            const data = JSON.parse(body || "{}");
            const scriptPath = path.resolve(__dirname, "../scripts/send_mail.py");
            const args = [
              scriptPath,
              data.toEmail || "",
              data.fullName || "Học viên",
              data.loginEmail || "",
              data.tempPassword || "Tms@2026",
              data.roleName || "Học viên",
            ];
            execFile("python", args, { timeout: 25000 }, (err, stdout, stderr) => {
              res.setHeader("Content-Type", "application/json; charset=utf-8");
              if (err) {
                console.error("[EmailService Error]", stderr || err.message);
                res.statusCode = 500;
                return res.end(JSON.stringify({ success: false, error: err.message }));
              }
              try {
                const parsed = JSON.parse(stdout);
                res.statusCode = parsed.success ? 200 : 400;
                res.end(stdout);
              } catch {
                res.statusCode = 200;
                res.end(JSON.stringify({ success: true, raw: stdout }));
              }
            });
          } catch (e) {
            res.statusCode = 400;
            res.end(JSON.stringify({ success: false, error: "Invalid JSON body" }));
          }
        });
      });
    },
  };
}

// Ứng dụng một trang (React Router). Máy chủ dev tự trả index.html cho mọi đường dẫn.
// Có VITE_API_URL (ví dụ trong .env.local: VITE_API_URL=http://localhost:8080) thì /api chuyển sang backend Java.
export default defineConfig(({ mode }) => {
  const { VITE_API_URL } = loadEnv(mode, process.cwd());
  return {
    plugins: [react(), devEmailPlugin()],
    server: VITE_API_URL ? { proxy: { "/api": { target: VITE_API_URL, changeOrigin: true } } } : undefined,
  };
});

