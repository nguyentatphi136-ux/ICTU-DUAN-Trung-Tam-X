package com.ems.dao;

import com.ems.config.DBConnection;
import com.ems.model.SchoolClass;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SchoolClassDAO {

    public SchoolClass findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, class_code, class_name, program_id, start_date, end_date, "
                + "max_capacity, default_classroom_id, study_mode, status "
                + "FROM classes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    SchoolClass sc = new SchoolClass();
                    sc.setId(rs.getLong("id"));
                    sc.setClassCode(rs.getString("class_code"));
                    sc.setClassName(rs.getString("class_name"));
                    sc.setProgramId(rs.getInt("program_id"));
                    Date sDate = rs.getDate("start_date");
                    if (sDate != null) sc.setStartDate(sDate.toLocalDate());
                    Date eDate = rs.getDate("end_date");
                    if (eDate != null) sc.setEndDate(eDate.toLocalDate());
                    sc.setMaxCapacity(rs.getInt("max_capacity"));
                    sc.setDefaultClassroomId(rs.getInt("default_classroom_id"));
                    sc.setStudyMode(rs.getString("study_mode"));
                    sc.setStatus(rs.getString("status"));
                    return sc;
                }
            }
        }
        return null;
    }

    public SchoolClass findById(long id) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            return findById(conn, id);
        }
    }

    public boolean exists(Connection connection, long id) throws SQLException {
        String sql = "SELECT 1 FROM classes WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<SchoolClass> listAll(Connection connection) throws SQLException {
        List<SchoolClass> list = new ArrayList<>();
        String sql = "SELECT id, class_code, class_name, program_id, status FROM classes ORDER BY id DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                SchoolClass sc = new SchoolClass();
                sc.setId(rs.getLong("id"));
                sc.setClassCode(rs.getString("class_code"));
                sc.setClassName(rs.getString("class_name"));
                sc.setProgramId(rs.getInt("program_id"));
                sc.setStatus(rs.getString("status"));
                list.add(sc);
            }
        }
        return list;
    }
}
