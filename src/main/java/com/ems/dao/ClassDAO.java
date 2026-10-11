package com.ems.dao;

import com.ems.config.ConnectionProvider;
import com.ems.config.DBConnection;
import com.ems.model.ClassSubject;
import com.ems.model.Clazz;
import com.ems.model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) xử lý truy vấn và lưu trữ lớp học (classes) cùng môn học liên kết (class_subjects)
 * Đảm bảo: Lưu lớp và sao chép môn học trong cùng 1 Transaction (Atomicity)
 */
public class ClassDAO {

    private final ConnectionProvider connectionProvider;

    public ClassDAO() {
        this(DBConnection::getConnection);
    }

    public ClassDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider != null ? connectionProvider : DBConnection::getConnection;
    }

    /**
     * Kiểm tra mã lớp đã tồn tại trong CSDL hay chưa
     */
    public boolean existsByClassCode(String classCode) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return existsByClassCode(classCode, connection);
        }
    }

    public boolean existsByClassCode(String classCode, Connection connection) throws SQLException {
        if (classCode == null || classCode.isBlank()) return false;
        String sql = "SELECT 1 FROM classes WHERE class_code = ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, classCode.trim());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Mở lớp học mới và tự động sao chép danh sách môn học của chương trình đào tạo
     * Thao tác được thực thi trên Connection do Service quản lý Transaction (Commit/Rollback)
     */
    public Clazz createClassWithSubjects(Clazz clazz, List<Subject> subjects, Connection connection) throws SQLException {
        String insertClassSql = "INSERT INTO classes (class_code, class_name, program_id, start_date, max_capacity, study_mode, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        long classId;
        try (PreparedStatement statement = connection.prepareStatement(insertClassSql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, clazz.getClassCode());
            statement.setString(2, clazz.getClassName());
            statement.setInt(3, clazz.getProgramId());
            statement.setDate(4, Date.valueOf(clazz.getStartDate()));
            statement.setInt(5, clazz.getMaxCapacity());
            statement.setString(6, clazz.getStudyMode());
            statement.setString(7, clazz.getStatus());

            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Thêm bản ghi lớp học thất bại, không có hàng nào bị ảnh hưởng.");
            }

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    classId = generatedKeys.getLong(1);
                    clazz.setId(classId);
                } else {
                    throw new SQLException("Không thể lấy khóa chính (ID) của lớp học vừa tạo.");
                }
            }
        }

        // Sao chép danh sách môn học vào class_subjects theo đúng thứ tự
        if (subjects != null && !subjects.isEmpty()) {
            String insertSubjectSql = "INSERT INTO class_subjects (class_id, subject_id, order_index, primary_instructor_id, status) "
                    + "VALUES (?, ?, ?, NULL, 'NOT_STARTED')";

            List<ClassSubject> classSubjects = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(insertSubjectSql, Statement.RETURN_GENERATED_KEYS)) {
                for (Subject subject : subjects) {
                    statement.setLong(1, classId);
                    statement.setInt(2, subject.getId());
                    statement.setInt(3, subject.getOrderIndex());
                    statement.addBatch();

                    ClassSubject cs = new ClassSubject(classId, subject.getId(), subject.getOrderIndex());
                    cs.setSubjectCode(subject.getSubjectCode());
                    cs.setSubjectName(subject.getSubjectName());
                    classSubjects.add(cs);
                }
                statement.executeBatch();
            }
            clazz.setSubjects(classSubjects);
        }

        return clazz;
    }

    /**
     * Tra cứu chi tiết thông tin lớp học và toàn bộ môn học thuộc lớp
     */
    public Clazz findById(long classId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(classId, connection);
        }
    }

    public Clazz findById(long classId, Connection connection) throws SQLException {
        String classSql = "SELECT c.id, c.class_code, c.class_name, c.program_id, p.program_name, "
                + "c.start_date, c.end_date, c.max_capacity, c.default_classroom_id, c.study_mode, c.status, c.created_at, c.updated_at "
                + "FROM classes c "
                + "LEFT JOIN programs p ON p.id = c.program_id "
                + "WHERE c.id = ?";

        Clazz clazz = null;
        try (PreparedStatement statement = connection.prepareStatement(classSql)) {
            statement.setLong(1, classId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    clazz = new Clazz();
                    clazz.setId(result.getLong("id"));
                    clazz.setClassCode(result.getString("class_code"));
                    clazz.setClassName(result.getString("class_name"));
                    clazz.setProgramId(result.getInt("program_id"));
                    clazz.setProgramName(result.getString("program_name"));
                    Date startDate = result.getDate("start_date");
                    if (startDate != null) clazz.setStartDate(startDate.toString());
                    Date endDate = result.getDate("end_date");
                    if (endDate != null) clazz.setEndDate(endDate.toString());
                    clazz.setMaxCapacity(result.getInt("max_capacity"));
                    int roomId = result.getInt("default_classroom_id");
                    if (!result.wasNull()) clazz.setDefaultClassroomId(roomId);
                    clazz.setStudyMode(result.getString("study_mode"));
                    clazz.setStatus(result.getString("status"));
                    clazz.setCreatedAt(result.getTimestamp("created_at"));
                    clazz.setUpdatedAt(result.getTimestamp("updated_at"));
                }
            }
        }

        if (clazz != null) {
            clazz.setSubjects(findSubjectsByClassId(classId, connection));
        }

        return clazz;
    }

    /**
     * Lấy danh sách các môn học thuộc về một lớp
     */
    public List<ClassSubject> findSubjectsByClassId(long classId, Connection connection) throws SQLException {
        List<ClassSubject> list = new ArrayList<>();
        String sql = "SELECT cs.id, cs.class_id, cs.subject_id, s.subject_code, s.subject_name, "
                + "cs.order_index, cs.primary_instructor_id, u.full_name AS instructor_name, cs.status "
                + "FROM class_subjects cs "
                + "JOIN subjects s ON s.id = cs.subject_id "
                + "LEFT JOIN users u ON u.id = cs.primary_instructor_id "
                + "WHERE cs.class_id = ? "
                + "ORDER BY cs.order_index ASC, cs.id ASC";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, classId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ClassSubject cs = new ClassSubject();
                    cs.setId(result.getLong("id"));
                    cs.setClassId(result.getLong("class_id"));
                    cs.setSubjectId(result.getInt("subject_id"));
                    cs.setSubjectCode(result.getString("subject_code"));
                    cs.setSubjectName(result.getString("subject_name"));
                    cs.setOrderIndex(result.getInt("order_index"));
                    long instructorId = result.getLong("primary_instructor_id");
                    if (!result.wasNull()) {
                        cs.setPrimaryInstructorId(instructorId);
                        cs.setPrimaryInstructorName(result.getString("instructor_name"));
                    }
                    cs.setStatus(result.getString("status"));
                    list.add(cs);
                }
            }
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả các lớp học
     */
    public List<Clazz> listAll() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return listAll(connection);
        }
    }

    public List<Clazz> listAll(Connection connection) throws SQLException {
        List<Clazz> list = new ArrayList<>();
        String sql = "SELECT c.id, c.class_code, c.class_name, c.program_id, p.program_name, "
                + "c.start_date, c.end_date, c.max_capacity, c.study_mode, c.status, c.created_at "
                + "FROM classes c "
                + "LEFT JOIN programs p ON p.id = c.program_id "
                + "ORDER BY c.created_at DESC, c.id DESC";

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Clazz clazz = new Clazz();
                clazz.setId(result.getLong("id"));
                clazz.setClassCode(result.getString("class_code"));
                clazz.setClassName(result.getString("class_name"));
                clazz.setProgramId(result.getInt("program_id"));
                clazz.setProgramName(result.getString("program_name"));
                Date startDate = result.getDate("start_date");
                if (startDate != null) clazz.setStartDate(startDate.toString());
                Date endDate = result.getDate("end_date");
                if (endDate != null) clazz.setEndDate(endDate.toString());
                clazz.setMaxCapacity(result.getInt("max_capacity"));
                clazz.setStudyMode(result.getString("study_mode"));
                clazz.setStatus(result.getString("status"));
                clazz.setCreatedAt(result.getTimestamp("created_at"));
                list.add(clazz);
            }
        }
        return list;
    }
}
