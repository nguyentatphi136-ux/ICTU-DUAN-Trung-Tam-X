package com.ems.service;

import com.ems.dao.ProgramSubjectDAO;
import com.ems.dto.ProgramSubjectRequest;
import com.ems.dto.ProgramSubjectResponse;
import com.ems.dto.ReorderSubjectsRequest;
import com.ems.exception.ConflictException;
import com.ems.exception.ValidationException;
import com.ems.model.ProgramSubject;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xử lý nghiệp vụ Gắn môn học vào chương trình và sắp xếp thứ tự học
 * User Story: S2-06 (EP-02)
 */
public class ProgramSubjectService {

    private final ProgramSubjectDAO programSubjectDAO;

    public ProgramSubjectService() {
        this.programSubjectDAO = new ProgramSubjectDAO();
    }

    public ProgramSubjectService(ProgramSubjectDAO programSubjectDAO) {
        this.programSubjectDAO = programSubjectDAO;
    }

    /**
     * Lấy danh sách lộ trình môn học của một chương trình đào tạo
     */
    public List<ProgramSubjectResponse> getCurriculum(int programId) throws SQLException {
        List<ProgramSubject> list = programSubjectDAO.findByProgramId(programId);
        return list.stream()
                .map(ProgramSubjectResponse::fromEntity)
                .toList();
    }

    /**
     * Gắn thêm môn học vào chương trình đào tạo
     */
    public List<ProgramSubjectResponse> addSubject(int programId, ProgramSubjectRequest request) throws SQLException {
        if (request == null || request.getSubjectId() == null) {
            throw new ValidationException("Vui lòng chọn môn học cần gắn vào chương trình.");
        }

        int subjectId = request.getSubjectId();
        if (programSubjectDAO.existsInProgram(programId, subjectId)) {
            throw new ConflictException("SUBJECT_ALREADY_IN_PROGRAM", "Môn học này đã có trong chương trình đào tạo.");
        }

        programSubjectDAO.addSubject(programId, subjectId, request.getOrderIndex(), request.getPrerequisiteSubjectId());
        return getCurriculum(programId);
    }

    /**
     * Gỡ môn học khỏi chương trình đào tạo
     */
    public List<ProgramSubjectResponse> removeSubject(int programId, int subjectId) throws SQLException {
        programSubjectDAO.removeSubject(programId, subjectId);
        return getCurriculum(programId);
    }

    /**
     * Sắp xếp lại thứ tự học các môn (kéo thả) và lưu lại
     */
    public List<ProgramSubjectResponse> reorder(int programId, ReorderSubjectsRequest request) throws SQLException {
        if (request == null || request.getSubjectIds() == null || request.getSubjectIds().isEmpty()) {
            throw new ValidationException("Danh sách thứ tự môn học không được để trống.");
        }

        programSubjectDAO.updateOrder(programId, request.getSubjectIds());
        return getCurriculum(programId);
    }

    /**
     * Cập nhật môn học tiên quyết
     */
    public List<ProgramSubjectResponse> updatePrerequisite(int programId, int subjectId, Integer prereqSubjectId) throws SQLException {
        if (prereqSubjectId != null && prereqSubjectId.equals(subjectId)) {
            throw new ValidationException("Môn học không thể chọn chính nó làm môn tiên quyết.");
        }
        programSubjectDAO.updatePrerequisite(programId, subjectId, prereqSubjectId);
        return getCurriculum(programId);
    }
}
