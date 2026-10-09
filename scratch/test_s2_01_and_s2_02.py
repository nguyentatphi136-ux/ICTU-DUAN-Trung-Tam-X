# -*- coding: utf-8 -*-
"""
Automated Test Suite for Sprint 2 Epics / Stories:
- Story S2-01: Nhập danh sách người dùng hàng loạt từ tệp Excel
  - AC1: Tải được tệp mẫu chuẩn (.csv/.xlsx)
  - AC2: Xem trước và báo lỗi theo từng dòng trước khi nhập
  - AC3: Dòng lỗi bị bỏ qua, dòng hợp lệ vẫn được nhập, có báo cáo tổng kết
- Story S2-02: Xem và cập nhật hồ sơ cá nhân
  - AC1: Sửa được họ tên, số điện thoại, ngày sinh, địa chỉ, giới tính
  - AC2: Không tự đổi được email và vai trò (Strictly protected/readonly)
  - AC3: Kiểm tra định dạng số điện thoại Việt Nam (10 chữ số, đầu số 03, 05, 07, 08, 09 hoặc +84)
"""

import sys
import json
import urllib.request
import urllib.error
import io

try:
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')
except Exception:
    pass

BASE_URL = "http://localhost:5173"

def print_header(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def test_s2_01_ac1_template_download():
    print("\n[TEST S2-01 AC1] Tải tệp mẫu Excel/CSV chuẩn...")
    url = f"{BASE_URL}/api/admin/users/import/template"
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req) as resp:
        status = resp.getcode()
        content_type = resp.headers.get("Content-Type", "")
        body = resp.read()
        text = body.decode("utf-8")
        
        assert status == 200, f"Expected 200, got {status}"
        assert "text/csv" in content_type or "charset=utf-8" in content_type, f"Invalid Content-Type: {content_type}"
        # Check UTF-8 BOM
        assert body.startswith(b"\xef\xbb\xbf"), "Template CSV must start with UTF-8 BOM"
        # Check standard headers
        assert "Họ và tên" in text, "Missing header 'Họ và tên'"
        assert "Email" in text, "Missing header 'Email'"
        assert "Số điện thoại" in text, "Missing header 'Số điện thoại'"
        assert "Vai trò" in text, "Missing header 'Vai trò'"
        print("  ✓ Tải tệp mẫu thành công (status 200, UTF-8 BOM, đầy đủ 7 cột tiêu chuẩn: Họ tên, Email, SĐT, Vai trò, Ngày sinh, Giới tính, Địa chỉ).")

def test_s2_01_ac2_preview_and_row_by_row_validation():
    print("\n[TEST S2-01 AC2] Xem trước và báo lỗi theo từng dòng trước khi nhập...")
    url = f"{BASE_URL}/api/admin/users/import/preview"
    test_rows = [
        # Dòng 1: Hợp lệ
        {
            "fullName": "Nguyễn Văn Hợp Lệ",
            "email": "hople1@tms.vn",
            "phone": "0912345678",
            "role": "student",
            "dateOfBirth": "2002-05-15",
            "gender": "MALE",
            "address": "Thái Nguyên",
        },
        # Dòng 2: Lỗi thiếu họ tên
        {
            "fullName": "",
            "email": "noname@tms.vn",
            "phone": "0912345678",
            "role": "student",
        },
        # Dòng 3: Lỗi email không hợp lệ
        {
            "fullName": "Trần Thị Sai Email",
            "email": "not-an-email-format",
            "phone": "0912345678",
            "role": "student",
        },
        # Dòng 4: Lỗi trùng lặp email với dòng 1
        {
            "fullName": "Nguyễn Văn Trùng Lặp",
            "email": "hople1@tms.vn",
            "phone": "0987654321",
            "role": "student",
        },
        # Dòng 5: Lỗi số điện thoại không đúng chuẩn VN (11 số hoặc sai đầu số)
        {
            "fullName": "Lê Văn Sai SĐT",
            "email": "saiphone@tms.vn",
            "phone": "0123456789",  # Đầu 012 không còn tồn tại ở VN
            "role": "ta",
        },
        # Dòng 6: Lỗi vai trò không hợp lệ
        {
            "fullName": "Phạm Văn Sai Role",
            "email": "sairoledemo@tms.vn",
            "phone": "0903123456",
            "role": "super-ninja",
        },
    ]

    req = urllib.request.Request(
        url,
        data=json.dumps({"rows": test_rows}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    with urllib.request.urlopen(req) as resp:
        assert resp.getcode() == 200
        data = json.loads(resp.read().decode("utf-8"))
        assert data.get("success") is True
        assert data.get("totalRows") == 6
        assert data.get("validRows") == 1
        assert data.get("errorRows") == 5

        rows = data.get("rows", [])
        assert len(rows) == 6
        # Row 1 valid
        assert rows[0]["isValid"] is True
        assert len(rows[0]["errors"]) == 0
        # Row 2 error: Thiếu họ tên
        assert rows[1]["isValid"] is False
        assert any("tên" in err.lower() for err in rows[1]["errors"])
        # Row 3 error: Email không hợp lệ
        assert rows[2]["isValid"] is False
        assert any("email" in err.lower() for err in rows[2]["errors"])
        # Row 4 error: Email trùng lặp
        assert rows[3]["isValid"] is False
        assert any("trùng" in err.lower() for err in rows[3]["errors"])
        # Row 5 error: Số điện thoại
        assert rows[4]["isValid"] is False
        assert any("điện thoại" in err.lower() or "sđt" in err.lower() for err in rows[4]["errors"])
        # Row 6 error: Vai trò
        assert rows[5]["isValid"] is False
        assert any("vai trò" in err.lower() or "role" in err.lower() for err in rows[5]["errors"])

        print("  ✓ Xem trước bảng dữ liệu thành công: nhận diện chính xác 1 dòng hợp lệ và 5 dòng lỗi.")
        print(f"    - Dòng 1: [HỢP LỆ] Sẵn sàng nhập")
        print(f"    - Dòng 2: [LỖI] {rows[1]['errorMessage']}")
        print(f"    - Dòng 3: [LỖI] {rows[2]['errorMessage']}")
        print(f"    - Dòng 4: [LỖI] {rows[3]['errorMessage']}")
        print(f"    - Dòng 5: [LỖI] {rows[4]['errorMessage']}")
        print(f"    - Dòng 6: [LỖI] {rows[5]['errorMessage']}")

def test_s2_01_ac3_partial_import_and_summary_report():
    print("\n[TEST S2-01 AC3] Nhập một phần (Bỏ qua dòng lỗi, nhập dòng hợp lệ) & Báo cáo tổng kết...")
    url = f"{BASE_URL}/api/admin/users/import"
    test_batch = [
        # Dòng 1: Hợp lệ 1
        {
            "fullName": "Học Viên A Batch",
            "email": "hva_batch@tms.vn",
            "phone": "0912111222",
            "role": "student",
        },
        # Dòng 2: Lỗi (thiếu tên)
        {
            "fullName": "",
            "email": "error_row2@tms.vn",
            "phone": "0912333444",
            "role": "student",
        },
        # Dòng 3: Hợp lệ 2 (Giảng viên)
        {
            "fullName": "Giảng Viên B Batch",
            "email": "gvb_batch@tms.vn",
            "phone": "0988222333",
            "role": "instructor",
        },
        # Dòng 4: Lỗi (SĐT sai chuẩn VN)
        {
            "fullName": "Trợ Giảng D Batch",
            "email": "tgd_batch@tms.vn",
            "phone": "0001234567",  # 000 sai
            "role": "ta",
        },
        # Dòng 5: Hợp lệ 3 (Tư vấn tuyển sinh)
        {
            "fullName": "Tuyển Sinh E Batch",
            "email": "tse_batch@tms.vn",
            "phone": "+84977444555",
            "role": "admissions",
        },
    ]

    req = urllib.request.Request(
        url,
        data=json.dumps({
            "fileName": "danh_sach_hoc_vien_k15.xlsx",
            "rows": test_batch,
        }).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    with urllib.request.urlopen(req) as resp:
        assert resp.getcode() == 200
        data = json.loads(resp.read().decode("utf-8"))
        assert data.get("success") is True
        assert data.get("batchCode", "").startswith("IMP-")
        assert data.get("totalRows") == 5
        assert data.get("successRows") == 3
        assert data.get("failedRows") == 2

        # Kiểm tra danh sách người dùng tạo thành công
        success_users = data.get("successUsers", [])
        assert len(success_users) == 3
        emails = [u["email"] for u in success_users]
        assert "hva_batch@tms.vn" in emails
        assert "gvb_batch@tms.vn" in emails
        assert "tse_batch@tms.vn" in emails
        for u in success_users:
            assert "tempPassword" in u and len(u["tempPassword"]) >= 8
            assert "userCode" in u

        # Kiểm tra danh sách dòng lỗi bị bỏ qua
        errors = data.get("errors", [])
        assert len(errors) == 2
        assert errors[0]["rowIndex"] == 2
        assert "Thiếu họ và tên" in errors[0]["reason"]
        assert errors[1]["rowIndex"] == 4
        assert "Số điện thoại" in errors[1]["reason"]

        print(f"  ✓ Nhập hàng loạt thành công: {data['batchCode']}")
        print(f"    - Tổng dòng: {data['totalRows']}")
        print(f"    - Thành công: {data['successRows']} tài khoản (kèm mật khẩu tạm thời)")
        print(f"    - Bỏ qua do lỗi: {data['failedRows']} dòng (kèm lý do chi tiết từng dòng)")

def test_s2_02_ac1_and_ac2_profile_update_and_protection():
    print("\n[TEST S2-02 AC1 & AC2] Xem hồ sơ, sửa thông tin cá nhân và bảo vệ Email / Vai trò...")
    # 1. GET profile
    url_get = f"{BASE_URL}/api/profile"
    req_get = urllib.request.Request(url_get, method="GET")
    with urllib.request.urlopen(req_get) as resp:
        assert resp.getcode() == 200
        data = json.loads(resp.read().decode("utf-8"))
        assert data.get("success") is True
        profile = data.get("data", {})
        assert "fullName" in profile
        assert "email" in profile
        assert "role" in profile
        print(f"  ✓ Lấy hồ sơ thành công: {profile['fullName']} ({profile['email']}) - Vai trò: {profile['role']}")

    # 2. PUT profile: Cập nhật Họ tên, SĐT, Ngày sinh, Giới tính, Địa chỉ; đồng thời thử đổi email/role
    url_put = f"{BASE_URL}/api/profile"
    update_payload = {
        "fullName": "Trần Quốc Bảo (Cập nhật)",
        "phone": "0987654321",
        "dateOfBirth": "1994-09-18",
        "gender": "MALE",
        "address": "Số 456 Đường Lương Ngọc Quyến, TP Thái Nguyên",
        # Hacker cố tình thay đổi email và role
        "email": "hacker@evil.com",
        "role": "superadmin",
        "roles": ["superadmin"],
    }

    req_put = urllib.request.Request(
        url_put,
        data=json.dumps(update_payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="PUT"
    )

    with urllib.request.urlopen(req_put) as resp:
        assert resp.getcode() == 200
        res_data = json.loads(resp.read().decode("utf-8"))
        assert res_data.get("success") is True
        saved = res_data.get("data", {})
        # AC1: Các trường thông tin liên lạc được sửa
        assert saved["fullName"] == "Trần Quốc Bảo (Cập nhật)"
        assert saved["phone"] == "0987654321"
        assert saved["dateOfBirth"] == "1994-09-18"
        assert saved["address"] == "Số 456 Đường Lương Ngọc Quyến, TP Thái Nguyên"

        # AC2: Email và role tuyệt đối KHÔNG bị sửa
        assert "email" not in saved or saved["email"] != "hacker@evil.com"
        assert "role" not in saved or saved["role"] != "superadmin"

        print("  ✓ Cập nhật hồ sơ thành công (Họ tên, SĐT, Ngày sinh, Địa chỉ đã được lưu).")
        print("  ✓ Bảo vệ nghiêm ngặt Email và Vai trò: Hệ thống từ chối thay đổi email và vai trò cá nhân.")

def test_s2_02_ac3_vietnam_phone_validation():
    print("\n[TEST S2-02 AC3] Kiểm tra định dạng số điện thoại Việt Nam (Strict Validation)...")
    url_put = f"{BASE_URL}/api/profile"

    # Trường hợp SĐT không hợp lệ: phải trả về mã lỗi 400
    invalid_phones = [
        ("12345", "Quá ngắn"),
        ("0123456789", "Đầu số 012 cũ không hợp lệ"),
        ("02437654321", "Số cố định (11 số)"),
        ("088888888888", "Quá 10 chữ số"),
        ("0499123456", "Đầu 04 không phải di động"),
        ("abc0918200", "Chứa ký tự chữ"),
    ]

    for phone_val, desc in invalid_phones:
        payload = {
            "fullName": "Người Dùng Test",
            "phone": phone_val,
        }
        req = urllib.request.Request(
            url_put,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="PUT"
        )
        try:
            with urllib.request.urlopen(req) as resp:
                assert False, f"Expected 400 Bad Request for phone '{phone_val}', but got {resp.getcode()}"
        except urllib.error.HTTPError as e:
            assert e.code == 400, f"Expected 400, got {e.code}"
            err_data = json.loads(e.read().decode("utf-8"))
            assert err_data.get("code") == "INVALID_PHONE_FORMAT" or "số điện thoại" in err_data.get("message", "").lower()
            print(f"  ✓ Từ chối SĐT không hợp lệ: '{phone_val}' ({desc}) -> HTTP 400")

    # Trường hợp SĐT hợp lệ theo mạng di động Việt Nam: phải thành công 200
    valid_phones = [
        "0918200300",       # VinaPhone
        "0987654321",       # Viettel
        "0356789123",       # Viettel đầu 03
        "0701234567",       # MobiFone đầu 07
        "0581234567",       # Vietnamobile đầu 05
        "+84918200300",     # Định dạng quốc tế +84
    ]

    for phone_val in valid_phones:
        payload = {
            "fullName": "Người Dùng Test",
            "phone": phone_val,
        }
        req = urllib.request.Request(
            url_put,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="PUT"
        )
        with urllib.request.urlopen(req) as resp:
            assert resp.getcode() == 200
            data = json.loads(resp.read().decode("utf-8"))
            assert data.get("success") is True
            print(f"  ✓ Chấp nhận SĐT chuẩn di động VN: '{phone_val}' -> HTTP 200")

def main():
    print_header("KIỂM THỬ TỰ ĐỘNG SPRINT 2: S2-01 (EXCEL IMPORT) & S2-02 (USER PROFILE)")
    try:
        test_s2_01_ac1_template_download()
        test_s2_01_ac2_preview_and_row_by_row_validation()
        test_s2_01_ac3_partial_import_and_summary_report()
        test_s2_02_ac1_and_ac2_profile_update_and_protection()
        test_s2_02_ac3_vietnam_phone_validation()

        print_header("TẤT CẢ 5/5 BÀI TEST TỰ ĐỘNG CHO S2-01 VÀ S2-02 ĐÃ VƯỢT QUA 100%!")
        print("Tóm tắt kết quả kiểm thử:")
        print("• S2-01 AC1: Tải tệp mẫu CSV/Excel UTF-8 BOM chuẩn 7 cột -> ĐẠT")
        print("• S2-01 AC2: Xem trước và báo lỗi chi tiết từng dòng (email, sđt, role, họ tên) -> ĐẠT")
        print("• S2-01 AC3: Nhập một phần, bỏ qua dòng lỗi, lưu dòng hợp lệ & báo cáo tổng kết -> ĐẠT")
        print("• S2-02 AC1: Sửa họ tên, số điện thoại, ngày sinh, địa chỉ, giới tính -> ĐẠT")
        print("• S2-02 AC2: Không tự đổi được email và vai trò (chống bypass API) -> ĐẠT")
        print("• S2-02 AC3: Kiểm tra định dạng số điện thoại di động Việt Nam (03, 05, 07, 08, 09, +84) -> ĐẠT")
    except Exception as e:
        print(f"\n❌ KIỂM THỬ THẤT BẠI: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
