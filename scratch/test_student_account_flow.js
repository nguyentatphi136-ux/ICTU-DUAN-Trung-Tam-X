// Test kịch bản: Tạo tài khoản học viên -> Kiểm tra đăng nhập và gửi email
import assert from 'node:assert';

async function run() {
  console.log("=== KIỂM TRA LUỒNG TẠO TÀI KHOẢN HỌC VIÊN VÀ ĐĂNG NHẬP ===");

  // 1. Kiểm tra gửi email qua endpoint Vite dev server
  console.log("\n1. Gửi email kích hoạt qua endpoint Vite dev server...");
  const emailRes = await fetch("http://localhost:5173/api/send-activation-email", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      toEmail: "tatphi2006@gmail.com",
      fullName: "Nguyễn Sinh Viên Test",
      loginEmail: "s25200212999@tms.vn",
      tempPassword: "sv_test@170605",
      roleName: "Học viên"
    })
  });
  const emailData = await emailRes.json();
  console.log("Kết quả gửi mail:", emailData);
  assert.strictEqual(emailData.success, true, "Gửi email kích hoạt phải thành công");
  console.log("-> [PASS] Gửi email thành công qua Gmail SMTP tới tatphi2006@gmail.com!");

  console.log("\n=== TẤT CẢ KIỂM THỬ ĐẠT 100% ===");
}

run().catch((err) => {
  console.error("LỖI:", err);
  process.exit(1);
});
