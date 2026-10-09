# -*- coding: utf-8 -*-
"""
Integrated Test Runner across Sprint 1 and Sprint 2
Hệ thống Quản lý Đào tạo Trung tâm (TMS/EMS)
"""

import subprocess
import sys
import os

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

WORKSPACE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def run_command(name, cmd, cwd=WORKSPACE):
    print("\n" + "=" * 80)
    print(f"▶ CHẠY KIỂM THỬ: {name}")
    print("=" * 80)
    res = subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, encoding='utf-8', errors='replace', shell=True)
    print(res.stdout)
    if res.stderr:
        print(res.stderr, file=sys.stderr)
    if res.returncode == 0:
        print(f"✔ {name}: ĐẠT (PASS)")
        return True
    else:
        print(f"❌ {name}: THẤT BẠI (FAIL) - Mã lỗi: {res.returncode}")
        return False

def main():
    print("*" * 80)
    print("TỔNG HỢP KIỂM THỬ TỰ ĐỘNG TOÀN DIỆN DỰ ÁN (SPRINT 1 & SPRINT 2)")
    print("*" * 80)

    results = []

    # 1. Frontend Build & Typecheck
    results.append(("Frontend Build (Vite)", run_command("Frontend Vite Build", ["npm", "run", "build"], cwd=os.path.join(WORKSPACE, "frontend"))))
    results.append(("Frontend Typecheck (TypeScript)", run_command("Frontend TypeScript Typecheck", ["npm", "run", "typecheck"], cwd=os.path.join(WORKSPACE, "frontend"))))

    # 2. Sprint 1 Test Suite
    results.append(("Sprint 1 (S1-01 -> S1-10)", run_command("Sprint 1 Comprehensive (10 Stories)", [sys.executable, "scratch/test_all_s1_stories.py"])))

    # 3. Sprint 2 Test Suites
    results.append(("Sprint 2: S2-01 & S2-02", run_command("Sprint 2 (Excel Import & Profile)", [sys.executable, "scratch/test_s2_01_and_s2_02.py"])))
    results.append(("Sprint 2: S2-04", run_command("Sprint 2 (Training Programs)", [sys.executable, "scratch/test_s2_04_training_programs.py"])))
    results.append(("Sprint 2: S2-09", run_command("Sprint 2 (Leads CRUD & Validation)", ["node", "scratch/test_s2_09_lead.js"])))

    print("\n" + "=" * 80)
    print("TỔNG KẾT KẾT QUẢ KIỂM THỬ TOÀN BỘ HỆ THỐNG")
    print("=" * 80)
    all_passed = True
    for name, passed in results:
        status_str = "✔ ĐẠT (PASS)" if passed else "❌ THẤT BẠI (FAIL)"
        print(f"  • {name:40}: {status_str}")
        if not passed:
            all_passed = False

    print("=" * 80)
    if all_passed:
        print("🎉 TẤT CẢ CÁC BỘ KIỂM THỬ ĐÃ VƯỢT QUA 100%! HỆ THỐNG ĐỒNG BỘ VÀ HOẠT ĐỘNG HOÀN HẢO!")
        sys.exit(0)
    else:
        print("⚠ CÓ BÀI TEST CHƯA ĐẠT. VUI LÒNG KIỂM TRA LẠI CHI TIẾT.")
        sys.exit(1)

if __name__ == "__main__":
    main()
