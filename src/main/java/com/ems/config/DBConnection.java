package com.ems.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Mục 12: JDBC & CRUD - Lớp tiện ích kết nối Cơ sở dữ liệu MySQL
 */
public class DBConnection {
    private static final String DB_URL = setting(
            "DB_URL",
            "jdbc:mysql://localhost:3306/ems_database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
    );
    private static final String DB_USER = setting("DB_USER", "root");
    private static final String DB_PASSWORD = setting("DB_PASSWORD", "");

    static {
        try {
            // Nạp Driver MySQL JDBC
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL JDBC Driver: " + e.getMessage());
        }
    }

    /**
     * Mở kết nối tới MySQL
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
