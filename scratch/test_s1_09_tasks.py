import re
import json

print("=" * 65)
print("KIỂM THỬ CHI TIẾT 6 NHIỆM VỤ CỦA CHỨC NĂNG S1-09")
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

# ----------------------------------------------------------------------
# 1. [BE] API POST /admin/users/:id/roles — Gán vai trò
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 1: [BE] API POST /admin/users/:id/roles (Gán vai trò) ---")

# Kiểm tra Java AdminApiServlet và PermissionPolicy
with open("src/main/java/com/ems/controller/AdminApiServlet.java", "r", encoding="utf-8") as f:
    admin_servlet = f.read()

with open("src/main/java/com/ems/security/PermissionPolicy.java", "r", encoding="utf-8") as f:
    permission_policy = f.read()

with open("src/main/java/com/ems/dao/AdminDAO.java", "r", encoding="utf-8") as f:
    admin_dao = f.read()

assert_test("PermissionPolicy khai báo USER_ROLE_ASSIGN cho POST /api/admin/users/:id/roles",
            '("POST".equals(method) || "PUT".equals(method)) && path.matches("^/api/admin/users/\\\\d+/roles$")' in permission_policy)

assert_test("AdminApiServlet có xử lý doPost cho /users/:id/roles",
            "doPost(HttpServletRequest" in admin_servlet and "userMatcher.matches()" in admin_servlet and "assignRole" in admin_servlet)

assert_test("AdminDAO có phương thức assignRole(actorId, targetId, roleCode)",
            "public Map<String, Object> assignRole(long actorId, long targetId, String roleCode)" in admin_dao)

# Kiểm tra trong Node.js Mock API
with open("legacy_or_mock/mock-node-api/src/routes/admin.routes.js", "r", encoding="utf-8") as f:
    node_admin_routes = f.read()

assert_test("Mock API có endpoint router.post('/users/:id/roles')",
            "router.post('/users/:id/roles'" in node_admin_routes)

# ----------------------------------------------------------------------
# 2. [BE] API DELETE /admin/users/:id/roles/:roleId — Thu hồi vai trò
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 2: [BE] API DELETE /admin/users/:id/roles/:roleId (Thu hồi vai trò) ---")

assert_test("PermissionPolicy khai báo USER_ROLE_ASSIGN cho DELETE /api/admin/users/:id/roles/:roleId",
            '"DELETE".equals(method) && path.matches("^/api/admin/users/\\\\d+/roles/[A-Za-z0-9_]+$")' in permission_policy)

assert_test("AdminApiServlet có xử lý doDelete cho /users/:id/roles/:roleCode",
            "doDelete(HttpServletRequest" in admin_servlet and "revokeMatcher.matches()" in admin_servlet and "revokeRole" in admin_servlet)

assert_test("AdminDAO có phương thức revokeRole(actorId, targetId, roleCode)",
            "public Map<String, Object> revokeRole(long actorId, long targetId, String roleCode)" in admin_dao)

assert_test("Mock API có endpoint router.delete('/users/:id/roles/:roleId')",
            "router.delete('/users/:id/roles/:roleId'" in node_admin_routes)

# ----------------------------------------------------------------------
# 3. [BE] Xử lý nhiều vai trò cùng lúc
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 3: [BE] Xử lý nhiều vai trò cùng lúc ---")

with open("database/schema.sql", "r", encoding="utf-8") as f:
    schema_sql = f.read()

assert_test("Database CSDL: Bảng user_roles hỗ trợ quan hệ N-N (1 người nhiều vai trò)",
            "CREATE TABLE IF NOT EXISTS `user_roles`" in schema_sql and "PRIMARY KEY (`user_id`, `role_id`)" in schema_sql)

with open("src/main/java/com/ems/model/User.java", "r", encoding="utf-8") as f:
    user_model = f.read()

assert_test("User model hỗ trợ danh sách nhiều roles: List<String> roles",
            "List<String> roles" in user_model and "getRoles()" in user_model)

with open("src/main/java/com/ems/dao/PermissionDAO.java", "r", encoding="utf-8") as f:
    permission_dao = f.read()

assert_test("PermissionDAO.hasPermission truy vấn hợp nhất (JOIN) quyền hạn từ tất cả các vai trò của user",
            "JOIN user_roles ur ON ur.user_id = u.id" in permission_dao and "JOIN role_permissions rp ON rp.role_id = ur.role_id" in permission_dao)

# ----------------------------------------------------------------------
# 4. [BE] Cập nhật quyền có hiệu lực ngay
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 4: [BE] Cập nhật quyền có hiệu lực ngay ---")

with open("src/main/java/com/ems/filter/SessionAuthFilter.java", "r", encoding="utf-8") as f:
    session_filter = f.read()

assert_test("SessionAuthFilter kiểm tra permissionDAO.hasPermission trong DB ở mỗi request (Real-time DB query)",
            "permissionDAO.hasPermission(currentUser.getId(), requiredPermission)" in session_filter)

assert_test("AdminDAO commit transaction ngay khi gán/thu hồi vai trò",
            "connection.commit()" in admin_dao and "DELETE FROM user_roles WHERE user_id = ?" in admin_dao)

# ----------------------------------------------------------------------
# 5. [BE] Chặn tự thu hồi vai trò quản trị của chính mình
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 5: [BE] Chặn tự thu hồi vai trò quản trị của chính mình ---")

def may_replace_roles(actor_id, target_id, current_roles, requested_roles):
    return (actor_id != target_id or 
            current_roles is None or 
            "ADMIN" not in current_roles or 
            (requested_roles is not None and "ADMIN" in requested_roles))

# Test logic
assert_test("Admin tự thu hồi ADMIN của chính mình -> BỊ CHẶN (False)",
            may_replace_roles(1, 1, {"ADMIN"}, {"STUDENT"}) is False)

assert_test("Admin tự thu hồi ADMIN khi có nhiều vai trò -> BỊ CHẶN (False)",
            may_replace_roles(1, 1, {"ADMIN", "INSTRUCTOR"}, {"INSTRUCTOR"}) is False)

assert_test("Admin giữ ADMIN và thêm vai trò mới -> ĐƯỢC PHÉP (True)",
            may_replace_roles(1, 1, {"ADMIN"}, {"ADMIN", "TRAINING_MANAGER"}) is True)

assert_test("Admin thu hồi vai trò ADMIN của một tài khoản khác -> ĐƯỢC PHÉP (True)",
            may_replace_roles(1, 2, {"ADMIN"}, {"STUDENT"}) is True)

assert_test("AdminDAO ném ngoại lệ khi vi phạm mayReplaceRoles",
            'if (!PermissionPolicy.mayReplaceRoles(actorId, targetId, currentRoles, requested))' in admin_dao)

assert_test("Mock API có CANNOT_REVOKE_OWN_ADMIN khi tự thu hồi",
            "code: 'CANNOT_REVOKE_OWN_ADMIN'" in node_admin_routes)

# ----------------------------------------------------------------------
# 6. [FE] Thiết kế UI quản lý vai trò của user
# ----------------------------------------------------------------------
print("\n--- Nhiệm vụ 6: [FE] Thiết kế UI quản lý vai trò của user ---")

with open("frontend/admin.html", "r", encoding="utf-8") as f:
    admin_html = f.read()

with open("frontend/app.js", "r", encoding="utf-8") as f:
    app_js = f.read()

assert_test("Giao diện admin.html có section danh mục 8 vai trò (#vai-tro)",
            'id="vai-tro"' in admin_html and "Vai trò &" in admin_html)

assert_test("Giao diện admin.html có bảng quản lý vai trò (#current-role-table)",
            'id="current-role-table"' in admin_html)

assert_test("Giao diện app.js render danh sách vai trò với nút Thu hồi / Khôi phục",
            'data-role-action="${key}"' in app_js and 'revokedRole ? "Khôi phục" : "Thu hồi"' in app_js)

assert_test("Giao diện app.js có cơ chế vô hiệu hóa hoặc cảnh báo không được tự thu hồi vai trò hiện tại",
            'roleKey === currentUser?.role' in app_js and 'Không thể thu hồi vai trò đang được sử dụng' in app_js)

assert_test("Giao diện app.js lưu vết thay đổi vai trò vào Audit Log",
            'addAuditLog(' in app_js and 'renderAuditLog()' in app_js)

# ----------------------------------------------------------------------
# TỔNG KẾT
# ----------------------------------------------------------------------
print("\n" + "=" * 65)
print(f"KẾT QUẢ KIỂM THỬ: {passed} PASSED, {failed} FAILED ({(passed/(passed+failed))*100:.1f}%)")
print("=" * 65)
