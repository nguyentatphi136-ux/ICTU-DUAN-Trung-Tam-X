import sys
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

SMTP_HOST = "smtp.gmail.com"
SMTP_PORT = 587
USERNAME = "tatphi2006@gmail.com"
PASSWORD = "tcjo toxt khfw yldn".replace(" ", "")

msg = MIMEMultipart()
msg["From"] = USERNAME
msg["To"] = USERNAME
msg["Subject"] = "[TMS] Kiem tra ket noi email Gmail SMTP"

body = """Xin chao,
Day la email kiem tra ket noi tu He thong Quan ly Dao tao TMS.
Tai khoan: tatphi2006@gmail.com
Mat khau tam: Test@123456
"""
msg.attach(MIMEText(body, "plain", "utf-8"))

try:
    server = smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=15)
    server.starttls()
    server.login(USERNAME, PASSWORD)
    server.send_message(msg)
    server.quit()
    print("SUCCESS: Gui email thanh cong qua Gmail SMTP!")
except Exception as e:
    print("FAILED:", str(e).encode("ascii", "replace").decode("ascii"))
