package com.ems.config;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Chạy migration khi ứng dụng khởi động: tạo bảng còn thiếu, thêm cột xoá mềm, sửa tên cột cũ.
 * Không kết nối được CSDL thì chỉ ghi cảnh báo, ứng dụng vẫn chạy.
 */
@WebListener
public class AppStartupListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        DatabaseMigration.runMigration();
    }
}
