package com.ems.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Mục 12: JDBC & CRUD - Lớp tiện ích kết nối Cơ sở dữ liệu MySQL
 */
public class DBConnection {
    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/ems_database?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String DEFAULT_DB_USER = "root";
    private static final String DEFAULT_DB_PASSWORD = "";

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
        return DriverManager.getConnection(dbUrl(), dbUser(), dbPassword());
    }

    private static String dbUrl() {
        return setting("DB_URL", DEFAULT_DB_URL);
    }

    private static String dbUser() {
        return setting("DB_USER", DEFAULT_DB_USER);
    }

    private static String dbPassword() {
        return setting("DB_PASSWORD", DEFAULT_DB_PASSWORD);
    }

    public static String setting(String name, String fallback) {
        String value = System.getenv(name);
        if (isBlank(value)) {
            value = System.getProperty(name);
        }
        if (isBlank(value)) {
            value = loadPropertyFromFile(name);
        }
        return isBlank(value) ? fallback : value;
    }

    private static String loadPropertyFromFile(String name) {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.load(input);
            return properties.getProperty(name);
        } catch (IOException e) {
            System.err.println("Không đọc được file database.properties: " + e.getMessage());
            return null;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
