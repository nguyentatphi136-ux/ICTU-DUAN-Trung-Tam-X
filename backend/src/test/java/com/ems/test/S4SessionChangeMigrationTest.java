package com.ems.test;

import com.ems.config.DBConnection;
import com.ems.config.S4SessionChangeMigration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class S4SessionChangeMigrationTest {
    @Test
    void createsChangeLogTableAndAllowsSessionWithoutSubject() throws Exception {
        TestDatabaseHelper.initTestDatabase();
        try (Connection conn = DBConnection.getConnection(); Statement st = conn.createStatement()) {
            // class_sessions như schema.sql gốc: class_subject_id NOT NULL
            st.execute("""
                CREATE TABLE IF NOT EXISTS class_sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    class_id BIGINT NOT NULL,
                    class_subject_id BIGINT NOT NULL,
                    subject_lesson_id INT NULL,
                    session_number INT NOT NULL,
                    session_date DATE NOT NULL,
                    start_time TIME NOT NULL,
                    end_time TIME NOT NULL,
                    classroom_id INT NOT NULL,
                    instructor_id BIGINT NOT NULL,
                    status VARCHAR(20) DEFAULT 'SCHEDULED',
                    cancel_reason VARCHAR(255) NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            S4SessionChangeMigration.run(conn);
            S4SessionChangeMigration.run(conn); // chạy lại khi khởi động lần sau không lỗi

            st.execute("DELETE FROM class_sessions WHERE id = 990001");
            assertDoesNotThrow(() -> st.execute("INSERT INTO class_sessions (id, class_id, class_subject_id, session_number, "
                    + "session_date, start_time, end_time, classroom_id, instructor_id) "
                    + "VALUES (990001, 1, NULL, 1, '2026-11-01', '09:00:00', '11:00:00', 1, 1)"));
            assertDoesNotThrow(() -> st.execute("INSERT INTO class_session_change_logs (session_id, action_type, performed_by, reason) "
                    + "VALUES (990001, 'CANCEL', 1, 'Giảng viên ốm')"));
            st.execute("DELETE FROM class_session_change_logs WHERE session_id = 990001");
            st.execute("DELETE FROM class_sessions WHERE id = 990001");
        }
    }
}
