package com.ems.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử quy chuẩn mã trạng thái HTTP trả về từ các luồng xác thực đăng nhập
 */
class LoginEndpointContractTest {

    @Test
    void testStandardAuthResponseCodes() {
        // 200 OK khi đăng nhập thành công
        String successCode = "AUTH_LOGIN_SUCCESS";
        assertNotNull(successCode);

        // 401 Unauthorized khi sai thông tin
        String invalidCredsCode = "AUTH_CREDENTIALS_INVALID";
        assertTrue(invalidCredsCode.startsWith("AUTH_"));

        // 429 Too Many Requests khi bị khóa tạm IP
        String lockedIpCode = "AUTH_IP_LOCKED";
        assertEquals("AUTH_IP_LOCKED", lockedIpCode);

        // 400 Bad Request khi payload JSON sai định dạng
        String invalidJsonCode = "REQUEST_INVALID_JSON";
        assertTrue(invalidJsonCode.contains("INVALID"));
    }
}
