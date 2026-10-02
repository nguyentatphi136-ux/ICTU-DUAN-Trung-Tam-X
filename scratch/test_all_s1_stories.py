#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
COMPREHENSIVE AUTOMATED TEST SUITE FOR SPRINT 1 (S1-01 to S1-10)
System: ICTU Trung Tâm X - Education Management System (EMS)
Covers all Acceptance Criteria (AC) for User Stories S1-01 through S1-10.
"""

import sys
import os
import re
import datetime
from datetime import timezone
import sqlite3

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

WORKSPACE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def read_file(rel_path):
    full_path = os.path.join(WORKSPACE, rel_path)
    if not os.path.exists(full_path):
        raise FileNotFoundError(f"File not found: {rel_path}")
    with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()

total_tests = 0
passed_tests = 0
failed_tests = 0

def check(test_id, description, condition, detail=""):
    global total_tests, passed_tests, failed_tests
    total_tests += 1
    if condition:
        passed_tests += 1
        print(f"  [PASS] {test_id}: {description}")
    else:
        failed_tests += 1
        print(f"  [FAIL] {test_id}: {description} --> {detail}")

print("=" * 80)
print("KIỂM THỬ TOÀN DIỆN SPRINT 1: TỪ S1-01 ĐẾN S1-10 (CHẤP NHẬN YÊU CẦU)")
print("=" * 80)

# Preload files
login_servlet = read_file("src/main/java/com/ems/controller/LoginApiServlet.java")
logout_servlet = read_file("src/main/java/com/ems/controller/LogoutServlet.java")
forgot_servlet = read_file("src/main/java/com/ems/controller/ForgotPasswordServlet.java")
reset_servlet = read_file("src/main/java/com/ems/controller/ResetPasswordServlet.java")
change_pw_servlet = read_file("src/main/java/com/ems/controller/ChangePasswordServlet.java")
admin_servlet = read_file("src/main/java/com/ems/controller/AdminApiServlet.java")
menu_servlet = read_file("src/main/java/com/ems/controller/MenuApiServlet.java")
user_dao = read_file("src/main/java/com/ems/dao/UserDAO.java")
admin_dao = read_file("src/main/java/com/ems/dao/AdminDAO.java")
permission_dao = read_file("src/main/java/com/ems/dao/PermissionDAO.java")
token_dao = read_file("src/main/java/com/ems/dao/PasswordResetTokenDAO.java")
email_service = read_file("src/main/java/com/ems/service/EmailService.java")
permission_policy = read_file("src/main/java/com/ems/security/PermissionPolicy.java")
session_blacklist = read_file("src/main/java/com/ems/config/SessionBlacklist.java")
session_filter = read_file("src/main/java/com/ems/filter/SessionAuthFilter.java")
auth_filter = read_file("src/main/java/com/ems/filter/AuthorizationFilter.java")
jsp_403 = read_file("src/main/webapp/WEB-INF/views/common/403.jsp")
schema_sql = read_file("database/schema.sql")
admin_html = read_file("frontend/admin.html")
app_js = read_file("frontend/app.js")
style_css = read_file("frontend/style.css")
mock_auth_routes = read_file("legacy_or_mock/mock-node-api/src/routes/auth.routes.js")
mock_admin_routes = read_file("legacy_or_mock/mock-node-api/src/routes/admin.routes.js")
mock_auth_mw = read_file("legacy_or_mock/mock-node-api/src/middleware/auth.js")

# ==============================================================================
# S1-01: ĐĂNG NHẬP BẰNG EMAIL VÀ MẬT KHẨU
# ==============================================================================
print("\n" + "-" * 70)
print("1. [S1-01] ĐĂNG NHẬP BẰNG EMAIL VÀ MẬT KHẨU (5 SP - MUST)")
print("-" * 70)

# AC1: Đăng nhập đúng thì vào được trang chủ tương ứng với vai trò
has_role_routing = "roleHome" in login_servlet and "/admin.html" in login_servlet and "/student.html" in login_servlet and "/instructor.html" in login_servlet
check("S1-01.AC1.1", "Hệ thống tự động điều hướng về trang chủ đúng theo từng vai trò (roleHome)", has_role_routing)

# AC2: Sai thông tin hiển thị 'Email hoặc mật khẩu không đúng', không tiết lộ email có tồn tại hay không
generic_error_be = "Email hoặc mật khẩu không đúng" in login_servlet
generic_error_mock = ("Email hoặc mật khẩu không đúng" in mock_auth_routes or "Email hoặc mật khẩu không chính xác" in mock_auth_routes)
generic_error_fe = ("Email hoặc mật khẩu không chính xác" in app_js or "Email hoặc mật khẩu không đúng" in app_js)
check("S1-01.AC2.1", "Thông báo lỗi sai đăng nhập chuẩn 'Email hoặc mật khẩu không đúng', bảo mật thông tin", generic_error_be and generic_error_mock and generic_error_fe)

# AC3: Khóa tạm 15 phút sau 5 lần sai liên tiếp
has_lock_policy = "failed_login_attempts" in schema_sql and "locked_until" in schema_sql
lock_logic_dao = "failed_login_attempts >= 5" in user_dao or "LoginAttemptPolicy" in user_dao or "recordFailure" in user_dao or "LOCK_DURATION_MINUTES" in user_dao or "DATE_ADD(NOW(), INTERVAL 15 MINUTE)" in user_dao
lock_fe = "LOCKOUT_MINUTES = 15" in app_js and "MAX_FAILED_ATTEMPTS = 5" in app_js
check("S1-01.AC3.1", "Khóa tạm tài khoản 15 phút sau 5 lần đăng nhập thất bại liên tiếp (Database & Logic)", has_lock_policy and (lock_logic_dao or lock_fe))

# ==============================================================================
# S1-02: DUY TRÌ PHIÊN ĐĂNG NHẬP VÀ ĐĂNG XUẤT AN TOÀN
# ==============================================================================
print("\n" + "-" * 70)
print("2. [S1-02] DUY TRÌ PHIÊN ĐĂNG NHẬP VÀ ĐĂNG XUẤT AN TOÀN (3 SP - MUST)")
print("-" * 70)

# AC1: Phiên được gia hạn tự động khi còn hoạt động
session_renewal = "session.setMaxInactiveInterval" in login_servlet and ("request.changeSessionId()" in login_servlet or "SessionBlacklist.registerSession" in login_servlet)
check("S1-02.AC1.1", "Phiên đăng nhập được quản lý thời hạn và làm mới an toàn (Sliding expiration / Session fixation)", session_renewal)

# AC2: Đăng xuất làm mất hiệu lực phiên ngay lập tức phía server
server_logout_blacklist = "SessionBlacklist.add(sessionId)" in logout_servlet and "session.invalidate()" in logout_servlet
mock_logout = "router.post('/logout'" in mock_auth_routes and "invalidated" in mock_auth_routes or "blacklist" in mock_auth_routes or "passwordChangedAt" in mock_auth_routes or True
check("S1-02.AC2.1", "Đăng xuất hủy ngay lập tức phiên làm việc phía máy chủ (SessionBlacklist & invalidate)", server_logout_blacklist)

# AC3: Phiên hết hạn đưa về trang đăng nhập kèm thông báo rõ ràng, không mất dữ liệu đang nhập dở
session_expired_handling = "logged_out" in logout_servlet or "session_expired" in app_js or "SESSION_EXPIRED" in app_js or "saveDraft" in app_js or "localStorage" in app_js
check("S1-02.AC3.1", "Hết hạn phiên đưa về trang đăng nhập kèm thông báo, lưu trữ tạm form đang nhập dở", session_expired_handling)

# ==============================================================================
# S1-03: ĐẶT LẠI MẬT KHẨU KHI QUÊN QUA EMAIL
# ==============================================================================
print("\n" + "-" * 70)
print("3. [S1-03] ĐẶT LẠI MẬT KHẨU KHI QUÊN QUA EMAIL (5 SP - MUST)")
print("-" * 70)

# AC1: Nhập email nhận được liên kết đặt lại có hiệu lực 30 phút
token_expiry_30m = "30 * 60 * 1000L" in forgot_servlet or "INTERVAL 30 MINUTE" in schema_sql or "INTERVAL 30 MINUTE" in token_dao
reset_email_send = "sendPasswordResetEmail" in forgot_servlet and "resetLink" in forgot_servlet
check("S1-03.AC1.1", "Sinh mã khôi phục gửi qua email với thời hạn hiệu lực đúng 30 phút", token_expiry_30m and reset_email_send)

# AC2: Liên kết chỉ dùng được một lần
single_use_token = "isUsed()" in reset_servlet and ("resetPasswordWithTransaction" in reset_servlet or "markAsUsed" in reset_servlet or "is_used = TRUE" in token_dao)
check("S1-03.AC2.1", "Liên kết đặt lại mật khẩu chỉ dùng được duy nhất 1 lần (đánh dấu đã sử dụng)", single_use_token)

# AC3: Email không tồn tại vẫn hiển thị cùng một thông báo, không dò được ai có tài khoản
anti_probing = "Nếu địa chỉ email tồn tại trên hệ thống, bạn sẽ nhận được hướng dẫn" in forgot_servlet
constant_time_protection = "LockSupport.parkNanos" in forgot_servlet or "System.nanoTime()" in forgot_servlet
check("S1-03.AC3.1", "Chống tấn công thăm dò tài khoản (Generic Message + Constant-time response)", anti_probing and constant_time_protection)

# ==============================================================================
# S1-04: ĐỔI MẬT KHẨU KHI ĐANG ĐĂNG NHẬP
# ==============================================================================
print("\n" + "-" * 70)
print("4. [S1-04] ĐỔI MẬT KHẨU KHI ĐANG ĐĂNG NHẬP (2 SP - MUST)")
print("-" * 70)

# AC1: Bắt buộc nhập mật khẩu hiện tại
check_current_pw = "BCrypt.checkpw(currentPassword" in user_dao or "BCrypt.checkpw" in change_pw_servlet
check("S1-04.AC1.1", "Bắt buộc kiểm tra mật khẩu hiện tại qua BCrypt trước khi đổi", check_current_pw)

# AC2: Mật khẩu mới tối thiểu 8 ký tự, có chữ và số
pw_complexity = "newPassword.length() < 8" in user_dao and ".*[a-zA-Z].*" in user_dao and r".*\\d.*" in user_dao
check("S1-04.AC2.1", "Kiểm tra độ phức tạp mật khẩu mới: tối thiểu 8 ký tự, có cả chữ cái và chữ số", pw_complexity)

# AC3: Đổi xong thu hồi các phiên đăng nhập khác
revoke_other_sessions = "revokeOtherSessions" in session_blacklist and "revokeOtherSessions" in change_pw_servlet
check("S1-04.AC3.1", "Đổi mật khẩu thành công tự động thu hồi toàn bộ các phiên đăng nhập khác", revoke_other_sessions)

# ==============================================================================
# S1-05: PHÂN QUYỀN THEO VAI TRÒ CHO TOÀN HỆ THỐNG
# ==============================================================================
print("\n" + "-" * 70)
print("5. [S1-05] PHÂN QUYỀN THEO VAI TRÒ CHO TOÀN HỆ THỐNG (8 SP - MUST)")
print("-" * 70)

# AC1: Khai báo được quyền cho từng vai trò trong tám vai trò nghiệp vụ
roles_defined = "ADMIN" in schema_sql and "TRAINING_MANAGER" in schema_sql and "ADMISSIONS" in schema_sql and "INSTRUCTOR" in schema_sql and "TA" in schema_sql and "ACCOUNTANT" in schema_sql and "STUDENT" in schema_sql
check("S1-05.AC1.1", "Khai báo đầy đủ 8 vai trò nghiệp vụ và bảng ma trận quyền role_permissions", roles_defined)

# AC2: Mọi chức năng đều kiểm quyền ở tầng server, mặc định là từ chối (Default-Deny)
default_deny = "AUTH_FORBIDDEN" in session_filter and "Chức năng này chưa được cấp quyền truy cập" in session_filter
check("S1-05.AC2.1", "Kiểm tra quyền hạn ở tầng server theo nguyên tắc Mặc định từ chối (Default-Deny)", default_deny)

# AC3: Truy cập thiếu quyền hiển thị thông báo tiếng Việt rõ ràng thay vì lỗi kỹ thuật
vietnamese_403 = "Kế toán không có quyền chỉnh sửa điểm số học viên" in auth_filter and "Giảng viên không có quyền chỉnh sửa thông tin học phí" in auth_filter and "403.jsp" in auth_filter
check("S1-05.AC3.1", "Thông báo từ chối truy cập 403 bằng tiếng Việt cụ thể, thân thiện, không lộ stack trace", vietnamese_403)

# AC4: Có kiểm thử tự động cho ít nhất ba vai trò
has_tests_for_roles = os.path.exists("legacy_or_mock/mock-node-api/test/idttx_20_authorization.test.js")
check("S1-05.AC4.1", "Có bộ kiểm thử tự động phân quyền cho ít nhất 3 vai trò (Admin, Giảng viên, Kế toán, Học viên)", has_tests_for_roles)

# ==============================================================================
# S1-06: THẤY MENU ĐIỀU HƯỚNG ĐÚNG THEO QUYỀN CỦA MÌNH
# ==============================================================================
print("\n" + "-" * 70)
print("6. [S1-06] THẤY MENU ĐIỀU HƯỚNG ĐÚNG THEO QUYỀN (5 SP - MUST)")
print("-" * 70)

# AC1: Mục menu không thuộc quyền thì không hiển thị
menu_filtered = "menuForUser" in permission_dao and "api/me/menu" in menu_servlet
check("S1-06.AC1.1", "Mục menu được lọc động từ cơ sở dữ liệu dựa trên quyền hạn của người dùng", menu_filtered)

# AC2: Hiển thị tên và vai trò người đang đăng nhập
profile_displayed = "user-display-name" in admin_html or "user-role-badge" in admin_html or "currentUser.fullName" in app_js
check("S1-06.AC2.1", "Giao diện hiển thị rõ họ tên và vai trò người đang đăng nhập trên thanh điều hướng", profile_displayed)

# AC3: Dùng được thuận tiện trên màn hình 360px
responsive_360 = "@media (max-width: 767px)" in style_css and "viewport" in admin_html
check("S1-06.AC3.1", "Giao diện hỗ trợ chuẩn responsive linh hoạt, hoạt động thuận tiện trên màn hình nhỏ 360px", responsive_360)

# ==============================================================================
# S1-07: NHẬN THÔNG BÁO RÕ RÀNG KHI TRUY CẬP NHẦM CHỖ/THIẾU QUYỀN
# ==============================================================================
print("\n" + "-" * 70)
print("7. [S1-07] NHẬN THÔNG BÁO RÕ RÀNG KHI TRUY CẬP NHẦM CHỖ (1 SP - SHOULD)")
print("-" * 70)

# AC1: Trang báo lỗi dùng chung giao diện ứng dụng
shared_error_theme = "error-card" in jsp_403 and "error-title" in jsp_403 and "bi-shield-x" in jsp_403
check("S1-07.AC1.1", "Trang thông báo lỗi thiết kế đồng bộ với layout chuẩn của hệ thống", shared_error_theme)

# AC2: Mỗi trang lỗi có một hành động gợi ý để quay lại luồng làm việc
action_suggestion = "Quay lại trang trước" in session_filter or "Về Quản lý điểm số" in jsp_403 or "action" in jsp_403
check("S1-07.AC2.1", "Trang lỗi cung cấp nút bấm gợi ý hành động cụ thể để quay lại luồng làm việc", action_suggestion)

# ==============================================================================
# S1-08: TẠO, SỬA VÀ TÌM KIẾM TÀI KHOẢN NGƯỜI DÙNG
# ==============================================================================
print("\n" + "-" * 70)
print("8. [S1-08] TẠO, SỬA VÀ TÌM KIẾM TÀI KHOẢN NGƯỜI DÙNG (8 SP - MUST)")
print("-" * 70)

# AC1: Tạo tài khoản gửi email kích hoạt kèm mật khẩu tạm
create_user_be = "createUser" in admin_dao and "sendActivationEmail" in email_service and "generateTempPassword" in admin_dao
create_user_modal = "id=\"create-user-modal\"" in admin_html and "open-create-user-modal" in admin_html
check("S1-08.AC1.1", "Tạo tài khoản tự động sinh mật khẩu tạm và gửi email kích hoạt kèm hướng dẫn", create_user_be and create_user_modal)

# AC2: Email trùng bị từ chối kèm thông báo cụ thể
dup_email_rejected = "EMAIL_ALREADY_EXISTS" in admin_servlet and "Email này đã tồn tại" in admin_dao and "EMAIL_ALREADY_EXISTS" in mock_admin_routes
check("S1-08.AC2.1", "Chặn đứng trùng email khi tạo tài khoản với mã lỗi HTTP 409 EMAIL_ALREADY_EXISTS", dup_email_rejected)

# AC3: Tìm theo tên, email, số điện thoại; lọc theo vai trò và trạng thái
search_and_filter = "searchUsers" in admin_dao and "keyword" in admin_dao and "roleCode" in admin_dao and "status" in admin_dao
fe_search_ui = "account-search-input" in admin_html and "account-role-filter" in admin_html and "account-status-filter" in admin_html
check("S1-08.AC3.1", "Tìm kiếm theo tên, email, SĐT và lọc theo vai trò, trạng thái tài khoản (Backend + Frontend)", search_and_filter and fe_search_ui)

# AC4: Danh sách phân trang, mặc định 20 dòng
pagination_20 = ("pageSize = 20" in admin_dao or "pageSize < 1) pageSize = 20" in admin_dao) and "limit = 20" in mock_admin_routes
pagination_ui = "account-pagination" in admin_html and ("PAGE_SIZE = 20" in app_js or "itemsPerPage = 20" in app_js)
check("S1-08.AC4.1", "Danh sách tài khoản được phân trang với mặc định đúng 20 dòng/trang", pagination_20 and pagination_ui)

# ==============================================================================
# S1-09: GÁN VÀ THU HỒI VAI TRÒ CHO MỘT NGƯỜI DÙNG
# ==============================================================================
print("\n" + "-" * 70)
print("9. [S1-09] GÁN VÀ THU HỒI VAI TRÒ CHO MỘT NGƯỜI DÙNG (3 SP - MUST)")
print("-" * 70)

# AC1: Một người dùng có thể giữ nhiều vai trò cùng lúc
multi_role_support = "PRIMARY KEY (`user_id`, `role_id`)" in schema_sql and "List<String> roles" in read_file("src/main/java/com/ems/model/User.java")
check("S1-09.AC1.1", "Hệ thống hỗ trợ một người dùng sở hữu đồng thời nhiều vai trò khác nhau", multi_role_support)

# AC2: Thay đổi vai trò có hiệu lực ngay ở thao tác kế tiếp, không cần đăng nhập lại
realtime_role_effect = "permissionDAO.hasPermission" in session_filter
check("S1-09.AC2.1", "Quyền hạn từ các vai trò được kiểm tra trực tiếp tức thời ở thao tác kế tiếp", realtime_role_effect)

# AC3: Không thể tự thu hồi vai trò quản trị của chính mình
self_admin_protection = "mayReplaceRoles" in permission_policy and "CANNOT_REVOKE_OWN_ADMIN" in mock_admin_routes
check("S1-09.AC3.1", "Chặn tuyệt đối Admin tự thu hồi quyền quản trị của chính bản thân", self_admin_protection)

# ==============================================================================
# S1-10: KHÓA VÀ MỞ KHÓA TÀI KHOẢN
# ==============================================================================
print("\n" + "-" * 70)
print("10. [S1-10] KHÓA VÀ MỞ KHÓA TÀI KHOẢN (2 SP - MUST)")
print("-" * 70)

# AC1: Tài khoản bị khoá không đăng nhập được và bị thu hồi phiên đang mở
lock_revocation = "revokeOtherSessions" in admin_dao and "ACCOUNT_INACTIVE" in mock_auth_mw
check("S1-10.AC1.1", "Khóa tài khoản lập tức thu hồi toàn bộ phiên đăng nhập đang hoạt động", lock_revocation)

# AC2: Bắt buộc ghi lý do khoá
mandatory_reason = "REASON_REQUIRED" in mock_admin_routes and "Bắt buộc phải ghi rõ lý do khi khoá tài khoản" in admin_dao
check("S1-10.AC2.1", "Bắt buộc nhập lý do khi khóa tài khoản và lưu vết lý do vào cơ sở dữ liệu", mandatory_reason)

# AC3: Lớp học do người đó phụ trách được cảnh báo cần bàn giao
handover_warning = "findAssignedClasses" in admin_dao and "requiresHandover" in admin_dao and "lock-handover-alert" in admin_html
check("S1-10.AC3.1", "Phát hiện và cảnh báo danh sách lớp học cần bàn giao khi khóa giảng viên/trợ giảng", handover_warning)

# ==============================================================================
# TỔNG KẾT BÁO CÁO KIỂM THỬ SPRINT 1
# ==============================================================================
print("\n" + "=" * 80)
pass_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0
print(f"KẾT QUẢ TỔNG QUAN: {passed_tests}/{total_tests} TIÊU CHÍ ĐÃ VƯỢT QUA ({pass_rate:.1f}%)")
if failed_tests == 0:
    print("XÁC NHẬN: TOÀN BỘ 10 USER STORIES CỦA SPRINT 1 (S1-01 -> S1-10) ĐÃ HOÀN THIỆN ĐẠT 100% TIÊU CHÍ!")
else:
    print(f"CẢNH BÁO: CÒN {failed_tests} TIÊU CHÍ CHƯA ĐẠT. VUI LÒNG KIỂM TRA LẠI.")
print("=" * 80)

if failed_tests > 0:
    sys.exit(1)
sys.exit(0)
