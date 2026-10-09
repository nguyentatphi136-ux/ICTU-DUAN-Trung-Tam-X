package com.ems.service;

import com.ems.dao.SubjectDAO;
import com.ems.dto.SubjectPageResponse;
import com.ems.dto.SubjectRequest;
import com.ems.dto.SubjectResponse;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.Subject;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ cho Quản lý danh mục Môn học
 * User Story: S2-05 (EP-02)
 */
public class SubjectService {

    private final SubjectDAO subjectDAO;

    public SubjectService() {
        this.subjectDAO = new SubjectDAO();
    }

    public SubjectService(SubjectDAO subjectDAO) {
        this.subjectDAO = subjectDAO;
    }

    /**
     * Tạo mới môn học
     * Ràng buộc: Mã môn là duy nhất
     */
    public SubjectResponse create(SubjectRequest request) throws SQLException {
        validateRequest(request, true);

        String code = request.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (subjectDAO.existsByCode(code)) {
            throw new ConflictException("SUBJECT_CODE_CONFLICT", "Mã môn học '" + code + "' đã tồn tại.");
        }

        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(request.getName().trim());
        subject.setSessions(request.getSessions());
        subject.setWeight(request.getWeight());
        if (request.getMinPassScore() != null) {
            subject.setMinPassScore(request.getMinPassScore());
        }
        subject.setOutcome(request.getOutcome() != null ? request.getOutcome().trim() : null);
        subject.setStatus(request.getStatus() != null && !request.getStatus().trim().isEmpty()
                ? request.getStatus().trim().toUpperCase(java.util.Locale.ROOT) : "ACTIVE");

        Subject created = subjectDAO.create(subject);
        return SubjectResponse.fromEntity(created);
    }

    /**
     * Lấy danh sách môn học có tìm kiếm và phân trang
     */
    public SubjectPageResponse getList(String keyword, int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        Map<String, Object> searchResult = subjectDAO.search(keyword, page, pageSize);
        @SuppressWarnings("unchecked")
        List<Subject> items = (List<Subject>) searchResult.get("items");
        long total = (long) searchResult.get("total");

        List<SubjectResponse> responseList = items.stream()
                .map(SubjectResponse::fromEntity)
                .toList();

        return new SubjectPageResponse(responseList, total, page, pageSize);
    }

    /**
     * Lấy chi tiết môn học theo ID
     */
    public SubjectResponse getById(int id) throws SQLException {
        Subject subject = subjectDAO.findById(id);
        if (subject == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id);
        }
        return SubjectResponse.fromEntity(subject);
    }

    /**
     * Cập nhật thông tin môn học
     */
    public SubjectResponse update(int id, SubjectRequest request) throws SQLException {
        Subject existing = subjectDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id);
        }

        validateRequest(request, false);

        String code = request.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (subjectDAO.existsByCodeAndNotId(code, id)) {
            throw new ConflictException("SUBJECT_CODE_CONFLICT", "Mã môn học '" + code + "' đã được sử dụng bởi môn học khác.");
        }

        existing.setCode(code);
        existing.setName(request.getName().trim());
        existing.setSessions(request.getSessions());
        existing.setWeight(request.getWeight());
        if (request.getMinPassScore() != null) {
            existing.setMinPassScore(request.getMinPassScore());
        }
        existing.setOutcome(request.getOutcome() != null ? request.getOutcome().trim() : null);
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            existing.setStatus(request.getStatus().trim().toUpperCase(java.util.Locale.ROOT));
        }

        Subject updated = subjectDAO.update(existing);
        return SubjectResponse.fromEntity(updated);
    }

    /**
     * Xoá môn học
     * Ràng buộc: Môn đã có lớp học thì KHÔNG ĐƯỢC XOÁ.
     */
    public void delete(int id) throws SQLException {
        Subject existing = subjectDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học với ID: " + id);
        }

        int openedClasses = subjectDAO.countOpenedClassesBySubjectId(id);
        if (openedClasses > 0) {
            throw new ConflictException("SUBJECT_HAS_OPENED_CLASSES",
                    "Môn đã có " + openedClasses + " lớp học nên không xoá được.");
        }

        subjectDAO.delete(id);
    }

    public void validateRequest(SubjectRequest request, boolean isCreate) {
        if (request == null) {
            throw new ValidationException("Dữ liệu yêu cầu không được để trống.");
        }

        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new ValidationException("Mã môn học không được để trống.");
        }
        String code = request.getCode().trim();
        if (code.length() > 50) {
            throw new ValidationException("Mã môn học không được vượt quá 50 ký tự.");
        }
        if (!code.matches("^[A-Za-z0-9_.-]+$")) {
            throw new ValidationException("Mã môn học chỉ được chứa chữ cái, chữ số, dấu gạch nối, gạch dưới hoặc chấm.");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Tên môn học không được để trống.");
        }
        if (request.getName().trim().length() > 150) {
            throw new ValidationException("Tên môn học không được vượt quá 150 ký tự.");
        }

        if (request.getSessions() == null || request.getSessions() <= 0) {
            throw new ValidationException("Số buổi học phải lớn hơn 0.");
        }

        if (request.getWeight() == null || request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Trọng số môn học phải lớn hơn 0.");
        }
    }
}
