package com.ems.config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional Interface cung cấp Connection CSDL
 * Cho phép linh hoạt chuyển đổi giữa MySQL thực tế và Test DB (H2)
 */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
