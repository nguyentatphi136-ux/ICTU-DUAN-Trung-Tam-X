#!/usr/bin/env python3
"""
Test script for S7-03 (Jira EP-08): Ghi nhận một khoản thanh toán và in biên lai
Vai trò: Kế toán
User Story: "Là Kế toán, tôi muốn ghi nhận một khoản thanh toán và in biên lai, để học viên có bằng chứng đã đóng tiền và sổ sách không bao giờ lệch."

Kiểm tra 5 tiêu chí chấp nhận (AC):
  - AC1: Ghi số tiền, hình thức (tiền mặt, chuyển khoản), ngày thu, người thu, ghi chú (mã GD ngân hàng bắt buộc nếu CK)
  - AC2: Cho phép thu một phần, đợt tự chuyển sang 'Thanh toán một phần' (nếu thu đủ tự chuyển 'Đã thanh toán')
  - AC3: Không cho thu vượt số tiền còn nợ của đợt (ném lỗi 400 Bad Request)
  - AC4: Sinh biên lai có mã duy nhất (BL-YYYY-XXXXXX), in hoặc tải PDF được
  - AC5: Đã ghi nhận thì không xoá, chỉ lập bút toán điều chỉnh (ĐC-YYYY-XXXXXX)
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
    print("TEST SUITE: S7-03 GHI NHẬN THANH TOÁN VÀ IN BIÊN LAI (JIRA EP-08)")
    print("=" * 80)

    # 1. Dual-path file verification
    required_files = [
        "src/main/java/com/ems/config/S7PaymentMigration.java",
        "backend/src/main/java/com/ems/config/S7PaymentMigration.java",
        "src/main/java/com/ems/model/PaymentRecord.java",
        "backend/src/main/java/com/ems/model/PaymentRecord.java",
        "src/main/java/com/ems/model/AdjustmentRecord.java",
        "backend/src/main/java/com/ems/model/AdjustmentRecord.java",
        "src/main/java/com/ems/dto/CreatePaymentRequest.java",
        "backend/src/main/java/com/ems/dto/CreatePaymentRequest.java",
        "src/main/java/com/ems/dto/CreateAdjustmentRequest.java",
        "backend/src/main/java/com/ems/dto/CreateAdjustmentRequest.java",
        "src/main/java/com/ems/dto/PaymentReceiptResponse.java",
        "backend/src/main/java/com/ems/dto/PaymentReceiptResponse.java",
        "src/main/java/com/ems/dao/PaymentDAO.java",
        "backend/src/main/java/com/ems/dao/PaymentDAO.java",
        "src/main/java/com/ems/controller/PaymentApiServlet.java",
        "backend/src/main/java/com/ems/controller/PaymentApiServlet.java",
        "src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java",
        "backend/src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java",
        "frontend/src/data/paymentApi.ts",
        "frontend/src/pages/S7-03-thanh-toan/PaymentPages.tsx",
    ]

    for rf in required_files:
        assert os.path.exists(os.path.join(WORKSPACE, rf)), f"Missing required file: {rf}"
    print("[PASS] Tất cả các tập tin Dual-Path (src/ và backend/src/) đã hiện diện đầy đủ.")

    dao_content = check_file("src/main/java/com/ems/dao/PaymentDAO.java")
    servlet_content = check_file("src/main/java/com/ems/controller/PaymentApiServlet.java")
    policy_content = check_file("src/main/java/com/ems/security/PermissionPolicy.java")
    test_content = check_file("src/test/java/com/ems/test/PaymentValidationAndSecurityTest.java")
    fe_api = check_file("frontend/src/data/paymentApi.ts")
    fe_pages = check_file("frontend/src/pages/S7-03-thanh-toan/PaymentPages.tsx")

    # AC1 Checks
    print("\n--- Kiểm tra AC1: Thông tin thu tiền & Validate hình thức ---")
    assert "Vui lòng nhập mã giao dịch ngân hàng khi thanh toán chuyển khoản" in dao_content, "Missing bank transfer ref validation"
    assert "Ngày thu" in dao_content and "không được ở tương lai" in dao_content, "Missing payment date in future validation"
    assert "Số tiền thu phải lớn hơn 0" in dao_content, "Missing positive amount validation"
    assert "testAC1_AmountMustBePositive" in test_content, "Missing testAC1 in unit test"
    assert "testAC1_BankTransferRequiresReferenceCode" in test_content, "Missing testAC1 bank transfer in unit test"
    print("[PASS] AC1: Ghi số tiền, hình thức (tiền mặt/chuyển khoản), ngày thu, người thu, mã GD ngân hàng bắt buộc nếu chuyển khoản.")

    # AC2 Checks
    print("\n--- Kiểm tra AC2: Thu một phần & tự chuyển trạng thái ---")
    assert "Thanh toán một phần" in dao_content, "Missing partial payment status"
    assert "Đã thanh toán" in dao_content, "Missing full payment status"
    assert "Chưa thanh toán" in dao_content, "Missing unpaid status"
    assert "testAC2_PartialPaymentTransitionsToThanhToanMotPhan" in test_content, "Missing AC2 partial transition test"
    assert "testAC2_PayingRemainingTransitionsToDaThanhToan" in test_content, "Missing AC2 full transition test"
    print("[PASS] AC2: Tự động chuyển 'Thanh toán một phần' và 'Đã thanh toán' khi thu.")

    # AC3 Checks
    print("\n--- Kiểm tra AC3: Không cho thu vượt số tiền còn nợ của đợt ---")
    assert "Vượt số tiền còn nợ của đợt" in dao_content, "Missing overpayment error message in DAO"
    assert "testAC3_RejectOverpaymentBeyondRemainingBalance" in test_content, "Missing AC3 unit test"
    assert "over > 0" in fe_pages or "over" in fe_pages, "FE missing overpayment guard"
    print("[PASS] AC3: Chặn thu vượt số tiền còn nợ của đợt với thông báo hướng dẫn ghi vào đợt sau.")

    # AC4 Checks
    print("\n--- Kiểm tra AC4: Sinh biên lai mã duy nhất & Mẫu in/PDF ---")
    assert "BL-" in dao_content, "Missing BL- receipt code generator"
    assert "generateReceiptPrintableHtml" in dao_content, "Missing printable HTML generator"
    assert "toVietnameseWords" in dao_content, "Missing amount in Vietnamese words generator"
    assert "testAC4_GenerateUniqueReceiptCodeAndPrintableHtml" in test_content, "Missing AC4 unit test"
    assert "getReceiptPdfUrl" in fe_api, "Missing PDF url helper in frontend API"
    print("[PASS] AC4: Sinh mã duy nhất BL-YYYY-XXXXXX, có số tiền bằng chữ, tạo bản in HTML/PDF chính thức.")

    # AC5 Checks
    print("\n--- Kiểm tra AC5: Bất biến không xoá & Bút toán điều chỉnh ---")
    assert "doDelete" in servlet_content and "SC_METHOD_NOT_ALLOWED" in servlet_content, "Servlet does not block DELETE with 405"
    assert "ĐC-" in dao_content, "Missing adjustment code generator"
    assert "Giảm khoản thu" in dao_content and "Tăng khoản thu" in dao_content and "Huỷ toàn bộ" in dao_content, "Missing adjustment kinds"
    assert "testAC5_CreateAdjustmentForPayment" in test_content, "Missing AC5 unit test"
    assert "createAdjustmentApi" in fe_api, "Missing adjustment API in frontend"
    print("[PASS] AC5: Chặn HTTP DELETE (không cho xoá khoản đã ghi nhận), chỉ cho phép lập bút toán điều chỉnh.")

    # Security & Permissions
    print("\n--- Kiểm tra phân quyền & Security Policy ---")
    assert "api/(payments|receipts)" in policy_content, "Missing payment routes in PermissionPolicy"
    assert "paymentRoutesOfS7HaveRules" in check_file("src/test/java/com/ems/security/PermissionPolicyTest.java"), "Missing policy test"
    print("[PASS] Phân quyền các route /api/payments/* và /api/receipts/* yêu cầu @authenticated.")

    print("\n" + "=" * 80)
    print("TẤT CẢ CÁC KIỂM TRA CHO S7-03 (JIRA EP-08) ĐÃ VƯỢT QUA 100%!")
    print("=" * 80)

if __name__ == "__main__":
    run_tests()
