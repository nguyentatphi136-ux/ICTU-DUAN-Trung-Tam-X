package com.ems.service;

/**
 * Service gửi email thông báo và liên kết đặt lại mật khẩu (IDTTX-41)
 */
public class EmailService {

    /**
     * Gửi email chứa liên kết đặt lại mật khẩu
     * @param toEmail Email người nhận
     * @param resetLink Đường dẫn đặt lại mật khẩu kèm token
     */
    public static boolean sendPasswordResetEmail(String toEmail, String resetLink) {
        System.out.println("===============================================================");
        System.out.println("📧 [EMAIL SERVICE] Đang gửi thư tới: " + toEmail);
        System.out.println("Tiêu đề: [EduManager] Yêu cầu đặt lại mật khẩu của bạn");
        System.out.println("Nội dung:");
        System.out.println("Xin chào,");
        System.out.println("Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.");
        System.out.println("Vui lòng nhấp vào liên kết sau (chỉ có hiệu lực trong 30 phút và dùng 1 lần):");
        System.out.println("👉 " + resetLink);
        System.out.println("Nếu bạn không yêu cầu hành động này, vui lòng bỏ qua email.");
        System.out.println("===============================================================");
        
        // Trả về true (Trong môi trường production thực tế sẽ kết nối SMTP Gmail/SendGrid)
        return true;
    }
}
