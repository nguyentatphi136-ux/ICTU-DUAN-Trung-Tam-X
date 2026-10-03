package com.ems.service;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service gửi email thông báo và liên kết đặt lại mật khẩu (IDTTX-41)
 */
public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    /**
     * Gửi email kích hoạt tài khoản kèm mật khẩu tạm (S1-08)
     * @param toEmail Email người nhận
     * @param fullName Họ và tên người dùng
     * @param tempPassword Mật khẩu tạm thời
     * @param loginLink Đường dẫn đăng nhập
     */
    public static boolean sendActivationEmail(String toEmail, String fullName, String tempPassword, String loginLink) {
        String host = setting("SMTP_HOST", "");
        String username = setting("SMTP_USERNAME", "");
        String password = setting("SMTP_PASSWORD", "");
        String from = setting("SMTP_FROM", username);
        if (host.isBlank() || from.isBlank()) {
            LOGGER.warning("Chưa cấu hình SMTP_HOST hoặc SMTP_FROM; không gửi được email kích hoạt.");
            return false;
        }

        try {
            String port = setting("SMTP_PORT", "587");
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", Boolean.toString(!username.isBlank()));
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");

            Authenticator authenticator = username.isBlank() ? null : new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
            MimeMessage message = new MimeMessage(Session.getInstance(properties, authenticator));
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Kích hoạt tài khoản EduManager - Thông tin đăng nhập", "UTF-8");
            message.setText(
                    "Xin chào " + fullName + ",\n\n"
                            + "Tài khoản của bạn trên hệ thống Quản lý Đào tạo EduManager đã được tạo thành công.\n"
                            + "Dưới đây là thông tin đăng nhập tạm thời:\n"
                            + "• Tên đăng nhập / Email: " + toEmail + "\n"
                            + "• Mật khẩu tạm thời: " + tempPassword + "\n\n"
                            + "Vui lòng truy cập đường dẫn sau để đăng nhập và đổi mật khẩu ngay lần đầu sử dụng:\n"
                            + loginLink + "\n\n"
                            + "Trân trọng,\nBan Quản trị Hệ thống EduManager",
                    "UTF-8"
            );
            Transport.send(message);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Không gửi được email kích hoạt tài khoản: "
                    + exception.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Gửi email chứa liên kết đặt lại mật khẩu
     * @param toEmail Email người nhận
     * @param resetLink Đường dẫn đặt lại mật khẩu kèm token
     */
    public static boolean sendPasswordResetEmail(String toEmail, String resetLink) {
        String host = setting("SMTP_HOST", "");
        String username = setting("SMTP_USERNAME", "");
        String password = setting("SMTP_PASSWORD", "");
        String from = setting("SMTP_FROM", username);
        if (host.isBlank() || from.isBlank()) {
            LOGGER.warning("Chưa cấu hình SMTP_HOST hoặc SMTP_FROM; không gửi được email khôi phục.");
            return false;
        }

        try {
            String port = setting("SMTP_PORT", "587");
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", port);
            properties.put("mail.smtp.auth", Boolean.toString(!username.isBlank()));
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");

            Authenticator authenticator = username.isBlank() ? null : new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
            MimeMessage message = new MimeMessage(Session.getInstance(properties, authenticator));
            message.setFrom(new InternetAddress(from));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Đặt lại mật khẩu EduManager", "UTF-8");
            message.setText(
                    "Mở liên kết sau để đặt lại mật khẩu. Liên kết có hiệu lực trong 30 phút và chỉ dùng một lần:\n\n"
                            + resetLink
                            + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.",
                    "UTF-8"
            );
            Transport.send(message);
            return true;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Không gửi được email khôi phục mật khẩu: "
                    + exception.getClass().getSimpleName());
            return false;
        }
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
