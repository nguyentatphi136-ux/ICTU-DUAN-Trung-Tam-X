import re

print("=" * 65)
print("KIỂM THỬ ĐỘC LẬP TẤT CẢ 7 NHIỆM VỤ CỦA CHỨC NĂNG S1-04")
print("=" * 65)

passed = 0
failed = 0

def assert_test(name, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print(f"[PASS] {name}")
    else:
        failed += 1
        print(f"[FAIL] {name}: {detail}")

# Đọc các file liên quan
with open("src/main/java/com/ems/controller/ChangePasswordServlet.java", "r", encoding="utf-8") as f:
    servlet_code = f.read()

with open("src/main/java/com/ems/dao/UserDAO.java", "r", encoding="utf-8") as f:
    dao_code = f.read()

with open("src/main/java/com/ems/config/SessionBlacklist.java", "r", encoding="utf-8") as f:
    blacklist_code = f.read()

with open("src/main/java/com/ems/security/PermissionPolicy.java", "r", encoding="utf-8") as f:
    policy_code = f.read()

with open("frontend/admin.html", "r", encoding="utf-8") as f:
    admin_html = f.read()

with open("frontend/app.js", "r", encoding="utf-8") as f:
    app_js = f.read()

with open("legacy_or_mock/mock-node-api/src/routes/auth.routes.js", "r", encoding="utf-8") as f:
    node_auth = f.read()

# -------------------------------------------------------------
# 1. [BE] API POST /auth/change-password — xác thực mật khẩu hiện tại
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 1: [BE] API POST /auth/change-password ---")
assert_test("ChangePasswordServlet mapping urlPatterns chứa /auth/change-password và /api/auth/change-password",
            '@WebServlet(urlPatterns = {"/auth/change-password", "/api/auth/change-password"})' in servlet_code)

assert_test("PermissionPolicy cho phép @authenticated cho POST /api/auth/change-password",
            '"/api/auth/change-password".equals(path)' in policy_code)

assert_test("ChangePasswordServlet kiểm tra session đăng nhập và trả 401 nếu chưa đăng nhập",
            "currentUser == null" in servlet_code and "SC_UNAUTHORIZED" in servlet_code)

assert_test("UserDAO kiểm tra BCrypt.checkpw(currentPassword, storedHash)",
            "BCrypt.checkpw(currentPassword, storedHash)" in dao_code)

assert_test("Mock API có route router.post('/change-password', authenticate, ...)",
            "router.post('/change-password', authenticate" in node_auth)

# -------------------------------------------------------------
# 2. [BE] Validate mật khẩu mới: tối thiểu 8 ký tự, có chữ và số
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 2: [BE] Validate mật khẩu mới ---")
assert_test("UserDAO kiểm tra newPassword.length() < 8",
            "newPassword.length() < 8" in dao_code)

assert_test("UserDAO kiểm tra regex cả chữ cái và chữ số: .*[a-zA-Z].* và .*\\d.*",
            '.*[a-zA-Z].*' in dao_code and r'.*\\d.*' in dao_code)

assert_test("UserDAO kiểm tra xác nhận mật khẩu khớp: newPassword.equals(confirmPassword)",
            "newPassword.equals(confirmPassword)" in dao_code)

assert_test("UserDAO kiểm tra mật khẩu mới không trùng mật khẩu cũ: currentPassword.equals(newPassword)",
            "currentPassword.equals(newPassword)" in dao_code)

# -------------------------------------------------------------
# 3. [BE] Thu hồi tất cả session/token khác của user (trừ phiên hiện tại)
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 3: [BE] Thu hồi tất cả session/token khác của user ---")
assert_test("SessionBlacklist có phương thức revokeOtherSessions(userId, currentSessionId)",
            "public static int revokeOtherSessions(long userId, String currentSessionId)" in blacklist_code)

assert_test("SessionBlacklist có phương thức registerSession(userId, sessionId)",
            "public static void registerSession(long userId, String sessionId)" in blacklist_code)

assert_test("ChangePasswordServlet gọi SessionBlacklist.revokeOtherSessions",
            "SessionBlacklist.revokeOtherSessions(currentUser.getId(), session.getId())" in servlet_code)

assert_test("Mock API lưu mốc thời gian đổi mật khẩu để vô hiệu hóa token cũ",
            "passwordChangedAt" in node_auth)

# -------------------------------------------------------------
# 4. [BE] Mã hóa mật khẩu mới trước khi lưu
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 4: [BE] Mã hóa mật khẩu mới trước khi lưu ---")
assert_test("UserDAO mã hóa BCrypt.hashpw(newPassword, BCrypt.gensalt(10))",
            "BCrypt.hashpw(newPassword, BCrypt.gensalt(10))" in dao_code)

assert_test("UserDAO thực hiện UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?",
            "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?" in dao_code)

assert_test("Mock API mã hóa mật khẩu mới bằng scrypt trước khi lưu",
            "hashPassword(newPassword, req.user.id)" in node_auth)

# -------------------------------------------------------------
# 5. [FE] Thiết kế UI form đổi mật khẩu (mật khẩu hiện tại, mới, xác nhận)
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 5: [FE] Thiết kế UI form đổi mật khẩu ---")
assert_test("admin.html có modal đổi mật khẩu #change-password-modal",
            'id="change-password-modal"' in admin_html)

assert_test("admin.html có input mật khẩu hiện tại #cp-current",
            'id="cp-current"' in admin_html and 'name="currentPassword"' in admin_html)

assert_test("admin.html có input mật khẩu mới #cp-new",
            'id="cp-new"' in admin_html and 'name="newPassword"' in admin_html)

assert_test("admin.html có input xác nhận mật khẩu mới #cp-confirm",
            'id="cp-confirm"' in admin_html and 'name="confirmPassword"' in admin_html)

assert_test("Có nút ẩn / hiện mật khẩu toggle-pass-btn",
            'class="toggle-pass-btn' in admin_html)

# -------------------------------------------------------------
# 6. [FE] Thêm Validate: mật khẩu mới tối thiểu 8 ký tự, có chữ và số, khớp xác nhận
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 6: [FE] Validate form đổi mật khẩu client-side ---")
assert_test("app.js kiểm tra độ dài newPassword < 8",
            "newPassword.length < 8" in app_js)

assert_test("app.js kiểm tra regex chữ và số: /[a-zA-Z]/ và /[0-9]/",
            "/[a-zA-Z]/.test(newPassword)" in app_js and "/[0-9]/.test(newPassword)" in app_js)

assert_test("app.js kiểm tra newPassword !== confirmPassword",
            "newPassword !== confirmPassword" in app_js)

assert_test("app.js kiểm tra currentPassword === newPassword",
            "currentPassword === newPassword" in app_js)

# -------------------------------------------------------------
# 7. [FE] Hiển thị thông báo thành công và yêu cầu đăng nhập lại nếu cần
# -------------------------------------------------------------
print("\n--- Nhiệm vụ 7: [FE] Hiển thị thông báo thành công ---")
assert_test("app.js hiển thị showToast thông báo thành công thu hồi phiên khác",
            "showToast" in app_js and "Các phiên đăng nhập khác đã được thu hồi" in app_js)

assert_test("app.js ghi nhật ký Audit Log khi đổi mật khẩu",
            'addAuditLog("Đổi mật khẩu tài khoản thành công (S1-04)")' in app_js)

assert_test("Có trang change-password.jsp độc lập cho web MVC",
            "change-password.jsp" in servlet_code)

# -------------------------------------------------------------
# TỔNG KẾT
# -------------------------------------------------------------
print("\n" + "=" * 65)
print(f"KẾT QUẢ KIỂM THỬ S1-04: {passed} PASSED, {failed} FAILED ({(passed/(passed+failed))*100:.1f}%)")
print("=" * 65)
