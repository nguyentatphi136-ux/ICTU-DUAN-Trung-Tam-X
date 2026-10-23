# Bảng Đặc Tả Mã Lỗi API (API Error Code Specifications)

## 1. Định Dạng Chuẩn Phản Hồi Lỗi
Tất cả các endpoint API trong hệ thống tuân theo chuẩn định dạng phản hồi thống nhất:

```json
{
  "success": false,
  "error": {
    "code": "MA_LOI",
    "message": "Thông báo chi tiết bằng tiếng Việt.",
    "action": {
      "label": "Nhãn nút điều hướng",
      "href": "/duong-dan"
    }
  }
}
```

## 2. Danh Mục Mã Lỗi Xác Thực (Authentication Errors)
| Mã Lỗi (Code) | HTTP Status | Ý Nghĩa / Tình Huống | Hướng Xử Lý |
| :--- | :--- | :--- | :--- |
| `AUTH_CREDENTIALS_INVALID` | 401 Unauthorized | Email hoặc mật khẩu không chính xác | Yêu cầu nhập lại thông tin |
| `AUTH_IP_LOCKED` | 429 Too Many Requests | IP máy bị tạm khóa 15 phút do nhập sai 5 lần | Chờ hết 15 phút hoặc thử từ IP khác |
| `AUTH_RATE_LIMITED` | 429 Too Many Requests | Gửi quá 30 request/15 phút tới endpoint auth | Chờ theo tiêu đề `Retry-After` |
| `AUTH_UNAUTHORIZED` | 401 Unauthorized | Chưa đăng nhập hoặc phiên đã hết hạn | Chuyển hướng về trang đăng nhập |
| `AUTH_FORBIDDEN` | 403 Forbidden | Tài khoản không có vai trò truy cập tài nguyên | Hiển thị màn hình 403 từ chối |
| `REQUEST_INVALID_JSON` | 400 Bad Request | Cấu trúc JSON gửi lên bị lỗi cú pháp | Kiểm tra lại body request |
