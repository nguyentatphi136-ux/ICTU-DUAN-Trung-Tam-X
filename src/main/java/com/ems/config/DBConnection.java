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
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException ignored) {}
    }

    /**
     * Mở kết nối tới MySQL / CSDL kiểm thử
     */
    public static Connection getConnection() throws SQLException {
        String url = setting("DB_URL", DB_URL);
        String user = setting("DB_USER", DB_USER);
        String pass = setting("DB_PASSWORD", DB_PASSWORD);
        return DriverManager.getConnection(url, user, pass);
    }

    public static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) value = System.getProperty(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
