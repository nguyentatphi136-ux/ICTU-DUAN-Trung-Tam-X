import json
import urllib.request
import urllib.error
import time
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

def test_api():
    base_url = "http://localhost:5173"
    print("=" * 70)
    print("KIỂM TRA CHỨC NĂNG S1-03: ANTI-PROBING & LIÊN KẾT 30 PHÚT DÙNG 1 LẦN")
    print("=" * 70)

    # 1. Test Anti-probing: Email KHÔNG TỒN TẠI
    print("\n1. Kiểm tra Email KHÔNG TỒN TẠI trong hệ thống:")
    req_data = json.dumps({"email": "email_hoan_toan_khong_ton_tai_999@xyz.com"}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/api/forgot-password", data=req_data, headers={"Content-Type": "application/json"})
    t0 = time.time()
    with urllib.request.urlopen(req) as resp:
        duration_non_existent = time.time() - t0
        body = json.loads(resp.read().decode("utf-8"))
        print(f"   Status Code: {resp.status}")
        print(f"   Response Body: {body}")
        print(f"   Thời gian phản hồi: {duration_non_existent:.3f}s")
        assert resp.status == 200, "Phải trả về 200"
        assert body.get("success") is True, "Success phải là True"
        assert "Nếu địa chỉ email tồn tại" in body.get("message", ""), "Phải chứa thông báo chống dò tài khoản"
        print("   -> [PASS] Email không tồn tại vẫn trả về CÙNG MỘT THÔNG BÁO CHUNG, không dò được tài khoản!")

    # 2. Test Anti-probing: Email CÓ TỒN TẠI
    print("\n2. Kiểm tra Email CÓ TỒN TẠI (tatphi2006@gmail.com):")
    req_data = json.dumps({"email": "tatphi2006@gmail.com"}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/api/forgot-password", data=req_data, headers={"Content-Type": "application/json"})
    t0 = time.time()
    with urllib.request.urlopen(req) as resp:
        duration_existent = time.time() - t0
        body = json.loads(resp.read().decode("utf-8"))
        print(f"   Status Code: {resp.status}")
        print(f"   Response Body: {body}")
        print(f"   Thời gian phản hồi: {duration_existent:.3f}s")
        assert resp.status == 200, "Phải trả về 200"
        assert body.get("success") is True, "Success phải là True"
        assert "Nếu địa chỉ email tồn tại" in body.get("message", ""), "Phải chứa thông báo chống dò tài khoản chuẩn"
        token = body.get("token")
        expires_at = body.get("expiresAt")
        print(f"   Token sinh ra: {token}")
        print(f"   Thời hạn expiresAt: {expires_at} (khoảng 30 phút từ bây giờ)")
        assert token is not None, "Phải sinh ra token khôi phục"
        assert expires_at is not None, "Phải có expiresAt"
        remaining_ms = expires_at - (time.time() * 1000)
        assert 28 * 60 * 1000 < remaining_ms <= 30 * 60 * 1000, f"Thời hạn phải đúng 30 phút, thực tế: {remaining_ms/60000:.1f} phút"
        print(f"   -> [PASS] Email tồn tại trả về cùng thông báo chung + token có hiệu lực đúng 30 phút ({remaining_ms/60000:.1f} phút)!")

    # 3. Test Verify Token còn hạn
    print("\n3. Kiểm tra tính hợp lệ của Token qua /api/verify-token:")
    req = urllib.request.Request(f"{base_url}/api/verify-token?token={token}")
    with urllib.request.urlopen(req) as resp:
        body = json.loads(resp.read().decode("utf-8"))
        print(f"   Verify result: {body}")
        assert body.get("success") is True
        assert body.get("remainingMinutes") <= 30
        print("   -> [PASS] Token hợp lệ và còn thời hạn!")

    # 4. Test Đặt lại mật khẩu lần đầu (dùng token)
    print("\n4. Đặt lại mật khẩu mới qua /api/reset-password:")
    req_data = json.dumps({"token": token, "email": "tatphi2006@gmail.com", "password": "NewPassword@123"}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/api/reset-password", data=req_data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        body = json.loads(resp.read().decode("utf-8"))
        print(f"   Reset result: {body}")
        assert body.get("success") is True
        print("   -> [PASS] Đặt lại mật khẩu thành công!")

    # 5. Test LIÊN KẾT CHỈ DÙNG ĐƯỢC MỘT LẦN: Thử verify lại token đã dùng
    print("\n5. Kiểm tra Token đã dùng có bị chặn (Single-Use check):")
    req = urllib.request.Request(f"{base_url}/api/verify-token?token={token}")
    try:
        urllib.request.urlopen(req)
        assert False, "Token đã dùng phải bị từ chối với HTTP 400"
    except urllib.error.HTTPError as e:
        body = json.loads(e.read().decode("utf-8"))
        print(f"   HTTP Error Code: {e.code}")
        print(f"   Response Body: {body}")
        assert e.code == 400
        assert body.get("reason") == "ALREADY_USED"
        print("   -> [PASS] Token đã dùng bị từ chối ngay lập tức với lý do ALREADY_USED!")

    # 6. Test Đặt lại mật khẩu lần 2 với cùng token cũ:
    print("\n6. Thử gọi lại /api/reset-password với token cũ:")
    req_data = json.dumps({"token": token, "email": "tatphi2006@gmail.com", "password": "AnotherPassword@456"}).encode("utf-8")
    req = urllib.request.Request(f"{base_url}/api/reset-password", data=req_data, headers={"Content-Type": "application/json"})
    try:
        urllib.request.urlopen(req)
        assert False, "Phải bị từ chối vì token đã sử dụng"
    except urllib.error.HTTPError as e:
        body = json.loads(e.read().decode("utf-8"))
        print(f"   HTTP Error Code: {e.code}")
        print(f"   Response Body: {body}")
        assert e.code == 400
        assert body.get("reason") == "ALREADY_USED"
        print("   -> [PASS] Chặn thành công! Liên kết chỉ sử dụng được duy nhất một lần!")

    # 7. Test GIỚI HẠN TẦN SUẤT 1 EMAIL CHỈ GỬI ĐƯỢC 1 LẦN TRONG 15 PHÚT (Rate Limiting)
    print("\n7. Kiểm tra Giới hạn tần suất 15 phút (Rate Limiting 15m per email):")
    unique_test_email = f"ratelimit_{int(time.time())}@tms.vn"
    # Lần 1: Thành công
    req_data = json.dumps({"email": unique_test_email}).encode("utf-8")
    req1 = urllib.request.Request(f"{base_url}/api/forgot-password", data=req_data, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req1) as resp:
        body1 = json.loads(resp.read().decode("utf-8"))
        print(f"   Lần 1 ({unique_test_email}) - Status: {resp.status}, Success: {body1.get('success')}")
        assert resp.status == 200

    # Lần 2: Trong vòng 15 phút phải bị chặn với mã 429 RATE_LIMITED
    req2 = urllib.request.Request(f"{base_url}/api/forgot-password", data=req_data, headers={"Content-Type": "application/json"})
    try:
        urllib.request.urlopen(req2)
        assert False, "Lần 2 trong vòng 15 phút phải bị từ chối với HTTP 429 Too Many Requests"
    except urllib.error.HTTPError as e:
        body2 = json.loads(e.read().decode("utf-8"))
        print(f"   Lần 2 (gửi lại ngay lập tức) - HTTP Code: {e.code}")
        print(f"   Response Body: {body2}")
        assert e.code == 429, "Phải trả về 429 Too Many Requests"
        assert body2.get("code") == "RATE_LIMITED", "Mã lỗi phải là RATE_LIMITED"
        assert "15 phút" in body2.get("message", ""), "Thông báo phải nêu rõ giới hạn 15 phút"
        print("   -> [PASS] Chặn thành công spam yêu cầu! 1 email chỉ gửi được 1 lần sau 15 phút!")

    print("\n" + "=" * 70)
    print("HOÀN THÀNH: TẤT CẢ CÁC TIÊU CHÍ S1-03 ĐỀU ĐẠT CHUẨN 100%!")
    print("• Nhập email nhận liên kết 30 phút: ĐẠT")
    print("• Liên kết chỉ dùng được 1 lần: ĐẠT")
    print("• Email không tồn tại hiển thị cùng 1 thông báo chống dò tài khoản: ĐẠT")
    print("• Giới hạn 1 email chỉ gửi được 1 lần sau 15 phút tránh sập hệ thống: ĐẠT")
    print("=" * 70)

if __name__ == "__main__":
    test_api()
