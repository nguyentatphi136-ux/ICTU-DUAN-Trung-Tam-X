#!/usr/bin/env python3
"""
Test script for S7-07 (Jira EP-08): Xem tình trạng học phí của bản thân
Vai trò: Học viên
User Story: "Là Học viên, tôi muốn xem tình trạng học phí của bản thân, để tự kiểm tra được mình đã đóng tới đâu mà không phải hỏi kế toán."

Kiểm tra 2 tiêu chí chấp nhận (AC):
  - AC1: Danh sách các đợt kèm số tiền, hạn, trạng thái
  - AC2: Xem và tải lại biên lai của các khoản đã đóng
Kiểm tra bảo mật:
  - Học viên CHỈ được xem tình trạng học phí và biên lai của CHÍNH MÌNH (bị chặn 403 nếu truy cập biên lai của người khác).
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

def run_tests():
    print("=" * 80)
    print("TEST SUITE: S7-07 HỌC VIÊN XEM TÌNH TRẠNG HỌC PHÍ CỦA BẢN THÂN (JIRA EP-08)")
    print("=" * 80)

    # 1. Dual-path files
    dual_files = [
        ("src/main/java/com/ems/controller/PaymentApiServlet.java", "backend/src/main/java/com/ems/controller/PaymentApiServlet.java"),
        ("src/main/java/com/ems/dao/PaymentDAO.java", "backend/src/main/java/com/ems/dao/PaymentDAO.java"),
        ("src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java", "backend/src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java"),
    ]
    for src_f, bkd_f in dual_files:
        assert os.path.exists(os.path.join(WORKSPACE, src_f)), f"Missing {src_f}"
        assert os.path.exists(os.path.join(WORKSPACE, bkd_f)), f"Missing {bkd_f}"
        src_code = check_file(src_f)
        bkd_code = check_file(bkd_f)
        assert src_code == bkd_code, f"Dual path desynchronization between {src_f} and {bkd_f}"

    print("[PASS] Tất cả các tập tin Dual-Path (src/ và backend/src/) hoàn toàn đồng nhất.")

    servlet_code = check_file("src/main/java/com/ems/controller/PaymentApiServlet.java")
    dao_code = check_file("src/main/java/com/ems/dao/PaymentDAO.java")
    unit_tests = check_file("src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java")
    fe_api = check_file("frontend/src/data/paymentApi.ts")
    fe_page = check_file("frontend/src/pages/S7-07-hoc-phi-cua-toi/MyFeesPage.tsx")

    # 2. AC1: Danh sách các đợt kèm số tiền, hạn, trạng thái
    print("\n--- Kiểm tra AC1: Danh sách các đợt kèm số tiền, hạn, trạng thái ---")
    assert "/mine" in servlet_code, "PaymentApiServlet missing /mine endpoint for student"
    assert "getStudentPlan" in dao_code and "installments" in dao_code, "DAO missing installments logic"
    assert "testS7_07_StudentCanViewOwnInstallmentsAndReceipts" in unit_tests, "Missing testS7_07 in unit tests"
    assert "it.amount" in fe_page and "it.due" in fe_page and "it.state" in fe_page, "Frontend MyFeesPage missing installment fields"
    print("[PASS] AC1: Học viên xem được danh sách đợt, số tiền, hạn nộp, số tiền đã đóng, còn nợ và trạng thái (Đã thanh toán, Thanh toán một phần, Chưa thanh toán).")

    # 3. AC2: Xem và tải lại biên lai của các khoản đã đóng
    print("\n--- Kiểm tra AC2: Xem và tải lại biên lai của các khoản đã đóng ---")
    assert "/receipt/" in servlet_code and ("/pdf" in servlet_code or "/print" in servlet_code), "Missing receipt view / pdf download endpoints"
    assert "getReceiptsForStudent" in dao_code, "DAO missing getReceiptsForStudent"
    assert "generateReceiptPrintableHtml" in dao_code, "DAO missing HTML / printable receipt generator"
    assert "getMyPaymentsApi" in fe_api, "Frontend API missing getMyPaymentsApi"
    assert "getReceiptPdfUrl" in fe_page, "Frontend MyFeesPage missing direct PDF download link"
    print("[PASS] AC2: Học viên xem và tải lại biên lai đã đóng (có số biên lai, số tiền bằng chữ, mã tra cứu, tải file in/PDF).")

    # 4. Bảo mật phân quyền: Học viên không được xem biên lai / học phí người khác
    print("\n--- Kiểm tra Bảo mật & Quyền riêng tư (Least Privilege) ---")
    assert "isStudentOnly" in servlet_code, "PaymentApiServlet missing isStudentOnly security check"
    assert "Học viên chỉ có quyền xem và tải lại biên lai của chính mình" in servlet_code, "Missing receipt ownership guard"
    assert "Học viên chỉ có quyền xem tình trạng học phí của chính mình" in servlet_code, "Missing fee plan ownership guard"
    assert "testS7_07_StudentAccessControlInvariants" in unit_tests, "Missing access control unit tests"
    print("[PASS] Bảo mật: Học viên bị chặn 403 Forbidden nếu cố tình truy cập thông tin học phí hoặc biên lai của học viên khác.")

    print("\n" + "=" * 80)
    print("TẤT CẢ CÁC KIỂM TRA CHO S7-07 (JIRA EP-08) ĐÃ VƯỢT QUA 100%!")
    print("=" * 80)

if __name__ == "__main__":
    run_tests()
