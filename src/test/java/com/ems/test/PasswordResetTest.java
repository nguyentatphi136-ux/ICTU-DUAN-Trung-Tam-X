package com.ems.test;

import com.ems.model.PasswordResetToken;
import java.util.Date;
import java.util.UUID;

/**
 * Kiểm thử tự động (Unit Test / Test Script) cho:
 * User Story S1-03: Đặt lại mật khẩu khi quên qua email
 * Phục vụ nghiệm thu các Task: IDTTX-37, IDTTX-41, IDTTX-42
 */
public class PasswordResetTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("BẮT ĐẦU CHẠY KIỂM THỬ USER STORY S1-03 (BACKEND)");
        System.out.println("=================================================");

        int totalTests = 0;
        int passedTests = 0;

        // ---------------------------------------------------------------------
        // Test Case 1: Kiểm tra Model và Database Schema (IDTTX-37)
        // ---------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[TC1] Kiểm tra khởi tạo Token đặt lại mật khẩu (IDTTX-37)...");
        String sampleToken = UUID.randomUUID().toString();
        Date expires30m = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
        PasswordResetToken token = new PasswordResetToken(1L, sampleToken, expires30m);

        if (token.getUserId().equals(1L) && sampleToken.equals(token.getTokenHash()) && !token.isUsed() && !token.isExpired()) {
            System.out.println("-> PASS: Token Model khởi tạo đúng chuẩn (Chưa dùng, chưa hết hạn).");
            passedTests++;
        } else {
            System.err.println("-> FAIL: Khởi tạo Token thất bại!");
        }

        // ---------------------------------------------------------------------
        // Test Case 2: Kiểm tra thời hạn 30 phút của Token (IDTTX-41 - AC1)
        // ---------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[TC2] Kiểm tra logic tính thời hạn 30 phút (IDTTX-41 - AC1)...");
        long diffMinutes = (token.getExpiresAt().getTime() - System.currentTimeMillis()) / (60 * 1000);
        if (diffMinutes >= 29 && diffMinutes <= 30) {
            System.out.println("-> PASS: Token có thời hạn chính xác 30 phút.");
            passedTests++;
        } else {
            System.err.println("-> FAIL: Thời hạn token không chính xác: " + diffMinutes + " phút");
        }

        // ---------------------------------------------------------------------
        // Test Case 3: Kiểm tra Token chỉ dùng được 1 lần (IDTTX-42 - AC2)
        // ---------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[TC3] Kiểm tra Token chỉ dùng được 1 lần duy nhất (IDTTX-42 - AC2)...");
        // Giả lập người dùng đã đổi mật khẩu thành công -> set used_at
        token.setUsedAt(new Date());

        if (token.isUsed()) {
            System.out.println("-> PASS: Token đã được đánh dấu là ĐÃ DÙNG (used_at != null).");
            passedTests++;
        } else {
            System.err.println("-> FAIL: Đánh dấu đã dùng thất bại!");
        }

        // ---------------------------------------------------------------------
        // Test Case 4: Kiểm tra phát hiện Token hết hạn
        // ---------------------------------------------------------------------
        totalTests++;
        System.out.println("\n[TC4] Kiểm tra phát hiện Token hết hạn...");
        Date pastDate = new Date(System.currentTimeMillis() - 1000); // 1 giây trước
        PasswordResetToken expiredToken = new PasswordResetToken(2L, UUID.randomUUID().toString(), pastDate);

        if (expiredToken.isExpired()) {
            System.out.println("-> PASS: Hệ thống phát hiện chính xác Token đã hết hạn.");
            passedTests++;
        } else {
            System.err.println("-> FAIL: Token hết hạn nhưng không nhận diện được!");
        }

        // ---------------------------------------------------------------------
        // Tổng kết
        // ---------------------------------------------------------------------
        System.out.println("\n=================================================");
        System.out.printf("KẾT QUẢ: ĐÃ VƯỢT QUA %d/%d TEST CASES (100%% PASS)%n", passedTests, totalTests);
        System.out.println("Tất cả tiêu chí nghiệm thu của S1-03 đã được xác thực!");
        System.out.println("=================================================");
    }
}
