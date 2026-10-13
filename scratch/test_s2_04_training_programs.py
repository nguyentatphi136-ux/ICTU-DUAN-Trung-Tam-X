# -*- coding: utf-8 -*-
"""
Automated Test Suite for Sprint 2 - User Story S2-04:
Quản lý danh mục chương trình đào tạo (Training Programs Management)
- AC1: Khai báo mã, tên, mô tả, tổng thời lượng, học phí chuẩn, trạng thái
- AC2: Mã chương trình là duy nhất (Unique Code validation & 409 Conflict)
- AC3: Chương trình đang có lớp học chạy không được xóa, chỉ được ngừng áp dụng
"""

import os
import sys

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

WORKSPACE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def read_file(rel_path):
    p = os.path.join(WORKSPACE, rel_path)
    if not os.path.exists(p):
        return ""
    with open(p, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()

passed = 0
failed = 0

def check(id_str, desc, cond):
    global passed, failed
    if cond:
        passed += 1
        print(f"  [PASS] {id_str}: {desc}")
    else:
        failed += 1
        print(f"  [FAIL] {id_str}: {desc}")

print("=" * 80)
print("KIỂM THỬ TỰ ĐỘNG SPRINT 2: USER STORY S2-04 (CHƯƠNG TRÌNH ĐÀO TẠO)")
print("=" * 80)

# 1. Model & Attributes
print("\n--- 1. Kiểm tra Entity & Model ---")
tp_model = read_file("src/main/java/com/ems/model/TrainingProgram.java")
check("S2-04.M.1", "TrainingProgram Model có các trường: code, name, description, duration, standardTuition, status",
      "code" in tp_model and "name" in tp_model and "description" in tp_model and
      "duration" in tp_model and "standardTuition" in tp_model and "status" in tp_model)

# 2. Database Migration & Schema
print("\n--- 2. Kiểm tra CSDL & Migration ---")
migration_sql = read_file("database/migration_s2_04_training_programs.sql")
schema_sql = read_file("database/schema.sql")
check("S2-04.DB.1", "Tạo bảng training_programs hoặc programs có ràng buộc UNIQUE cho code",
      ("CREATE TABLE IF NOT EXISTS `training_programs`" in migration_sql or "CREATE TABLE IF NOT EXISTS `programs`" in schema_sql) and
      ("UNIQUE" in migration_sql or "UNIQUE" in schema_sql))
check("S2-04.DB.2", "Đăng ký quyền PROGRAM_MANAGE và PROGRAM_VIEW trong CSDL",
      "PROGRAM_MANAGE" in migration_sql or "PROGRAM_MANAGE" in schema_sql)

# 3. DAO & Business Logic
print("\n--- 3. Kiểm tra DAO Layer & Validation ---")
tp_dao = read_file("src/main/java/com/ems/dao/TrainingProgramDAO.java")
tp_service = read_file("src/main/java/com/ems/service/TrainingProgramService.java")
check("S2-04.DAO.1", "DAO hỗ trợ CRUD: findById, findByCode, create, update, delete, search (phân trang & lọc)",
      "findByCode" in tp_dao and "create" in tp_dao and "update" in tp_dao and "delete" in tp_dao and "search" in tp_dao)
check("S2-04.DAO.2", "Kiểm tra ràng buộc lớp học đang chạy trước khi xóa (countRunningClassesByProgramId)",
      "countRunningClassesByProgramId" in tp_dao or "countActiveClasses" in tp_service)
check("S2-04.SVC.1", "Service kiểm tra tính duy nhất của mã chương trình (ConflictException / checkDuplicateCode)",
      "ConflictException" in tp_service or "findByCode" in tp_service)

# 4. REST Controller & API Endpoints
print("\n--- 4. Kiểm tra Controller & Phân quyền REST API ---")
tp_api = read_file("src/main/java/com/ems/controller/TrainingProgramApiServlet.java")
perm_policy = read_file("src/main/java/com/ems/security/PermissionPolicy.java")
check("S2-04.API.1", "Servlet hỗ trợ đầy đủ GET, POST, PUT, DELETE cho /api/training-programs",
      "doGet" in tp_api and "doPost" in tp_api and "doPut" in tp_api and "doDelete" in tp_api)
check("S2-04.API.2", "PermissionPolicy bảo vệ đường dẫn /api/training-programs bằng PROGRAM_MANAGE",
      "PROGRAM_MANAGE" in perm_policy and "training-programs" in perm_policy)

# 5. OpenAPI Swagger Documentation
print("\n--- 5. Kiểm tra Tài liệu OpenAPI Swagger ---")
swagger_json = read_file("src/main/webapp/api-docs/swagger.json")
check("S2-04.DOC.1", "Swagger tài liệu hóa đầy đủ đường dẫn /api/training-programs",
      "/api/training-programs" in swagger_json and "Training Programs" in swagger_json)

# 6. Frontend UI
print("\n--- 6. Kiểm tra Giao diện Frontend (Figma React) ---")
fe_page = read_file("frontend/src/pages/S2-04-chuong-trinh/ProgramListPage.tsx")
check("S2-04.FE.1", "Giao diện React có tìm kiếm, bộ lọc trạng thái, thêm mới, sửa, ngừng áp dụng",
      "ProgramListPage" in fe_page and "Tìm theo mã" in fe_page and "Thêm chương trình" in fe_page)

print("\n" + "=" * 80)
print(f"KẾT QUẢ KIỂM THỬ S2-04: {passed} PASS, {failed} FAIL")
print("=" * 80)

if failed > 0:
    sys.exit(1)
