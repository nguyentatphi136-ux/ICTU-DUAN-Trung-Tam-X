import sys
import json
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SMTP_HOST = "smtp.gmail.com"
SMTP_PORT = 587
USERNAME = "tatphi2006@gmail.com"
PASSWORD = "tcjo toxt khfw yldn".replace(" ", "")

def send_activation(to_email, full_name, login_email, temp_password, role_name="Học viên"):
    try:
        if not to_email or "@" not in to_email:
            return {"success": False, "error": "Địa chỉ email không hợp lệ"}

        msg = MIMEMultipart("alternative")
        msg["From"] = f"TMS - Quản Lý Đào Tạo <{USERNAME}>"
        msg["To"] = to_email
        msg["Subject"] = f"[TMS] Thông tin tài khoản và mật khẩu tạm thời ({role_name})"
        
        text_content = f"""Xin chào {full_name},

Tài khoản của bạn trên Hệ thống Quản lý Đào tạo TMS đã được khởi tạo thành công với vai trò {role_name}.

Thông tin đăng nhập:
- Tên đăng nhập / Email: {login_email}
- Mật khẩu tạm thời: {temp_password}
- Đường dẫn đăng nhập: http://localhost:5173/dang-nhap

LƯU Ý: Vui lòng đăng nhập và đổi mật khẩu mới trong lần đầu tiên.

Trân trọng,
Ban Quản trị Hệ thống TMS
"""
        html_content = f"""
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;">
          <div style="text-align: center; margin-bottom: 20px;">
            <h2 style="color: #2563eb; margin: 0;">HỆ THỐNG QUẢN LÝ ĐÀO TẠO TMS</h2>
            <p style="color: #64748b; font-size: 14px; margin-top: 4px;">Thông báo cấp tài khoản người dùng</p>
          </div>
          <p>Xin chào <strong>{full_name}</strong>,</p>
          <p>Tài khoản của bạn trên Hệ thống Quản lý Đào tạo TMS đã được khởi tạo thành công với vai trò <strong>{role_name}</strong>.</p>
          <div style="background-color: #f8fafc; border: 1px solid #cbd5e1; border-radius: 6px; padding: 16px; margin: 20px 0;">
            <p style="margin: 6px 0;">📧 <strong>Tên đăng nhập / Email:</strong> <code style="color: #2563eb; font-size: 15px; font-weight: bold;">{login_email}</code></p>
            <p style="margin: 6px 0;">🔑 <strong>Mật khẩu tạm thời:</strong> <code style="color: #dc2626; font-size: 15px; font-weight: bold;">{temp_password}</code></p>
            <p style="margin: 6px 0;">🌐 <strong>Đường dẫn đăng nhập:</strong> <a href="http://localhost:5173/dang-nhap" style="color: #2563eb; font-weight: bold;">http://localhost:5173/dang-nhap</a></p>
          </div>
          <div style="background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 12px; margin: 16px 0; font-size: 13px; color: #991b1b;">
            <strong>LƯU Ý QUAN TRỌNG:</strong> Đây là mật khẩu tạm thời. Khi đăng nhập lần đầu tiên, hệ thống sẽ yêu cầu bạn đổi mật khẩu mới để bảo mật tài khoản cá nhân.
          </div>
          <p style="font-size: 13px; color: #64748b; margin-top: 24px;">Nếu bạn không yêu cầu tài khoản này, vui lòng bỏ qua email này hoặc liên hệ Quản trị viên.</p>
          <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;" />
          <p style="text-align: center; font-size: 12px; color: #94a3b8;">Ban Quản trị Hệ thống TMS &copy; 2026 ICTU - CodeGym</p>
        </div>
        """
        msg.attach(MIMEText(text_content, "plain", "utf-8"))
        msg.attach(MIMEText(html_content, "html", "utf-8"))
        
        server = smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=15)
        server.starttls()
        server.login(USERNAME, PASSWORD)
        server.send_message(msg)
        server.quit()
        return {"success": True, "message": f"Đã gửi email thành công tới {to_email}"}
    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) >= 5:
        to_email = sys.argv[1]
        full_name = sys.argv[2]
        login_email = sys.argv[3]
        temp_password = sys.argv[4]
        role_name = sys.argv[5] if len(sys.argv) > 5 else "Học viên"
        res = send_activation(to_email, full_name, login_email, temp_password, role_name)
        print(json.dumps(res, ensure_ascii=False))
    elif not sys.stdin.isatty():
        try:
            stdin_data = sys.stdin.read().strip()
            if stdin_data:
                data = json.loads(stdin_data)
                res = send_activation(
                    to_email=data.get("toEmail", ""),
                    full_name=data.get("fullName", "Học viên"),
                    login_email=data.get("loginEmail", ""),
                    temp_password=data.get("tempPassword", ""),
                    role_name=data.get("roleName", "Học viên")
                )
                print(json.dumps(res, ensure_ascii=False))
            else:
                print(json.dumps({"success": False, "error": "Dữ liệu trống"}))
        except Exception as e:
            print(json.dumps({"success": False, "error": str(e)}))
    else:
        print(json.dumps({"success": False, "error": "Thiếu tham số dòng lệnh hoặc stdin"}))
