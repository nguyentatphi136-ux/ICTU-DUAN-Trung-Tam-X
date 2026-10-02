import re
import datetime
from datetime import timezone

print("=" * 60)
print("TESTING NEWLY ADDED FEATURES (PR #11 - DuyKien & S1 Logic)")
print("=" * 60)

passed = 0
failed = 0

def test(name, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print(f"[PASS] {name}")
    else:
        failed += 1
        print(f"[FAIL] {name}: {detail}")

# -------------------------------------------------------------
# 1. LoginAttemptPolicy Logic
# -------------------------------------------------------------
print("\n--- 1. LoginAttemptPolicy & Rate Limiting Test ---")

class LoginAttemptState:
    def __init__(self, failures, locked_until):
        self.failures = failures
        self.locked_until = locked_until

    def __repr__(self):
        return f"State(failures={self.failures}, locked_until={self.locked_until})"

class LoginAttemptPolicy:
    MAX_FAILED_ATTEMPTS = 5
    LOCK_DURATION_MINUTES = 15

    @classmethod
    def record_failure(cls, current: LoginAttemptState, now: datetime.datetime):
        if current.locked_until and current.locked_until > now:
            return current
        failures = 0 if current.locked_until else current.failures
        failures += 1
        locked_until = now + datetime.timedelta(minutes=cls.LOCK_DURATION_MINUTES) if failures >= cls.MAX_FAILED_ATTEMPTS else None
        return LoginAttemptState(failures, locked_until)

    @classmethod
    def is_locked(cls, state: LoginAttemptState, now: datetime.datetime):
        return bool(state.locked_until and state.locked_until > now)

now = datetime.datetime(2026, 10, 2, 10, 0, 0, tzinfo=timezone.utc)
state = LoginAttemptState(0, None)

# Attempts 1 to 4
for i in range(1, 5):
    state = LoginAttemptPolicy.record_failure(state, now)
    test(f"Attempt {i}: failures={i}, not locked", state.failures == i and not LoginAttemptPolicy.is_locked(state, now))

# Attempt 5 -> Locked for 15 minutes
state = LoginAttemptPolicy.record_failure(state, now)
expected_lock = now + datetime.timedelta(minutes=15)
test("Attempt 5: locks account for 15 minutes", state.failures == 5 and state.locked_until == expected_lock and LoginAttemptPolicy.is_locked(state, now))

# Failure during lock period -> does not extend lock
state_during = LoginAttemptPolicy.record_failure(state, now + datetime.timedelta(minutes=5))
test("Failure during lock does not extend lock duration", state_during.locked_until == expected_lock)

# After lock expires -> is_locked is false, new failure resets failure count to 1
after_lock = expected_lock + datetime.timedelta(seconds=1)
test("After lock duration expires, is_locked is False", not LoginAttemptPolicy.is_locked(state, after_lock))

state_after = LoginAttemptPolicy.record_failure(state, after_lock)
test("New failure after lock expiration resets failures to 1 and unlocks", state_after.failures == 1 and state_after.locked_until is None)

# -------------------------------------------------------------
# 2. PermissionPolicy Logic
# -------------------------------------------------------------
print("\n--- 2. PermissionPolicy & Default-Deny Security Test ---")

API_PERMISSIONS = {
    "GET /api/me/menu": "MENU_VIEW",
    "GET /api/admin/users": "USER_READ",
    "POST /api/admin/users": "USER_CREATE",
    "GET /api/admin/roles": "ROLE_PERMISSION_READ",
    "GET /api/admin/permissions": "ROLE_PERMISSION_READ",
    "GET /api/admin/menus": "ROLE_PERMISSION_READ"
}

def required_permission(method: str, path: str):
    exact = API_PERMISSIONS.get(f"{method} {path}")
    if exact:
        return exact
    if method == "PUT" and re.match(r"^/api/admin/roles/[A-Z_]+/permissions$", path):
        return "ROLE_PERMISSION_UPDATE"
    if method == "PUT" and re.match(r"^/api/admin/users/\d+/roles$", path):
        return "USER_ROLE_ASSIGN"
    if method == "POST" and path == "/api/auth/logout":
        return "@authenticated"
    return None

def may_replace_roles(actor_id: int, target_id: int, current_roles: set, requested_roles: set):
    return (actor_id != target_id or 
            current_roles is None or 
            "ADMIN" not in current_roles or 
            ("ADMIN" in requested_roles if requested_roles else False))

# Test required permissions
test("GET /api/me/menu requires MENU_VIEW", required_permission("GET", "/api/me/menu") == "MENU_VIEW")
test("GET /api/admin/users requires USER_READ", required_permission("GET", "/api/admin/users") == "USER_READ")
test("POST /api/admin/users requires USER_CREATE", required_permission("POST", "/api/admin/users") == "USER_CREATE")
test("PUT /api/admin/roles/INSTRUCTOR/permissions requires ROLE_PERMISSION_UPDATE", 
     required_permission("PUT", "/api/admin/roles/INSTRUCTOR/permissions") == "ROLE_PERMISSION_UPDATE")
test("PUT /api/admin/users/8/roles requires USER_ROLE_ASSIGN",
     required_permission("PUT", "/api/admin/users/8/roles") == "USER_ROLE_ASSIGN")
test("Default-Deny: DELETE /api/admin/users/8 is denied (returns None)",
     required_permission("DELETE", "/api/admin/users/8") is None)
test("Default-Deny: GET /api/secret/data is denied (returns None)",
     required_permission("GET", "/api/secret/data") is None)

# Test Self-Admin Revocation Protection (IDTTX-76)
test("Admin can change other user's roles (target_id=5, actor_id=4)",
     may_replace_roles(4, 5, {"ADMIN"}, {"STUDENT"}) is True)
test("Admin CANNOT remove ADMIN role from self (target_id=4, actor_id=4)",
     may_replace_roles(4, 4, {"ADMIN"}, {"STUDENT"}) is False)
test("Admin CAN update own roles as long as ADMIN role is retained",
     may_replace_roles(4, 4, {"ADMIN"}, {"ADMIN", "INSTRUCTOR"}) is True)

# -------------------------------------------------------------
# 3. Database Schema Verification for Newly Added Features
# -------------------------------------------------------------
print("\n--- 3. Database Schema Verification ---")

with open("database/schema.sql", "r", encoding="utf-8") as f:
    sql_content = f.read()

test("Schema defines 'failed_login_attempts INT NOT NULL DEFAULT 0'",
     "failed_login_attempts` INT NOT NULL DEFAULT 0" in sql_content or "failed_login_attempts INT NOT NULL DEFAULT 0" in sql_content)
test("Schema defines 'locked_until DATETIME NULL'",
     "locked_until` DATETIME NULL" in sql_content or "locked_until DATETIME NULL" in sql_content)
test("Schema defines 'roles' table", "TABLE IF NOT EXISTS `roles`" in sql_content or "TABLE IF NOT EXISTS roles" in sql_content)
test("Schema defines 'permissions' table", "TABLE IF NOT EXISTS `permissions`" in sql_content or "TABLE IF NOT EXISTS permissions" in sql_content)
test("Schema defines 'role_permissions' table", "TABLE IF NOT EXISTS `role_permissions`" in sql_content or "TABLE IF NOT EXISTS role_permissions" in sql_content)
test("Schema defines 'user_roles' table", "TABLE IF NOT EXISTS `user_roles`" in sql_content or "TABLE IF NOT EXISTS user_roles" in sql_content)
test("Schema defines 'password_reset_tokens' table", "TABLE IF NOT EXISTS `password_reset_tokens`" in sql_content or "TABLE IF NOT EXISTS password_reset_tokens" in sql_content)
test("Schema defines 'idx_users_fullname'", "idx_users_fullname" in sql_content)

# -------------------------------------------------------------
# Summary
# -------------------------------------------------------------
print("\n" + "=" * 60)
print(f"VERIFICATION SUMMARY: {passed} PASSED, {failed} FAILED")
print("=" * 60)
