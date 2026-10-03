import re

print("=" * 65)
print("KIỂM THỬ: XỬ LÝ TRẢ VỀ THÔNG BÁO TIẾNG VIỆT KHI THIẾU QUYỀN (403)")
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

with open("src/main/java/com/ems/filter/AuthorizationFilter.java", "r", encoding="utf-8") as f:
    auth_filter = f.read()

with open("src/main/java/com/ems/filter/SessionAuthFilter.java", "r", encoding="utf-8") as f:
    session_filter = f.read()

with open("src/main/webapp/WEB-INF/views/common/403.jsp", "r", encoding="utf-8") as f:
    jsp_403 = f.read()

with open("src/main/java/com/ems/security/ApiResponse.java", "r", encoding="utf-8") as f:
    api_resp = f.read()

with open("legacy_or_mock/mock-node-api/src/middleware/auth.js", "r", encoding="utf-8") as f:
    mock_auth = f.read()

# ----------------------------------------------------------------------
# 1. Java Web Filter: AuthorizationFilter.java (JSP / Web Views)
# ----------------------------------------------------------------------
print("\n--- 1. Kiểm tra AuthorizationFilter.java (Java Web Views) ---")

assert_test("Thiết lập mã trạng thái HTTP 403 (HttpServletResponse.SC_FORBIDDEN)",
            "res.setStatus(HttpServletResponse.SC_FORBIDDEN)" in auth_filter)

assert_test("Kế toán thử sửa điểm -> Thông báo tiếng Việt: 'Kế toán không có quyền chỉnh sửa điểm số học viên.'",
            "Kế toán không có quyền chỉnh sửa điểm số học viên." in auth_filter)

assert_test("Giảng viên thử sửa học phí -> Thông báo tiếng Việt: 'Giảng viên không có quyền chỉnh sửa thông tin học phí.'",
            "Giảng viên không có quyền chỉnh sửa thông tin học phí." in auth_filter)

assert_test("Học viên hoặc người thiếu quyền xem học phí -> Thông báo: 'Bạn không có quyền xem thông tin học phí.'",
            "Bạn không có quyền xem thông tin học phí." in auth_filter)

assert_test("Nguyên tắc Default-Deny -> Thông báo: 'Bạn không có quyền truy cập chức năng này.'",
            "Bạn không có quyền truy cập chức năng này." in auth_filter)

assert_test("Chuyển tiếp (forward) tới trang giao diện lỗi tiếng Việt chuyên dụng: /WEB-INF/views/common/403.jsp",
            'req.getRequestDispatcher("/WEB-INF/views/common/403.jsp").forward(req, res)' in auth_filter)

# ----------------------------------------------------------------------
# 2. Java API Filter: SessionAuthFilter.java (REST API Endpoints)
# ----------------------------------------------------------------------
print("\n--- 2. Kiểm tra SessionAuthFilter.java (REST API Endpoints) ---")

assert_test("API chưa cấp quyền (Default-Deny) trả về HTTP 403 với thông báo tiếng Việt",
            'ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN"' in session_filter and
            '"Chức năng này chưa được cấp quyền truy cập."' in session_filter)

assert_test("API người dùng không đủ quyền trả về HTTP 403 với thông báo tiếng Việt",
            'ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "AUTH_FORBIDDEN"' in session_filter and
            '"Bạn không có quyền thực hiện chức năng này."' in session_filter)

assert_test("ApiResponse chuẩn hóa cấu trúc lỗi gồm code, message và gợi ý hành động tiếng Việt",
            '"action"' in api_resp and '"Quay lại trang trước"' in session_filter and '"Về trang làm việc"' in session_filter)

# ----------------------------------------------------------------------
# 3. Giao diện trang lỗi: 403.jsp
# ----------------------------------------------------------------------
print("\n--- 3. Kiểm tra trang giao diện 403.jsp ---")

assert_test("Tiêu đề trang tiếng Việt rõ ràng: 'Truy cập bị từ chối (403 Forbidden)'",
            "Truy cập bị từ chối (403 Forbidden)" in jsp_403)

assert_test("Hiển thị thông báo lỗi chi tiết được truyền từ Filter (forbiddenMessage/errorMessage)",
            "${forbiddenMessage}" in jsp_403 and "${errorMessage}" in jsp_403)

assert_test("Hiển thị thông tin tài khoản và danh sách vai trò hiện tại của người dùng",
            "Tài khoản đang đăng nhập:" in jsp_403 and "Vai trò của bạn:" in jsp_403)

assert_test("Có nút điều hướng gợi ý hành động: 'Quay lại', 'Về Quản lý điểm số', 'Về Quản lý học phí', 'Trang chủ'",
            "Quay lại" in jsp_403 and "Về Quản lý điểm số" in jsp_403 and "Về Quản lý học phí" in jsp_403 and "Trang chủ" in jsp_403)

assert_test("Giao diện thẻ lỗi chuyên nghiệp (icon bi-shield-x, màu cảnh báo, không xuất hiện stack trace)",
            "bi-shield-x" in jsp_403 and "error-card" in jsp_403 and "printStackTrace" not in jsp_403)

# ----------------------------------------------------------------------
# 4. Mock API Node.js: auth.js
# ----------------------------------------------------------------------
print("\n--- 4. Kiểm tra Middleware Mock API (Node.js) ---")

assert_test("Mock API trả HTTP 403 FORBIDDEN với thông báo tiếng Việt cho Kế toán sửa điểm",
            "message: 'Kế toán không có quyền chỉnh sửa điểm số học viên'" in mock_auth)

assert_test("Mock API trả HTTP 403 FORBIDDEN với thông báo tiếng Việt cho Giảng viên sửa học phí",
            "message: 'Giảng viên không có quyền chỉnh sửa thông tin học phí'" in mock_auth)

assert_test("Mock API defaultDeny trả về thông báo tiếng Việt rõ ràng",
            "Truy cập bị từ chối: Chức năng chưa được cấp quyền ở tầng máy chủ" in mock_auth)

# ----------------------------------------------------------------------
# TỔNG KẾT
# ----------------------------------------------------------------------
print("\n" + "=" * 65)
print(f"KẾT QUẢ KIỂM THỬ: {passed} PASSED, {failed} FAILED ({(passed/(passed+failed))*100:.1f}%)")
print("=" * 65)
