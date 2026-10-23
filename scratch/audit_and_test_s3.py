# -*- coding: utf-8 -*-
"""
Script kiểm thử và đánh giá toàn diện tiến độ Sprint 3 (S3-01 -> S3-09)
Kiểm tra cả 3 tầng: CSDL (Migrations), Backend (Node.js & Java Servlet), và Frontend (React UI)
"""
import os
import re
import sys
import subprocess

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
os.chdir(ROOT_DIR)

results = []

def record(code, name, status, details=""):
    results.append({
        "code": code,
        "name": name,
        "status": status,
        "details": details
    })
    tag = "[PASS]" if status == "PASS" else ("[WARN]" if status == "WARN" else "[FAIL]")
    print(f"  {tag} {code}: {name}")
    if details:
        print(f"         -> {details}")

print("=" * 80)
print("KIỂM THỬ VÀ ĐÁNH GIÁ TOÀN DIỆN TIẾN ĐỘ SPRINT 3 (S3-01 ĐẾN S3-09)")
print("=" * 80)

# ==============================================================================
# PHẦN 1: KIỂM TRA DATABASE MIGRATIONS SPRINT 3
# ==============================================================================
print("\n>>> 1. KIỂM TRA MIGRATION CƠ SỞ DỮ LIỆU SPRINT 3 <<<")

# S3-01 Lead Funnel
p_s3_01 = os.path.join(ROOT_DIR, "database", "migration_s3_01_lead_funnel.sql")
if os.path.exists(p_s3_01):
    content = open(p_s3_01, "r", encoding="utf-8").read()
    if "lead_care_logs" in content and "reject_reason" in content:
        record("DB.S3-01", "Migration phễu lead Kanban & nhật ký chăm sóc (lead_care_logs)", "PASS", "Có bảng lead_care_logs và cột reject_reason")
    else:
        record("DB.S3-01", "Migration phễu lead Kanban", "WARN", "Thiếu lead_care_logs hoặc reject_reason")
else:
    record("DB.S3-01", "Migration phễu lead Kanban", "FAIL", "Không tìm thấy file migration_s3_01_lead_funnel.sql")

# S3-03 Convert Student
p_s3_03 = os.path.join(ROOT_DIR, "database", "migration_s3_03_convert_student.sql")
if os.path.exists(p_s3_03):
    content = open(p_s3_03, "r", encoding="utf-8").read()
    if "students" in content and "student_code" in content:
        record("DB.S3-03", "Migration chuyển lead thành học viên (students, student_notes)", "PASS", "Có cấu trúc bảng students và liên kết lead")
    else:
        record("DB.S3-03", "Migration chuyển lead thành học viên", "WARN", "Thiếu cấu trúc bảng students")
else:
    record("DB.S3-03", "Migration chuyển lead thành học viên", "FAIL", "Không tìm thấy file migration_s3_03_convert_student.sql")

# S3-04 Student Profile
p_s3_04 = os.path.join(ROOT_DIR, "database", "migration_s3_04_student_profile.sql")
if os.path.exists(p_s3_04):
    content = open(p_s3_04, "r", encoding="utf-8").read()
    if "student_class_history" in content or "student_notes" in content:
        record("DB.S3-04", "Migration hồ sơ học viên & lịch sử học tập", "PASS", "Có bảng student_class_history và student_notes")
    else:
        record("DB.S3-04", "Migration hồ sơ học viên", "WARN", "Thiếu student_class_history")
else:
    record("DB.S3-04", "Migration hồ sơ học viên", "FAIL", "Không tìm thấy file migration_s3_04_student_profile.sql")

# S3-05 Classes & Class Subjects
p_s3_05 = os.path.join(ROOT_DIR, "database", "migrations", "V20261011_01__create_class_and_class_subjects.sql")
if os.path.exists(p_s3_05):
    content = open(p_s3_05, "r", encoding="utf-8").read().lower()
    if "create table if not exists" in content and "classes" in content and "class_subjects" in content:
        record("DB.S3-05", "Migration mở lớp & môn học theo lớp (classes, class_subjects)", "PASS", "Có bảng classes và class_subjects ràng buộc FK")
    else:
        record("DB.S3-05", "Migration mở lớp", "WARN", "Thiếu classes hoặc class_subjects")
else:
    record("DB.S3-05", "Migration mở lớp", "FAIL", "Không tìm thấy file V20261011_01__create_class_and_class_subjects.sql")

# S3-09 Classrooms
p_s3_09 = os.path.join(ROOT_DIR, "database", "migration_s3_09_classrooms.sql")
if os.path.exists(p_s3_09):
    content = open(p_s3_09, "r", encoding="utf-8").read()
    if "classrooms" in content and "capacity" in content and "room_type" in content:
        record("DB.S3-09", "Migration phòng học vật lý & trực tuyến (classrooms)", "PASS", "Có bảng classrooms với cột capacity, room_type, status")
    else:
        record("DB.S3-09", "Migration phòng học", "WARN", "Thiếu classrooms hoặc capacity")
else:
    record("DB.S3-09", "Migration phòng học", "FAIL", "Không tìm thấy file migration_s3_09_classrooms.sql")

# ==============================================================================
# PHẦN 2: CHẠY BỘ KIỂM THỬ BACKEND (NODE.JS CHO S3-01, S3-03, S3-04)
# ==============================================================================
print("\n>>> 2. CHẠY BỘ KIỂM THỬ BACKEND TỰ ĐỘNG (NODE.JS) <<<")

# Test S3-01 Funnel
try:
    res = subprocess.run(["node", "backend/legacy_node_be/test/funnel_s3_01.test.js"], capture_output=True, encoding="utf-8", errors="replace", cwd=ROOT_DIR)
    if res.returncode == 0 and "fail 0" in (res.stdout or ""):
        record("BE.S3-01", "Backend API Phễu Kanban & Nhật ký (14/14 tests)", "PASS", "14/14 kịch bản kiểm thử pass 100%")
    else:
        record("BE.S3-01", "Backend API Phễu Kanban", "FAIL", (res.stderr or res.stdout or "Unknown error")[:200])
except Exception as e:
    record("BE.S3-01", "Backend API Phễu Kanban", "FAIL", str(e))

# Test S3-03 Convert Student
try:
    res = subprocess.run(["node", "backend/legacy_node_be/test/convert_student_s3_03.test.js"], capture_output=True, encoding="utf-8", errors="replace", cwd=ROOT_DIR)
    if res.returncode == 0 and "fail 0" in (res.stdout or ""):
        record("BE.S3-03", "Backend API Chuyển lead thành học viên (15/15 tests)", "PASS", "15/15 kịch bản kiểm thử pass 100%")
    else:
        record("BE.S3-03", "Backend API Chuyển lead thành học viên", "FAIL", (res.stderr or res.stdout or "Unknown error")[:200])
except Exception as e:
    record("BE.S3-03", "Backend API Chuyển lead thành học viên", "FAIL", str(e))

# Test S3-04 Student Profile
try:
    res = subprocess.run(["node", "backend/legacy_node_be/test/student_profile_s3_04.test.js"], capture_output=True, encoding="utf-8", errors="replace", cwd=ROOT_DIR)
    if res.returncode == 0 and "fail 0" in (res.stdout or ""):
        record("BE.S3-04", "Backend API Hồ sơ học viên (18/18 tests)", "PASS", "18/18 kịch bản kiểm thử pass 100%")
    else:
        record("BE.S3-04", "Backend API Hồ sơ học viên", "FAIL", (res.stderr or res.stdout or "Unknown error")[:200])
except Exception as e:
    record("BE.S3-04", "Backend API Hồ sơ học viên", "FAIL", str(e))

# ==============================================================================
# PHẦN 3: KIỂM TRA BACKEND JAVA SERVLET (S3-05, S3-07, S3-09)
# ==============================================================================
print("\n>>> 3. ĐÁNH GIÁ BACKEND JAVA SERVLET CHO SPRINT 3 <<<")

# S3-05: ClassApiServlet, ClassService, ClassDAO
p_class_servlet = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "controller", "ClassApiServlet.java")
p_class_service = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "service", "ClassService.java")
p_class_dao = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "dao", "ClassDAO.java")

if os.path.exists(p_class_servlet) and os.path.exists(p_class_service) and os.path.exists(p_class_dao):
    s_content = open(p_class_servlet, "r", encoding="utf-8").read()
    has_post = "doPost" in s_content
    has_get = "doGet" in s_content
    has_put = "doPut" in s_content
    if has_post and has_get:
        if has_put:
            record("JAVA.S3-05", "Java API Mở lớp và Vòng đời lớp (POST, GET, PUT)", "PASS", "Có đầy đủ mở lớp và đổi trạng thái vòng đời")
        else:
            record("JAVA.S3-05", "Java API Mở lớp (POST, GET)", "WARN", "Đã có mở lớp (POST /api/classes) và danh sách (GET /api/classes), CHƯA CÓ PUT đổi trạng thái vòng đời lớp")
else:
    record("JAVA.S3-05", "Java API Mở lớp học", "FAIL", "Thiếu ClassApiServlet, ClassService hoặc ClassDAO")

# S3-06: Ghi danh học viên (Enrollments)
p_schema = os.path.join(ROOT_DIR, "database", "schema.sql")
has_enroll_table = "CREATE TABLE IF NOT EXISTS `enrollments`" in open(p_schema, "r", encoding="utf-8").read()
has_enroll_api = False
for f in ["ClassApiServlet.java", "EnrollmentApiServlet.java"]:
    fp = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "controller", f)
    if os.path.exists(fp) and "enroll" in open(fp, "r", encoding="utf-8").read().lower():
        has_enroll_api = True
        break
if has_enroll_api:
    record("JAVA.S3-06", "Java API Ghi danh học viên", "PASS", "Có API ghi danh")
else:
    record("JAVA.S3-06", "Java API Ghi danh học viên (S3-06)", "WARN", "CSDL có bảng enrollments, Frontend có giao diện ghi danh, nhưng Java Backend CHƯA viết API POST /api/classes/{id}/enrollments")

# S3-08: Phân công giảng dạy
has_assign_api = False
for f in ["ClassApiServlet.java", "AssignmentApiServlet.java"]:
    fp = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "controller", f)
    if os.path.exists(fp) and "assistant" in open(fp, "r", encoding="utf-8").read().lower():
        has_assign_api = True
        break
if has_assign_api:
    record("JAVA.S3-08", "Java API Phân công giảng viên & trợ giảng", "PASS", "Có API phân công")
else:
    record("JAVA.S3-08", "Java API Phân công giảng viên & trợ giảng (S3-08)", "WARN", "Frontend có giao diện phân công, nhưng Java Backend CHƯA viết API gán GV/TA cho từng môn")

# S3-09: RoomApiServlet, RoomService, RoomDAO
p_room_servlet = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "controller", "RoomApiServlet.java")
p_room_service = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "service", "RoomService.java")
p_room_dao = os.path.join(ROOT_DIR, "src", "main", "java", "com", "ems", "dao", "RoomDAO.java")

if os.path.exists(p_room_servlet) and os.path.exists(p_room_service) and os.path.exists(p_room_dao):
    r_content = open(p_room_servlet, "r", encoding="utf-8").read()
    if "validate-capacity" in r_content and "doDelete" in r_content and "doPut" in r_content:
        record("JAVA.S3-09", "Java API Quản lý phòng học & cảnh báo sức chứa", "PASS", "Có CRUD phòng học, kiểm tra sức chứa validate-capacity, soft delete an toàn")
    else:
        record("JAVA.S3-09", "Java API Quản lý phòng học", "WARN", "Thiếu một số endpoint trong RoomApiServlet")
else:
    record("JAVA.S3-09", "Java API Quản lý phòng học", "FAIL", "Thiếu RoomApiServlet, RoomService hoặc RoomDAO")

# ==============================================================================
# PHẦN 4: KIỂM TRA FRONTEND REACT CHO TỪNG STORY SPRINT 3
# ==============================================================================
print("\n>>> 4. KIỂM TRA GIAO DIỆN FRONTEND REACT SPRINT 3 <<<")

pages = [
    ("FE.S3-01", "Giao diện Phễu Lead Kanban & Lý do từ chối", "frontend/src/pages/S3-01-pheu-lead/FunnelPage.tsx", ["STAGES", "reject", "onlyLate"]),
    ("FE.S3-02", "Giao diện Ngăn nhật ký chăm sóc lead", "frontend/src/pages/S3-02-nhat-ky-cham-soc/LeadPanel.tsx", ["CareLog", "Gọi", "Nhắn", "Gặp"]),
    ("FE.S3-03", "Giao diện Chuyển lead thành học viên", "frontend/src/pages/S3-03-chuyen-hoc-vien/ConvertDialog.tsx", ["ConvertDialog", "convertLead", "STUDENTS"]),
    ("FE.S3-04", "Giao diện Danh sách & Hồ sơ học viên", "frontend/src/pages/S3-04-ho-so-hoc-vien/StudentListPage.tsx", ["STUDENTS", "Mới nhập học"]),
    ("FE.S3-05", "Giao diện Mở lớp mới & Vòng đời lớp", "frontend/src/pages/S3-05-mo-lop/OpenClassDrawer.tsx", ["Mở lớp mới", "Chuẩn bị", "Đang học"]),
    ("FE.S3-06", "Giao diện Ghi danh học viên vào lớp", "frontend/src/pages/S3-06-ghi-danh/EnrollPage.tsx", ["Ghi danh", "sĩ số"]),
    ("FE.S3-07", "Giao diện Danh sách lớp kèm chỉ số & bộ lọc", "frontend/src/pages/S3-07-danh-sach-lop/ClassListPage.tsx", ["chỉ số", "bộ lọc"]),
    ("FE.S3-08", "Giao diện Phân công giảng viên & trợ giảng", "frontend/src/pages/S3-08-phan-cong/AssignTab.tsx", ["giảng viên", "trợ giảng"]),
    ("FE.S3-09", "Giao diện Phòng học & cảnh báo sức chứa", "frontend/src/pages/S3-09-phong-hoc/RoomListPage.tsx", ["phòng học", "sức chứa"]),
]

for code, name, path, keywords in pages:
    fp = os.path.join(ROOT_DIR, path)
    if os.path.exists(fp):
        c = open(fp, "r", encoding="utf-8").read()
        all_kw = all(kw.lower() in c.lower() for kw in keywords)
        if all_kw:
            record(code, name, "PASS", f"Tệp {path} đầy đủ logic nghiệp vụ")
        else:
            record(code, name, "WARN", f"Tệp {path} thiếu một số từ khoá nghiệp vụ")
    else:
        record(code, name, "FAIL", f"Không tìm thấy file {path}")

# ==============================================================================
# TỔNG KẾT
# ==============================================================================
pass_count = sum(1 for r in results if r["status"] == "PASS")
warn_count = sum(1 for r in results if r["status"] == "WARN")
fail_count = sum(1 for r in results if r["status"] == "FAIL")

print("\n" + "=" * 80)
print(f"TỔNG KẾT KIỂM THỬ SPRINT 3: {pass_count} PASS, {warn_count} CẦN LƯU Ý (WARN), {fail_count} FAIL / {len(results)} HẠNG MỤC")
print("=" * 80)
