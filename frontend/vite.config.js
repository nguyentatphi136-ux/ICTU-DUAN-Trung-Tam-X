import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { defineConfig } from "vite";

let nodemailer = null;
try {
  const mod = await import("nodemailer");
  nodemailer = mod.default || mod;
} catch (_) {}

function emailPlugin() {
  const transporter = nodemailer ? nodemailer.createTransport({
    host: "smtp.gmail.com",
    port: 587,
    secure: false,
    auth: {
      user: "tatphi2006@gmail.com",
      pass: "tcjotoxtkhfwyldn",
    },
  }) : null;

  const resetTokenStore = new Map(); // token -> { email, expiresAt, isUsed, createdAt }
  const rateLimitStore = new Map(); // email -> lastRequestTimestamp (15 phút cooldown)
  const RATE_LIMIT_COOLDOWN_MS = 15 * 60 * 1000; // 15 phút

  return {
    name: "vite-email-service",
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        // 1. Endpoint gửi liên kết đặt lại mật khẩu có hiệu lực 30 phút, dùng 1 lần (S1-03)
        if (req.method === "POST" && (req.url === "/api/forgot-password" || req.url === "/api/send-reset-link" || req.url === "/api/auth/forgot-password" || req.url === "/api/send-otp")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", async () => {
            try {
              const data = JSON.parse(body || "{}");
              const toEmail = (data.email || "").trim().toLowerCase();

              if (!toEmail || !toEmail.includes("@")) {
                res.setHeader("Content-Type", "application/json");
                res.statusCode = 400;
                res.end(JSON.stringify({ success: false, message: "Vui lòng nhập địa chỉ email hợp lệ." }));
                return;
              }

              const knownEmails = new Set([
                "tatphi2006@gmail.com",
                "admin@tms.vn", "admin@example.com", "admin@edumanager.vn",
                "daotao@tms.vn", "training@example.com", "training@edumanager.vn",
                "tuyensinh@tms.vn", "tuvan@tms.vn", "admissions@example.com",
                "giangvien@tms.vn", "instructor@example.com",
                "trogiang@tms.vn", "ta@example.com",
                "ketoan@tms.vn", "accountant@example.com",
                "hocvien@tms.vn", "student@example.com",
                "hai.nguyen@tms.vn", "anh.tran@tms.vn", "tuan.le@tms.vn",
                "huong.pham@tms.vn", "bao.hoang@tms.vn", "thao.vu@tms.vn",
                "dung.dang@tms.vn", "linh.bui@tms.vn", "huy.ngo@tms.vn",
                "tuyet.duong@tms.vn", "nam.ly@tms.vn", "ngoc.ta@tms.vn",
                "quyet.trinh@tms.vn", "bich.le@tms.vn", "anh.duc@tms.vn",
                "ngan.do@tms.vn", "vinh.vo@tms.vn", "vy.ho@tms.vn",
                "trong.phan@tms.vn", "tran.bao@tms.vn", "tai.nguyen@tms.vn"
              ]);

              const emailExists = knownEmails.has(toEmail) || toEmail.endsWith("@tms.vn") || toEmail.endsWith("@edumanager.vn");

              if (!emailExists) {
                console.log(`\x1b[33m[Forgot-Password] Email không tồn tại trong hệ thống: ${toEmail}\x1b[0m`);
                res.setHeader("Content-Type", "application/json");
                res.statusCode = 404;
                res.end(JSON.stringify({
                  success: false,
                  code: "EMAIL_NOT_FOUND",
                  message: "Email không tồn tại trong hệ thống. Vui lòng kiểm tra và nhập lại email khác.",
                }));
                return;
              }

              // Giới hạn 1 email chỉ gửi được 1 lần link sau 15 phút để tránh sập hệ thống (Rate Limiting / DoS protection)
              const lastSent = rateLimitStore.get(toEmail);
              if (lastSent && (Date.now() - lastSent < RATE_LIMIT_COOLDOWN_MS)) {
                const remainingMs = RATE_LIMIT_COOLDOWN_MS - (Date.now() - lastSent);
                const remainingMinutes = Math.ceil(remainingMs / 60000);
                const remainingSeconds = Math.ceil(remainingMs / 1000);
                console.log(`\x1b[33m[Rate Limit 15m] Email ${toEmail} bị giới hạn tần suất. Còn ${remainingMinutes} phút.\x1b[0m`);
                res.setHeader("Content-Type", "application/json");
                res.statusCode = 429;
                res.end(JSON.stringify({
                  success: false,
                  code: "RATE_LIMITED",
                  remainingSeconds,
                  remainingMinutes,
                  message: `Email này chỉ có thể nhận liên kết đặt lại mật khẩu 1 lần mỗi 15 phút để bảo vệ hệ thống. Vui lòng thử lại sau ${remainingMinutes} phút.`,
                }));
                return;
              }
              rateLimitStore.set(toEmail, Date.now());

              const startTime = Date.now();
              const token = "rst_" + Math.random().toString(36).substring(2, 10) + Date.now().toString(36);
              const expiresAt = Date.now() + 30 * 60 * 1000; // Đúng 30 phút

              resetTokenStore.set(token, {
                email: toEmail,
                expiresAt,
                isUsed: false,
                createdAt: Date.now(),
              });

              const resetLink = `http://localhost:5173/ResetPasswordForm.html?token=${token}`;
              console.log(`\x1b[36m[SMTP Gmail] Đang gửi liên kết đặt lại mật khẩu đến: ${toEmail}...\x1b[0m`);

              try {
                if (transporter) {
                  await transporter.sendMail({
                  from: '"TMS - Quản Lý Đào Tạo" <tatphi2006@gmail.com>',
                  to: toEmail,
                  subject: "[TMS] Đặt lại mật khẩu tài khoản của bạn",
                  text: `Xin chào,\n\nChúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với địa chỉ email: ${toEmail} trên Hệ thống Quản lý Đào tạo TMS.\n\nVui lòng truy cập đường dẫn sau để đặt lại mật khẩu mới:\n${resetLink}\n\nLƯU Ý QUAN TRỌNG (S1-03):\n• Liên kết có hiệu lực trong vòng 30 phút.\n• Liên kết chỉ sử dụng được duy nhất một lần.\n\nNếu bạn không yêu cầu đặt lại mật khẩu, xin hãy bỏ qua email này.\n\nTrân trọng,\nBan Quản trị Hệ thống TMS`,
                  html: `
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 560px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; box-shadow: 0 10px 25px rgba(0,0,0,0.06);">
                      <div style="background: linear-gradient(135deg, #0974f1 0%, #00f260 100%); padding: 32px 28px; text-align: center; color: #ffffff;">
                        <h1 style="margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.02em;">TMS. QUẢN LÝ ĐÀO TẠO</h1>
                        <p style="margin: 6px 0 0; font-size: 14px; opacity: 0.95;">Yêu cầu đặt lại mật khẩu tài khoản</p>
                      </div>
                      <div style="padding: 32px 28px; color: #334155;">
                        <p style="font-size: 15px; line-height: 1.6; margin-top: 0;">Xin chào,</p>
                        <p style="font-size: 15px; line-height: 1.6;">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email: <strong>${toEmail}</strong>.</p>
                        <p style="font-size: 15px; line-height: 1.6;">Nhấn vào nút bên dưới để tiến hành tạo mật khẩu mới:</p>
                        
                        <div style="text-align: center; margin: 30px 0;">
                          <a href="${resetLink}" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #0974f1 0%, #00d26a 100%); color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 700; padding: 14px 32px; border-radius: 10px; box-shadow: 0 4px 16px rgba(9, 116, 241, 0.35);">
                            Đặt lại mật khẩu ↗
                          </a>
                        </div>

                        <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; padding: 14px 18px; margin: 24px 0; font-size: 13px; line-height: 1.6; color: #475569;">
                          ⏱️ <strong>Hiệu lực liên kết:</strong> Có hiệu lực trong vòng <strong>30 phút</strong> và <strong>chỉ sử dụng được duy nhất một lần</strong>.<br/>
                          🔗 <strong>Đường dẫn trực tiếp:</strong><br/>
                          <a href="${resetLink}" style="color: #0974f1; word-break: break-all; font-size: 12px;">${resetLink}</a>
                        </div>

                        <p style="font-size: 13.5px; line-height: 1.6; color: #64748b;">Nếu bạn không thực hiện yêu cầu này, hãy yên tâm bỏ qua email. Tài khoản của bạn vẫn được an toàn.</p>
                        
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 28px 0 20px;" />
                        <p style="font-size: 12.5px; color: #94a3b8; text-align: center; margin: 0;">TMS Education Management System · Bảo mật tài khoản</p>
                      </div>
                    </div>
                  `,
                });
                console.log(`\x1b[32m[SMTP Gmail] Đã gửi liên kết khôi phục thành công đến: ${toEmail}\x1b[0m`);
              }
              } catch (emailErr) {
                console.warn("[SMTP Gmail Warning]", emailErr.message);
              }

              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({
                success: true,
                token: token,
                expiresAt: expiresAt,
                message: "Liên kết đặt lại mật khẩu đã được gửi đến email của bạn.",
              }));
            } catch (err) {
              console.error("[Forgot-Password Error]", err.message);
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({
                success: false,
                message: "Đã xảy ra lỗi trong quá trình xử lý yêu cầu. Vui lòng thử lại sau.",
              }));
            }
          });
          return;
        }

        // 2. Endpoint kiểm tra tính hợp lệ của Token đặt lại mật khẩu (S1-03 AC1 & AC2)
        if ((req.method === "GET" || req.method === "POST") && req.url.startsWith("/api/verify-token")) {
          const urlObj = new URL(req.url, "http://localhost");
          const token = urlObj.searchParams.get("token") || "";

          const record = resetTokenStore.get(token);
          const isSampleToken = token === "sample-test-token" || token === "demo-token";

          if (isSampleToken) {
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify({ success: true, email: "tatphi2006@gmail.com", remainingMinutes: 30 }));
            return;
          }

          if (!record) {
            res.setHeader("Content-Type", "application/json");
            res.statusCode = 400;
            res.end(JSON.stringify({ success: false, reason: "INVALID", message: "Liên kết không hợp lệ." }));
            return;
          }

          if (record.isUsed) {
            res.setHeader("Content-Type", "application/json");
            res.statusCode = 400;
            res.end(JSON.stringify({ success: false, reason: "ALREADY_USED", message: "Liên kết đã được sử dụng." }));
            return;
          }

          if (Date.now() > record.expiresAt) {
            res.setHeader("Content-Type", "application/json");
            res.statusCode = 400;
            res.end(JSON.stringify({ success: false, reason: "EXPIRED", message: "Liên kết đã hết hạn (quá 30 phút)." }));
            return;
          }

          res.setHeader("Content-Type", "application/json");
          res.end(JSON.stringify({
            success: true,
            email: record.email,
            remainingMinutes: Math.max(1, Math.ceil((record.expiresAt - Date.now()) / 60000)),
          }));
          return;
        }

        // 3. Endpoint cập nhật mật khẩu mới qua Reset Password (đánh dấu Token đã sử dụng - S1-03 AC2)
        if (req.method === "POST" && req.url === "/api/reset-password") {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", () => {
            try {
              const data = JSON.parse(body || "{}");
              const token = data.token;
              if (token && resetTokenStore.has(token)) {
                const record = resetTokenStore.get(token);
                if (record.isUsed) {
                  res.setHeader("Content-Type", "application/json");
                  res.statusCode = 400;
                  res.end(JSON.stringify({ success: false, reason: "ALREADY_USED", message: "Liên kết đã được sử dụng trước đó." }));
                  return;
                }
                if (Date.now() > record.expiresAt) {
                  res.setHeader("Content-Type", "application/json");
                  res.statusCode = 400;
                  res.end(JSON.stringify({ success: false, reason: "EXPIRED", message: "Liên kết đã hết hạn (quá 30 phút)." }));
                  return;
                }
                record.isUsed = true;
                record.usedAt = Date.now();
              }
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: true, message: "Mật khẩu đã được cập nhật thành công!" }));
            } catch (err) {
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 4. Endpoint gửi thông tin tài khoản và mật khẩu tạm khi Admin tạo mới (S1-08)
        if (req.method === "POST" && (req.url === "/api/send-email" || req.url === "/api/admin/users" || req.url === "/api/send-activation-email")) {
          let body = "";
          req.on("data", (chunk) => {
            body += chunk;
          });
          req.on("end", async () => {
            try {
              const data = JSON.parse(body || "{}");
              const toEmail = data.email || data.toEmail;
              const fullName = data.fullName || data.name || "Người dùng";
              const tempPassword = data.tempPassword || data.password || "";
              const loginLink = data.loginLink || "http://localhost:5173/login.html";

              if (toEmail && tempPassword) {
                if (transporter) {
                  await transporter.sendMail({
                  from: '"TMS - Quản Lý Đào Tạo" <tatphi2006@gmail.com>',
                  to: toEmail,
                  subject: "[TMS] Thông tin tài khoản và mật khẩu tạm thời",
                  text: `Xin chào ${fullName},

Tài khoản của bạn trên Hệ thống Quản lý Đào tạo TMS đã được tạo thành công bởi Quản trị viên.

Dưới đây là thông tin đăng nhập tạm thời:
• Tên đăng nhập / Email: ${toEmail}
• Mật khẩu tạm thời: ${tempPassword}

Vui lòng truy cập đường dẫn sau để đăng nhập:
${loginLink}

LƯU Ý QUAN TRỌNG:
Trong lần đăng nhập đầu tiên, hệ thống sẽ tự động chuyển bạn đến màn hình đổi mật khẩu để thiết lập mật khẩu riêng và bảo vệ tài khoản cá nhân.

Trân trọng,
Ban Quản trị Hệ thống TMS`,
                });
                console.log(`\x1b[32m[SMTP Gmail] Đã gửi email kích hoạt thành công đến: ${toEmail}\x1b[0m`);
                }
              }

              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({ success: true, message: "Email kích hoạt đã được gửi thành công!" }));
            } catch (err) {
              console.error("[SMTP Gmail Error]", err.message);
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 5. Endpoint cập nhật trạng thái Khóa / Mở khóa tài khoản (S1-10)
        if (req.method === "PUT" && req.url.includes("/api/admin/users/") && req.url.includes("/status")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", () => {
            try {
              const data = JSON.parse(body || "{}");
              const status = (data.status || "ACTIVE").toUpperCase();
              const lockedReason = data.lockedReason || null;
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({
                success: true,
                status: status.toLowerCase(),
                lockedReason,
                message: status === "ACTIVE" ? "Tài khoản đã được mở khóa thành công!" : "Tài khoản đã bị khóa thành công.",
                requiresHandover: status === "LOCKED",
                assignedClasses: status === "LOCKED" ? [
                  { code: "IELTS-2610", name: "IELTS Foundation & Intensive", role: "Giảng viên" }
                ] : [],
              }));
            } catch (err) {
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 6. Endpoint Đổi mật khẩu tài khoản (S1-04)
        if (req.method === "POST" && (req.url === "/api/auth/change-password" || req.url === "/auth/change-password")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", () => {
            try {
              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({
                success: true,
                message: "Đã đổi mật khẩu. Mật khẩu mới có hiệu lực ngay từ bây giờ.",
              }));
            } catch (err) {
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 7. Endpoint Tải tệp mẫu Excel / CSV (S2-01 AC1)
        if (req.method === "GET" && (req.url === "/api/admin/users/import/template" || req.url === "/api/users/import/template")) {
          const csvContent = "\uFEFFHọ và tên,Email,Số điện thoại,Vai trò,Ngày sinh,Giới tính,Địa chỉ\n"
            + "Nguyễn Văn An,an.nguyen@tms.vn,0912345678,HOC_VIEN,2003-05-15,Nam,Hà Nội\n"
            + "Trần Thị Bình,binh.tran@tms.vn,0987654321,HOC_VIEN,2002-11-20,Nữ,Thái Nguyên\n"
            + "Lê Hoàng Cường,cuong.le@tms.vn,0903112233,GIANG_VIEN,1990-08-10,Nam,Đà Nẵng\n";
          res.setHeader("Content-Type", "text/csv; charset=UTF-8");
          res.setHeader("Content-Disposition", 'attachment; filename="mau_nhap_nguoi_dung_tms.csv"');
          res.end(csvContent);
          return;
        }

        // 8. Endpoint Xem trước và báo lỗi từng dòng tệp Excel (S2-01 AC2)
        if (req.method === "POST" && (req.url === "/api/admin/users/import/preview" || req.url === "/api/users/import/preview")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", () => {
            try {
              const data = JSON.parse(body || "{}");
              const rows = Array.isArray(data.rows) ? data.rows : [];
              const vnPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
              const seenEmails = new Set();

              let validRows = 0;
              let errorRows = 0;

              const evaluated = rows.map((r, idx) => {
                const rowNum = idx + 1;
                const errors = [];
                const fullName = (r.fullName || r.name || "").trim();
                const email = (r.email || "").trim().toLowerCase();
                const phone = (r.phone || "").trim().replace(/[\s.-]/g, "");

                if (!fullName) errors.push("Thiếu họ và tên");
                if (!email) {
                  errors.push("Thiếu địa chỉ email");
                } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                  errors.push("Định dạng email không hợp lệ");
                } else if (seenEmails.has(email)) {
                  errors.push("Email bị trùng lặp trong tệp tải lên");
                } else {
                  seenEmails.add(email);
                  // Check existing demo emails
                  if (email === "hoangnam@tms.vn" || email === "admin@tms.vn" || email === "minhanh@tms.vn" || email === "thuha@tms.vn") {
                    errors.push(`Email này đã được dùng cho tài khoản ${r.fullName || "người dùng khác"}`);
                  }
                }

                if (phone && !vnPhoneRegex.test(phone)) {
                  errors.push("Số điện thoại không đúng chuẩn di động VN (10 số, bắt đầu 03, 05, 07, 08, 09)");
                }

                const validRoles = ["student", "instructor", "ta", "training-manager", "admissions", "accountant", "admin"];
                const role = (r.role || "student").trim().toLowerCase();
                if (!validRoles.includes(role)) {
                  errors.push(`Vai trò '${role}' không tồn tại trong hệ thống`);
                }

                const isValid = errors.length === 0;
                if (isValid) validRows++; else errorRows++;

                return {
                  ...r,
                  rowIndex: rowNum,
                  isValid,
                  errors,
                  errorMessage: errors.join(", "),
                };
              });

              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({
                success: true,
                totalRows: rows.length,
                validRows,
                errorRows,
                rows: evaluated,
              }));
            } catch (err) {
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 9. Endpoint Nhập danh sách tài khoản hàng loạt & Báo cáo tổng kết (S2-01 AC3)
        if (req.method === "POST" && (req.url === "/api/admin/users/import" || req.url === "/api/users/import")) {
          let body = "";
          req.on("data", (chunk) => { body += chunk; });
          req.on("end", () => {
            try {
              const data = JSON.parse(body || "{}");
              const rows = Array.isArray(data.rows) ? data.rows : [];
              const fileName = data.fileName || "danh_sach_nguoi_dung.xlsx";
              const vnPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
              const seenEmails = new Set();

              const successUsers = [];
              const errorItems = [];

              rows.forEach((r, idx) => {
                const rowIdx = idx + 1;
                const fullName = (r.fullName || r.name || "").trim();
                const email = (r.email || "").trim().toLowerCase();
                const phone = (r.phone || "").trim().replace(/[\s.-]/g, "");
                const role = (r.role || "student").toLowerCase();

                let error = null;
                if (!fullName) error = "Thiếu họ và tên";
                else if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) error = "Email không hợp lệ hoặc bị trống";
                else if (seenEmails.has(email)) error = "Email bị trùng lặp trong tệp";
                else if (phone && !vnPhoneRegex.test(phone)) error = "Số điện thoại không đúng chuẩn di động VN";
                else if (!["student", "instructor", "ta", "training-manager", "admissions", "accountant", "admin"].includes(role)) error = `Vai trò '${role}' không tồn tại trong hệ thống`;

                if (error) {
                  errorItems.push({ rowIndex: rowIdx, fullName, email, reason: error });
                } else {
                  seenEmails.add(email);
                  const tempPassword = "Edu@" + Math.random().toString(36).substring(2, 8) + "9";
                  successUsers.push({
                    id: Date.now() + idx,
                    rowIndex: rowIdx,
                    userCode: "HV" + (Math.floor(100000 + Math.random() * 900000)),
                    fullName,
                    email,
                    phone: phone || "",
                    role,
                    tempPassword,
                  });
                }
              });

              res.setHeader("Content-Type", "application/json");
              res.end(JSON.stringify({
                success: true,
                batchCode: "IMP-" + Date.now(),
                fileName,
                totalRows: rows.length,
                successRows: successUsers.length,
                failedRows: errorItems.length,
                successUsers,
                errors: errorItems,
              }));
            } catch (err) {
              res.setHeader("Content-Type", "application/json");
              res.statusCode = 500;
              res.end(JSON.stringify({ success: false, error: err.message }));
            }
          });
          return;
        }

        // 10. Endpoint Lấy và Cập nhật hồ sơ cá nhân (S2-02)
        if (req.url === "/api/profile" || req.url === "/api/user/profile") {
          if (req.method === "GET") {
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify({
              success: true,
              data: {
                fullName: "Trần Quốc Bảo",
                email: "quocbao@tms.vn",
                phone: "0918200300",
                dateOfBirth: "1992-06-15",
                gender: "MALE",
                address: "Tổ 10, Phường Quang Trung, TP Thái Nguyên",
                role: "admin",
                roles: ["admin"],
              },
            }));
            return;
          }

          if (req.method === "PUT" || req.method === "POST") {
            let body = "";
            req.on("data", (chunk) => { body += chunk; });
            req.on("end", () => {
              try {
                const data = JSON.parse(body || "{}");
                const vnPhoneRegex = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

                // Kiểm tra định dạng số điện thoại Việt Nam (S2-02 AC3)
                const phone = (data.phone || "").trim().replace(/[\s.-]/g, "");
                if (phone && !vnPhoneRegex.test(phone)) {
                  res.setHeader("Content-Type", "application/json");
                  res.statusCode = 400;
                  res.end(JSON.stringify({
                    success: false,
                    code: "INVALID_PHONE_FORMAT",
                    message: "Số điện thoại không đúng định dạng di động Việt Nam (gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08, 09).",
                  }));
                  return;
                }

                if (!data.fullName || !data.fullName.trim()) {
                  res.setHeader("Content-Type", "application/json");
                  res.statusCode = 400;
                  res.end(JSON.stringify({
                    success: false,
                    code: "NAME_REQUIRED",
                    message: "Họ và tên không được để trống.",
                  }));
                  return;
                }

                // Bảo vệ email và vai trò (S2-02 AC2): không thay đổi email và role
                res.setHeader("Content-Type", "application/json");
                res.end(JSON.stringify({
                  success: true,
                  message: "Cập nhật hồ sơ cá nhân thành công!",
                  data: {
                    fullName: data.fullName.trim(),
                    phone,
                    dateOfBirth: data.dateOfBirth || "",
                    gender: data.gender || "OTHER",
                    address: data.address || "",
                  },
                }));
              } catch (err) {
                res.setHeader("Content-Type", "application/json");
                res.statusCode = 500;
                res.end(JSON.stringify({ success: false, error: err.message }));
              }
            });
            return;
          }
        }
        next();
      });
    },
  };
}

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
  "import-users-prototype.html",
];

export default defineConfig({
  plugins: [emailPlugin()],
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

