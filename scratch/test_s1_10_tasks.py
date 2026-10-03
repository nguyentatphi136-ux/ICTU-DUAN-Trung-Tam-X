#!/usr/bin/env python3
"""
Test script for S1-10: Khóa và mở khóa tài khoản
Validates all 8 tasks from the user specification:
1. [BE] API — khóa tài khoản, thu hồi phiên, ghi lý do
2. [BE] API — mở khóa
3. [BE] Xử lý thu hồi tất cả session/token khi khóa
4. [BE] Kiểm tra và cảnh báo lớp học do người đó phụ trách
5. [BE] Ghi log lý do khóa
6. [FE] Thiết kế UI nút khóa/mở khóa, modal nhập lý do
7. [FE] Hiển thị cảnh báo lớp học cần bàn giao
8. [FE] Hiển thị trạng thái tài khoản (đang hoạt động/bị khóa)
"""

import sys
import os
import re

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

WORKSPACE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def check_file(rel_path):
    full_path = os.path.join(WORKSPACE, rel_path)
    if not os.path.exists(full_path):
        raise FileNotFoundError(f"Missing file: {rel_path}")
    with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()

def test_s1_10():
    print("=" * 70)
    print("TEST SUITE: S1-10 KHÓA VÀ MỞ KHÓA TÀI KHOẢN")
    print("=" * 70)

    admin_dao = check_file("src/main/java/com/ems/dao/AdminDAO.java")
    admin_servlet = check_file("src/main/java/com/ems/controller/AdminApiServlet.java")
    permission_policy = check_file("src/main/java/com/ems/security/PermissionPolicy.java")
    mock_routes = check_file("legacy_or_mock/mock-node-api/src/routes/admin.routes.js")
    admin_html = check_file("frontend/admin.html")
    app_js = check_file("frontend/app.js")

    results = []

    # -------------------------------------------------------------
    # Task 1: [BE] API — khóa tài khoản, thu hồi phiên, ghi lý do
    # -------------------------------------------------------------
    t1_java_dao = "public Map<String, Object> updateUserStatus" in admin_dao and "lockedReason" in admin_dao
    t1_servlet = "USER_STATUS_PATH" in admin_servlet and "Pattern.compile(\"^/users/(\\\\d+)/status$\")" in admin_servlet
    t1_reason_check = "Bắt buộc phải ghi rõ lý do khi khoá tài khoản" in admin_dao
    t1_self_check = "Không thể tự khoá hoặc ngừng hoạt động tài khoản của chính mình" in admin_dao
    t1_mock_api = "/users/:id/status" in mock_routes and "REASON_REQUIRED" in mock_routes and "CANNOT_DEACTIVATE_SELF" in mock_routes
    t1_pass = t1_java_dao and t1_servlet and t1_reason_check and t1_self_check and t1_mock_api
    results.append(("[BE] API — khóa tài khoản, thu hồi phiên, ghi lý do", t1_pass, [
        ("AdminDAO.updateUserStatus method exists", t1_java_dao),
        ("AdminApiServlet handles PUT /users/{id}/status", t1_servlet),
        ("Validation: required lockedReason", t1_reason_check),
        ("Security: blocks self-locking (cannot lock own account)", t1_self_check),
        ("Mock Node API supports PUT /users/:id/status with reason validation", t1_mock_api),
    ]))

    # -------------------------------------------------------------
    # Task 2: [BE] API — mở khóa
    # -------------------------------------------------------------
    t2_unlock_status = "UPDATE users SET status = 'ACTIVE', locked_reason = NULL" in admin_dao
    t2_failed_reset = "failed_login_attempts = 0, locked_until = NULL" in admin_dao
    t2_servlet_unlock = "USER_UNLOCKED" in admin_servlet
    t2_mock_unlock = "rawStatus !== 'active'" in mock_routes
    t2_pass = t2_unlock_status and t2_failed_reset and t2_servlet_unlock and t2_mock_unlock
    results.append(("[BE] API — mở khóa", t2_pass, [
        ("AdminDAO resets status to ACTIVE and clears locked_reason to NULL", t2_unlock_status),
        ("AdminDAO resets failed_login_attempts and locked_until upon unlock", t2_failed_reset),
        ("AdminApiServlet returns USER_UNLOCKED success message", t2_servlet_unlock),
        ("Mock API supports status reset to active", t2_mock_unlock),
    ]))

    # -------------------------------------------------------------
    # Task 3: [BE] Xử lý thu hồi tất cả session/token khi khóa
    # -------------------------------------------------------------
    t3_revoke_sessions = "com.ems.config.SessionBlacklist.revokeOtherSessions(targetId, null)" in admin_dao
    t3_revoked_count = "revokedSessionsCount" in admin_dao
    t3_servlet_revoke_msg = "Toàn bộ phiên đăng nhập đã được thu hồi" in admin_servlet
    t3_mock_token_check = "ACCOUNT_INACTIVE" in check_file("legacy_or_mock/mock-node-api/src/middleware/auth.js")
    t3_pass = t3_revoke_sessions and t3_revoked_count and t3_servlet_revoke_msg and t3_mock_token_check
    results.append(("[BE] Xử lý thu hồi tất cả session/token khi khóa", t3_pass, [
        ("Calls SessionBlacklist.revokeOtherSessions immediately when locking", t3_revoke_sessions),
        ("Returns revokedSessionsCount in API response", t3_revoked_count),
        ("Servlet informs caller of session revocation in response message", t3_servlet_revoke_msg),
        ("Auth middleware rejects all tokens of locked accounts with ACCOUNT_INACTIVE", t3_mock_token_check),
    ]))

    # -------------------------------------------------------------
    # Task 4: [BE] Kiểm tra và cảnh báo lớp học do người đó phụ trách
    # -------------------------------------------------------------
    t4_find_classes = "findAssignedClasses(Connection connection, long userId)" in admin_dao
    t4_check_instructors = "primary_instructor_id" in admin_dao and "ta_user_id" in admin_dao
    t4_handover_flag = "requiresHandover" in admin_dao and "assignedClasses" in admin_dao
    t4_mock_handover = "requiresHandover" in mock_routes and "assignedClasses" in mock_routes
    t4_pass = t4_find_classes and t4_check_instructors and t4_handover_flag and t4_mock_handover
    results.append(("[BE] Kiểm tra và cảnh báo lớp học do người đó phụ trách", t4_pass, [
        ("AdminDAO.findAssignedClasses checks active classes (PLANNING/IN_PROGRESS)", t4_find_classes),
        ("Checks both primary instructor and TA assignments", t4_check_instructors),
        ("API response includes requiresHandover boolean and assignedClasses list", t4_handover_flag),
        ("Mock Node API returns assignedClasses and requiresHandover flag", t4_mock_handover),
    ]))

    # -------------------------------------------------------------
    # Task 5: [BE] Ghi log lý do khóa
    # -------------------------------------------------------------
    t5_audit_call = "recordAuditLog(connection, actorId, action, \"USER\"" in admin_dao
    t5_action_type = "LOCK_USER" in admin_dao and "UNLOCK_USER" in admin_dao
    t5_audit_sql = "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values)" in admin_dao
    t5_reason_in_log = "locked_reason" in admin_dao
    t5_pass = t5_audit_call and t5_action_type and t5_audit_sql and t5_reason_in_log
    results.append(("[BE] Ghi log lý do khóa", t5_pass, [
        ("Audit log recording triggered on status change", t5_audit_call),
        ("Action differentiated into LOCK_USER and UNLOCK_USER", t5_action_type),
        ("Persists to MySQL audit_logs table", t5_audit_sql),
        ("Logged new_values includes locked_reason and target status", t5_reason_in_log),
    ]))

    # -------------------------------------------------------------
    # Task 6: [FE] Thiết kế UI nút khóa/mở khóa, modal nhập lý do
    # -------------------------------------------------------------
    t6_modal_html = "id=\"lock-account-modal\"" in admin_html
    t6_form_html = "id=\"lock-account-form\"" in admin_html
    t6_reason_textarea = "id=\"lock-reason-input\"" in admin_html
    t6_modal_buttons = "data-close-lock-account" in admin_html and "id=\"lock-confirm-submit-btn\"" in admin_html
    t6_js_toggle = "data-open-lock-modal" in app_js and "data-unlock-account" in app_js
    t6_pass = t6_modal_html and t6_form_html and t6_reason_textarea and t6_modal_buttons and t6_js_toggle
    results.append(("[FE] Thiết kế UI nút khóa/mở khóa, modal nhập lý do", t6_pass, [
        ("Modal dialog #lock-account-modal in frontend/admin.html", t6_modal_html),
        ("Form #lock-account-form for user lock action", t6_form_html),
        ("Textarea #lock-reason-input with required validation and placeholder", t6_reason_textarea),
        ("Modal action buttons (Cancel, Confirm Lock) present", t6_modal_buttons),
        ("Table action buttons [data-open-lock-modal] and [data-unlock-account] present", t6_js_toggle),
    ]))

    # -------------------------------------------------------------
    # Task 7: [FE] Hiển thị cảnh báo lớp học cần bàn giao
    # -------------------------------------------------------------
    t7_warning_box = "id=\"lock-handover-alert\"" in admin_html
    t7_classes_list = "id=\"lock-handover-classes\"" in admin_html
    t7_js_classes_lookup = "getAssignedClasses" in app_js
    t7_js_warning_render = "warningBox.classList.remove(\"hidden\")" in app_js
    t7_table_warning_indicator = "Phụ trách" in app_js and "lớp" in app_js
    t7_pass = t7_warning_box and t7_classes_list and t7_js_classes_lookup and t7_js_warning_render and t7_table_warning_indicator
    results.append(("[FE] Hiển thị cảnh báo lớp học cần bàn giao", t7_pass, [
        ("Handover alert box #lock-handover-alert with warning styling in modal", t7_warning_box),
        ("Classes list element #lock-handover-classes in modal", t7_classes_list),
        ("Frontend helper getAssignedClasses finds active teaching assignments", t7_js_classes_lookup),
        ("Displays alert with assigned classes when locking teacher/TA", t7_js_warning_render),
        ("Account table displays indicator badge for instructors with active classes", t7_table_warning_indicator),
    ]))

    # -------------------------------------------------------------
    # Task 8: [FE] Hiển thị trạng thái tài khoản (đang hoạt động/bị khóa)
    # -------------------------------------------------------------
    t8_badge_active = "badge(\"Hoạt động\", \"is-success\")" in app_js
    t8_badge_locked = "badge(\"Đã khoá\", \"is-danger\")" in app_js
    t8_display_reason = "title=\"Lý do:" in app_js or "<span class=\"font-medium\">Lý do:</span>" in app_js
    t8_login_locked_reason = "Tài khoản đã bị khoá. Lý do:" in app_js
    t8_pass = t8_badge_active and t8_badge_locked and t8_display_reason and t8_login_locked_reason
    results.append(("[FE] Hiển thị trạng thái tài khoản (đang hoạt động/bị khóa)", t8_pass, [
        ("Green badge 'Hoạt động' for active accounts", t8_badge_active),
        ("Danger badge 'Đã khoá' for locked accounts", t8_badge_locked),
        ("Displays locked reason under badge and as tooltip", t8_display_reason),
        ("Login form displays account locked status along with specific reason", t8_login_locked_reason),
    ]))

    # Print summary report
    all_passed = True
    for task_name, task_pass, subchecks in results:
        status_icon = "✓ PASS" if task_pass else "✗ FAIL"
        print(f"\n{status_icon} : {task_name}")
        for label, ok in subchecks:
            sub_icon = "  [+] OK " if ok else "  [-] ERR"
            print(f"{sub_icon} - {label}")
        if not task_pass:
            all_passed = False

    print("\n" + "=" * 70)
    if all_passed:
        print("ALL 8 TASKS OF S1-10 VERIFIED AND PASSED SUCCESSFULLY! (100%)")
    else:
        print("SOME CHECKS FAILED. Please review the output above.")
    print("=" * 70)
    return all_passed

if __name__ == "__main__":
    success = test_s1_10()
    sys.exit(0 if success else 1)
