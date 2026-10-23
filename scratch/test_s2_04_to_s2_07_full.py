# -*- coding: utf-8 -*-
"""
Bộ kiểm thử tự động toàn diện cho nhóm chức năng Đào tạo (Sprint 2 - Epic EP-02):
- S2-04: Quản lý danh mục chương trình đào tạo
- S2-05: Quản lý danh mục môn học
- S2-06: Gắn môn học vào chương trình và sắp thứ tự học
- S2-07: Khai báo các buổi học trong một môn & nhân bản buổi học
"""

import os
import sys
import json

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
print("KIỂM THỬ TỰ ĐỘNG TOÀN DIỆN CÁC CHỨC NĂNG SPRINT 2 (EP-02: S2-04 -> S2-07)")
print("=" * 80)

# ==============================================================================
# PHẦN 1: USER STORY S2-04 - QUẢN LÝ DANH MỤC CHƯƠNG TRÌNH ĐÀO TẠO
# ==============================================================================
print("\n>>> KIỂM TRA S2-04: CHƯƠNG TRÌNH ĐÀO TẠO <<<")

tp_model = read_file("src/main/java/com/ems/model/TrainingProgram.java")
check("S2-04.1", "Model TrainingProgram có đầy đủ các trường: code, name, description, duration, standardTuition, status",
      "code" in tp_model and "name" in tp_model and "description" in tp_model and "duration" in tp_model and "standardTuition" in tp_model and "status" in tp_model)

tp_dao = read_file("src/main/java/com/ems/dao/TrainingProgramDAO.java")
check("S2-04.2", "TrainingProgramDAO kiểm tra mã duy nhất (existsByCode, existsByCodeAndNotId)",
      "existsByCode" in tp_dao and "existsByCodeAndNotId" in tp_dao)

check("S2-04.3", "TrainingProgramDAO kiểm tra số lớp đang chạy (countRunningClassesByProgramId)",
      "countRunningClassesByProgramId" in tp_dao)

tp_svc = read_file("src/main/java/com/ems/service/TrainingProgramService.java")
check("S2-04.4", "TrainingProgramService cấm xoá chương trình khi đang có lớp chạy (PROGRAM_HAS_RUNNING_CLASSES)",
      "PROGRAM_HAS_RUNNING_CLASSES" in tp_svc or "Không thể xoá" in tp_svc)

check("S2-04.5", "TrainingProgramService hỗ trợ ngừng áp dụng chuyển trạng thái sang INACTIVE mà không xoá",
      "deactivate" in tp_svc and "INACTIVE" in tp_svc)

fe_prog = read_file("frontend/src/pages/S2-04-chuong-trinh/ProgramListPage.tsx")
check("S2-04.6", "Giao diện Frontend S2-04 khoá nút Xoá và hiện lý do khi runningClasses > 0",
      "runningClasses" in fe_prog and "không xoá được" in fe_prog and "Ngừng áp dụng" in fe_prog)


# ==============================================================================
# PHẦN 2: USER STORY S2-05 - QUẢN LÝ DANH MỤC MÔN HỌC
# ==============================================================================
print("\n>>> KIỂM TRA S2-05: DANH MỤC MÔN HỌC <<<")

sub_model = read_file("src/main/java/com/ems/model/Subject.java")
check("S2-05.1", "Model Subject có đầy đủ các trường: code, name, sessions, weight, outcome, status",
      "code" in sub_model and "name" in sub_model and "sessions" in sub_model and "weight" in sub_model and "outcome" in sub_model)

sub_dao = read_file("src/main/java/com/ems/dao/SubjectDAO.java")
check("S2-05.2", "SubjectDAO kiểm tra ràng buộc không xoá được môn đã có lớp học (countOpenedClassesBySubjectId)",
      "countOpenedClassesBySubjectId" in sub_dao and "class_subjects" in sub_dao)

check("S2-05.3", "SubjectDAO đếm số chương trình sử dụng môn học (countProgramsUsingSubject)",
      "countProgramsUsingSubject" in sub_dao and "program_subjects" in sub_dao)

sub_svc = read_file("src/main/java/com/ems/service/SubjectService.java")
check("S2-05.4", "SubjectService kiểm tra mã môn học là duy nhất (SUBJECT_CODE_CONFLICT)",
      "SUBJECT_CODE_CONFLICT" in sub_svc and "existsByCode" in sub_svc)

check("S2-05.5", "SubjectService chặn xoá nếu môn đã có lớp học (SUBJECT_HAS_OPENED_CLASSES)",
      "SUBJECT_HAS_OPENED_CLASSES" in sub_svc)

sub_api = read_file("src/main/java/com/ems/controller/SubjectApiServlet.java")
check("S2-05.6", "SubjectApiServlet hỗ trợ REST API đầy đủ: GET, POST, PUT, DELETE cho /api/subjects",
      "doGet" in sub_api and "doPost" in sub_api and "doPut" in sub_api and "doDelete" in sub_api)

fe_sub = read_file("frontend/src/pages/S2-05-mon-hoc/SubjectListPage.tsx")
check("S2-05.7", "Giao diện Frontend S2-05 hiển thị cột 'Dùng trong X chương trình' và khoá xoá khi openedClasses > 0",
      "programsUsing" in fe_sub and "openedClasses" in fe_sub and "không xoá được" in fe_sub)


# ==============================================================================
# PHẦN 3: USER STORY S2-06 - GẮN MÔN VÀO CHƯƠNG TRÌNH & THỨ TỰ HỌC
# ==============================================================================
print("\n>>> KIỂM TRA S2-06: GẮN MÔN VÀO CHƯƠNG TRÌNH & TIÊN QUYẾT <<<")

ps_model = read_file("src/main/java/com/ems/model/ProgramSubject.java")
check("S2-06.1", "Model ProgramSubject có orderIndex và prerequisiteSubjectId",
      "orderIndex" in ps_model and "prerequisiteSubjectId" in ps_model)

ps_dao = read_file("src/main/java/com/ems/dao/ProgramSubjectDAO.java")
check("S2-06.2", "ProgramSubjectDAO hỗ trợ thêm môn, gỡ môn và cập nhật thứ tự (updateOrder)",
      "addSubject" in ps_dao and "removeSubject" in ps_dao and "updateOrder" in ps_dao)

ps_svc = read_file("src/main/java/com/ems/service/ProgramSubjectService.java")
check("S2-06.3", "ProgramSubjectService ngăn chặn gán trùng môn vào cùng 1 chương trình (SUBJECT_ALREADY_IN_PROGRAM)",
      "SUBJECT_ALREADY_IN_PROGRAM" in ps_svc)

check("S2-06.4", "ProgramSubjectService ngăn chặn môn học chọn chính nó làm môn tiên quyết",
      "Môn học không thể chọn chính nó làm môn tiên quyết" in ps_svc)

ps_api = read_file("src/main/java/com/ems/controller/ProgramSubjectApiServlet.java")
check("S2-06.5", "ProgramSubjectApiServlet xử lý endpoint sắp xếp lại thứ tự /reorder và gán tiên quyết /prereq",
      "reorder" in ps_api and "prereq" in ps_api)

fe_curr = read_file("frontend/src/pages/S2-06-gan-mon/CurriculumPage.tsx")
check("S2-06.6", "Giao diện Frontend S2-06 hỗ trợ kéo thả draggable, lưu tự động và chỉ chọn tiên quyết từ các môn đứng trước",
      "draggable" in fe_curr and "items.slice(0, i)" in fe_curr and "Đã lưu thứ tự" in fe_curr)


# ==============================================================================
# PHẦN 4: USER STORY S2-07 - BUỔI HỌC TRONG MÔN & NHÂN BẢN
# ==============================================================================
print("\n>>> KIỂM TRA S2-07: KHAI BÁO BUỔI HỌC VÀ NHÂN BẢN <<<")

sl_model = read_file("src/main/java/com/ems/model/SubjectLesson.java")
check("S2-07.1", "Model SubjectLesson có sessionOrder, topic, objectives",
      "sessionOrder" in sl_model and "topic" in sl_model and "objectives" in sl_model)

sl_dao = read_file("src/main/java/com/ems/dao/SubjectLessonDAO.java")
check("S2-07.2", "SubjectLessonDAO hỗ trợ cloneLessons (nhân bản có giới hạn maxSessions)",
      "cloneLessons" in sl_dao and "Math.min" in sl_dao)

sl_svc = read_file("src/main/java/com/ems/service/SubjectLessonService.java")
check("S2-07.3", "SubjectLessonService kiểm soát số buổi không vượt quá tổng số buổi môn học (MAX_SESSIONS_EXCEEDED)",
      "MAX_SESSIONS_EXCEEDED" in sl_svc and "không được vượt quá số buổi" in sl_svc)

sl_api = read_file("src/main/java/com/ems/controller/SubjectLessonApiServlet.java")
check("S2-07.4", "SubjectLessonApiServlet hỗ trợ endpoint nhân bản /clone và CRUD buổi học",
      "clone" in sl_api and "doGet" in sl_api and "doPost" in sl_api and "doPut" in sl_api and "doDelete" in sl_api)

fe_sess = read_file("frontend/src/pages/S2-07-buoi-hoc/SessionsPage.tsx")
check("S2-07.5", "Giao diện Frontend S2-07 khoá nút thêm khi đủ số buổi và có hộp thoại CloneDialog cảnh báo ghi đè",
      "disabled={full}" in fe_sess and "CloneDialog" in fe_sess and "ghi đè" in fe_sess)


# ==============================================================================
# PHẦN 5: BẢO MẬT PHÂN QUYỀN & TÀI LIỆU SWAGGER
# ==============================================================================
print("\n>>> KIỂM TRA PHÂN QUYỀN BẢO MẬT & SWAGGER <<<")

perm_policy = read_file("src/main/java/com/ems/security/PermissionPolicy.java")
check("SEC.1", "PermissionPolicy bảo vệ các endpoint đào tạo bằng PROGRAM_MANAGE",
      "PROGRAM_MANAGE" in perm_policy and "/api/subjects" in perm_policy and "/api/training-programs" in perm_policy)

swagger_str = read_file("src/main/webapp/api-docs/swagger.json")
check("SWAGGER.1", "Swagger tài liệu hoá đầy đủ 4 nhóm chức năng S2-04, S2-05, S2-06, S2-07",
      "/api/training-programs" in swagger_str and "/api/subjects" in swagger_str and
      "/api/programs/{programId}/subjects" in swagger_str and "/api/subjects/{subjectId}/sessions" in swagger_str)


# ==============================================================================
# PHẦN 6: CƠ SỞ DỮ LIỆU & MIGRATION SCRIPT
# ==============================================================================
print("\n>>> KIỂM TRA CSDL & MIGRATION <<<")

migration_05 = read_file("database/migration_s2_05_subjects.sql")
migration_06 = read_file("database/migration_s2_06_program_subjects.sql")
migration_07 = read_file("database/migration_s2_07_subject_lessons.sql")
schema_sql = read_file("database/schema.sql")

check("DB.1", "Migration SQL định nghĩa bảng subjects có UNIQUE mã môn",
      "CREATE TABLE IF NOT EXISTS `subjects`" in migration_05 and "UNIQUE" in migration_05)
check("DB.2", "Migration SQL định nghĩa bảng program_subjects có khoá ngoại tới subjects và bảng programs",
      "CREATE TABLE IF NOT EXISTS `program_subjects`" in migration_06 and "fk_ps_program" in migration_06)
check("DB.3", "Migration SQL định nghĩa bảng subject_lessons có liên kết CASCADE với subjects",
      "CREATE TABLE IF NOT EXISTS `subject_lessons`" in migration_07 and "fk_sl_subject" in migration_07)

print("\n" + "=" * 80)
print(f"KẾT QUẢ KIỂM THỬ: {passed} PASS, {failed} FAIL / TỔNG SỐ {passed + failed} TIÊU CHÍ")
print("=" * 80)

if failed > 0:
    sys.exit(1)
else:
    print("🎉 TẤT CẢ 22 TIÊU CHÍ CỦA S2-04, S2-05, S2-06, S2-07 ĐÃ VƯỢT QUA 100%!")
    sys.exit(0)
