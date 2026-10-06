import os
import re

FRONTEND_DIR = r"d:\TTCS\ICTU-DUAN-Trung-Tam-X\ICTU-DUAN-Trung-Tam-X\frontend"

ACTOR_PAGES = [
    ("Quản trị viên (admin)", "admin.html"),
    ("Quản lý đào tạo (training-manager)", "training-manager.html"),
    ("Giảng viên (instructor)", "instructor.html"),
    ("Kế toán (accountant)", "accountant.html"),
    ("Học viên (student)", "student.html"),
    ("Tư vấn tuyển sinh (admissions)", "admissions.html"),
    ("Trợ giảng (ta)", "ta.html"),
]

REQUIRED_SELECTORS = [
    ("Nút Topbar 'Đổi mật khẩu'", r'id="topbar-change-pass-btn"'),
    ("Menu item Dropdown 'Đổi mật khẩu'", r'id="open-change-password-modal"'),
    ("Modal container", r'id="change-password-modal"'),
    ("Form đổi mật khẩu", r'id="change-password-form"'),
    ("Input Mật khẩu hiện tại", r'id="cp-current"'),
    ("Input Mật khẩu mới", r'id="cp-new"'),
    ("Input Xác nhận mật khẩu", r'id="cp-confirm"'),
    ("4 tiêu chí mật khẩu (Live validation)", r'id="cp-rule-length"'),
    ("Nút Cập nhật mật khẩu", r'id="cp-submit-btn"'),
    ("Màn hình thông báo Hoàn tất", r'id="cp-success-view"'),
]

print("=" * 80)
print("KIỂM TRA CHỨC NĂNG ĐỔI MẬT KHẨU TRÊN TẤT CẢ 7 ACTOR")
print("=" * 80)

all_ok = True
for role_name, filename in ACTOR_PAGES:
    path = os.path.join(FRONTEND_DIR, filename)
    assert os.path.exists(path), f"File {filename} does not exist!"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    print(f"\n kiểm tra actor: {role_name} ({filename})")
    role_ok = True
    for desc, pattern in REQUIRED_SELECTORS:
        if re.search(pattern, content):
            print(f"  [PASS] {desc}")
        else:
            print(f"  [FAIL] THIẾU: {desc} (Pattern: {pattern})")
            role_ok = False
            all_ok = False

# Kiểm tra logic trong app.js
app_js_path = os.path.join(FRONTEND_DIR, "app.js")
with open(app_js_path, "r", encoding="utf-8") as f:
    app_js = f.read()

print("\n" + "=" * 80)
print("KIỂM TRA LOGIC JAVASCRIPT HỖ TRỢ ĐA VAI TRÒ (app.js)")
print("=" * 80)

checks = [
    ("Hàm verifyCurrentPassword hỗ trợ đa vai trò", r"verifyCurrentPassword = \(entered\) =>"),
    ("Khớp tài khoản theo DEMO_ACCOUNTS và customAccounts", r"allAccounts\.find"),
    ("Lưu mật khẩu mới cho cả email và userRole", r"customPasswords\[userRole\] = newPassword"),
    ("Kích hoạt setupChangePasswordModal tự động", r"setupChangePasswordModal\(\);"),
]

for desc, pattern in checks:
    if re.search(pattern, app_js):
        print(f"  [PASS] {desc}")
    else:
        print(f"  [FAIL] {desc}")
        all_ok = False

print("\n" + "=" * 80)
if all_ok:
    print(" KẾT QUẢ: TẤT CẢ 7 ACTOR ĐỀU ĐÃ ĐƯỢC TRANG BỊ ĐẦY ĐỦ GIAO DIỆN VÀ LOGIC ĐỔI MẬT KHẨU!")
else:
    print("❌ KẾT QUẢ: CÒN MỘT SỐ PHẦN TỬ CHƯA ĐẠT!")
print("=" * 80)
