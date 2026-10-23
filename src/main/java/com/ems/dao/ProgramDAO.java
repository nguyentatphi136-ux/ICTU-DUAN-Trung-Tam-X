package com.ems.dao;

import com.ems.config.ConnectionProvider;
import com.ems.config.DBConnection;
import com.ems.model.Program;
import com.ems.model.Subject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) xử lý truy vấn bảng programs, subjects và program_subjects
 */
public class ProgramDAO {

    private final ConnectionProvider connectionProvider;

    public ProgramDAO() {
        this(DBConnection::getConnection);
    }

    public ProgramDAO(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider != null ? connectionProvider : DBConnection::getConnection;
    }

    public Program findById(int programId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findById(programId, connection);
        }
    }

    public Program findById(int programId, Connection connection) throws SQLException {
        String sql = "SELECT id, program_code, program_name, description, duration_months, standard_tuition, status "
                + "FROM programs WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, programId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                Program program = new Program();
                program.setId(result.getInt("id"));
                program.setProgramCode(result.getString("program_code"));
                program.setProgramName(result.getString("program_name"));
                program.setDescription(result.getString("description"));
                program.setDurationMonths(result.getInt("duration_months"));
                program.setStandardTuition(result.getBigDecimal("standard_tuition"));
                program.setStatus(result.getString("status"));
                return program;
            }
        }
    }

    public List<Subject> findSubjectsByProgramId(int programId) throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            return findSubjectsByProgramId(programId, connection);
        }
    }

    public List<Subject> findSubjectsByProgramId(int programId, Connection connection) throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT s.id, s.subject_code, s.subject_name, s.total_sessions, s.credit_weight, "
                + "s.min_pass_score, s.learning_outcomes, s.status, ps.order_index "
                + "FROM program_subjects ps "
                + "JOIN subjects s ON s.id = ps.subject_id "
                + "WHERE ps.program_id = ? AND s.status = 'ACTIVE' "
                + "ORDER BY ps.order_index ASC, s.id ASC";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, programId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Subject subject = new Subject();
                    subject.setId(result.getInt("id"));
                    subject.setSubjectCode(result.getString("subject_code"));
                    subject.setSubjectName(result.getString("subject_name"));
                    subject.setTotalSessions(result.getInt("total_sessions"));
                    subject.setCreditWeight(result.getBigDecimal("credit_weight"));
                    subject.setMinPassScore(result.getBigDecimal("min_pass_score"));
                    subject.setLearningOutcomes(result.getString("learning_outcomes"));
                    subject.setStatus(result.getString("status"));
                    subject.setOrderIndex(result.getInt("order_index"));
                    subjects.add(subject);
                }
            }
        }
        return subjects;
    }

    public List<Program> listAllActivePrograms() throws SQLException {
        try (Connection connection = connectionProvider.getConnection()) {
            List<Program> list = new ArrayList<>();
            String sql = "SELECT id, program_code, program_name, description, duration_months, standard_tuition, status "
                    + "FROM programs WHERE status = 'ACTIVE' ORDER BY program_name ASC";
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Program program = new Program();
                    program.setId(result.getInt("id"));
                    program.setProgramCode(result.getString("program_code"));
                    program.setProgramName(result.getString("program_name"));
                    program.setDescription(result.getString("description"));
                    program.setDurationMonths(result.getInt("duration_months"));
                    program.setStandardTuition(result.getBigDecimal("standard_tuition"));
                    program.setStatus(result.getString("status"));
                    list.add(program);
                }
            }
            return list;
        }
    }
}
