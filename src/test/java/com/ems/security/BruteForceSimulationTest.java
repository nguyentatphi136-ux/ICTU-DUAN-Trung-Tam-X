package com.ems.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử mô phỏng đa luồng tấn công Brute-force từ nhiều dải IP khác nhau
 */
class BruteForceSimulationTest {

    @BeforeEach
    void setUp() {
        IpLoginAttemptService.resetAll();
    }

    @Test
    void testConcurrentAttacksFromMultipleIps() throws InterruptedException {
        int attackerCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(attackerCount);
        CountDownLatch latch = new CountDownLatch(attackerCount);

        for (int i = 1; i <= attackerCount; i++) {
            final String ip = "192.168.100." + i;
            executor.submit(() -> {
                try {
                    // Mỗi kẻ tấn công nhập sai 5 lần
                    for (int attempt = 0; attempt < 5; attempt++) {
                        IpLoginAttemptService.recordFailure(ip);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        assertTrue(completed, "Các luồng tấn công phải hoàn tất trong 5 giây");

        // Xác nhận toàn bộ 10 IP tấn công đều đã bị khóa 15 phút
        for (int i = 1; i <= attackerCount; i++) {
            String ip = "192.168.100." + i;
            assertTrue(IpLoginAttemptService.isIpLocked(ip), "IP " + ip + " phải bị khóa");
        }

        // Xác nhận IP người dùng bình thường hoàn toàn không bị ảnh hưởng
        String cleanUserIp = "172.16.0.1";
        assertFalse(IpLoginAttemptService.isIpLocked(cleanUserIp));
        assertEquals(5, IpLoginAttemptService.getRemainingAttempts(cleanUserIp));
    }
}
