package com.ems.service;

import com.ems.dao.SubjectDAO;
import com.ems.dao.SubjectLessonDAO;
import com.ems.dto.CloneLessonsRequest;
import com.ems.dto.SubjectLessonRequest;
import com.ems.dto.SubjectLessonResponse;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.Subject;
import com.ems.model.SubjectLesson;

import java.sql.SQLException;
import java.util.List;

/**
 * Service xử lý nghiệp vụ Khai báo các buổi học trong một môn và Nhân bản buổi học
 * User Story: S2-07 (EP-02)
 */
public class SubjectLessonService {

    private final SubjectLessonDAO subjectLessonDAO;
    private final SubjectDAO subjectDAO;

    public SubjectLessonService() {
        this.subjectLessonDAO = new SubjectLessonDAO();
        this.subjectDAO = new SubjectDAO();
    }

    public SubjectLessonService(SubjectLessonDAO subjectLessonDAO, SubjectDAO subjectDAO) {
        this.subjectLessonDAO = subjectLessonDAO;
        this.subjectDAO = subjectDAO;
    }

    /**
     * Lấy danh sách buổi học của một môn
     */
    public List<SubjectLessonResponse> getLessons(int subjectId) throws SQLException {
        List<SubjectLesson> list = subjectLessonDAO.findBySubjectId(subjectId);
        return list.stream()
                .map(SubjectLessonResponse::fromEntity)
                .toList();
    }

    /**
     * Thêm một buổi học mới
     * Ràng buộc: Số buổi khai báo KHÔNG ĐƯỢC VƯỢT QUÁ số buổi của môn học
     */
    public SubjectLessonResponse addLesson(int subjectId, SubjectLessonRequest request) throws SQLException {
        Subject subject = subjectDAO.findById(subjectId);
        if (subject == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học với ID: " + subjectId);
        }

        int currentCount = subjectLessonDAO.countBySubjectId(subjectId);
        if (currentCount >= subject.getSessions()) {
            throw new ConflictException("MAX_SESSIONS_EXCEEDED",
                    "Số buổi khai báo (" + (currentCount + 1) + ") không được vượt quá số buổi của môn học (" + subject.getSessions() + " buổi).");
        }

        if (request == null || request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            throw new ValidationException("Chủ đề buổi học không được để trống.");
        }

        int sessionOrder = (request.getSessionOrder() != null && request.getSessionOrder() > 0)
                ? request.getSessionOrder() : currentCount + 1;

        SubjectLesson lesson = new SubjectLesson();
        lesson.setSubjectId(subjectId);
        lesson.setSessionOrder(sessionOrder);
        lesson.setTopic(request.getTopic().trim());
        lesson.setObjectives(request.getObjectives() != null ? request.getObjectives().trim() : null);

        SubjectLesson created = subjectLessonDAO.create(lesson);
        return SubjectLessonResponse.fromEntity(created);
    }

    /**
     * Cập nhật thông tin buổi học
     */
    public SubjectLessonResponse updateLesson(int id, SubjectLessonRequest request) throws SQLException {
        SubjectLesson existing = subjectLessonDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + id);
        }

        if (request == null || request.getTopic() == null || request.getTopic().trim().isEmpty()) {
            throw new ValidationException("Chủ đề buổi học không được để trống.");
        }

        if (request.getSessionOrder() != null && request.getSessionOrder() > 0) {
            existing.setSessionOrder(request.getSessionOrder());
        }
        existing.setTopic(request.getTopic().trim());
        existing.setObjectives(request.getObjectives() != null ? request.getObjectives().trim() : null);

        SubjectLesson updated = subjectLessonDAO.update(existing);
        return SubjectLessonResponse.fromEntity(updated);
    }

    /**
     * Xoá buổi học
     */
    public void deleteLesson(int id) throws SQLException {
        SubjectLesson existing = subjectLessonDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy buổi học với ID: " + id);
        }
        subjectLessonDAO.delete(id);
    }

    /**
     * Nhân bản danh sách buổi từ môn khác
     * Ràng buộc: Số buổi chép sang không vượt quá số buổi của môn học đích
     */
    public List<SubjectLessonResponse> cloneLessons(int targetSubjectId, CloneLessonsRequest request) throws SQLException {
        Subject targetSubject = subjectDAO.findById(targetSubjectId);
        if (targetSubject == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học đích với ID: " + targetSubjectId);
        }

        Subject sourceSubject = null;
        if (request != null && request.getSourceSubjectId() != null) {
            sourceSubject = subjectDAO.findById(request.getSourceSubjectId());
        } else if (request != null && request.getSourceSubjectCode() != null) {
            sourceSubject = subjectDAO.findByCode(request.getSourceSubjectCode());
        }

        if (sourceSubject == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học nguồn để nhân bản.");
        }

        List<SubjectLesson> cloned = subjectLessonDAO.cloneLessons(
                sourceSubject.getId(), targetSubjectId, targetSubject.getSessions());

        return cloned.stream()
                .map(SubjectLessonResponse::fromEntity)
                .toList();
    }
}
