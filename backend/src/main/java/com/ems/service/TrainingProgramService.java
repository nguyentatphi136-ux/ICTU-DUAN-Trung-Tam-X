package com.ems.service;

import com.ems.dao.TrainingProgramDAO;
import com.ems.dto.TrainingProgramPageResponse;
import com.ems.dto.TrainingProgramRequest;
import com.ems.dto.TrainingProgramResponse;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.TrainingProgram;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý nghiệp vụ cho Chương trình đào tạo (Training Program)
 * User Story S2-04: Quản lý danh mục chương trình đào tạo
 */
public class TrainingProgramService {

    private final TrainingProgramDAO trainingProgramDAO;
    private final com.ems.dao.SubjectDAO subjectDAO = new com.ems.dao.SubjectDAO();

    public TrainingProgramService() {
        this.trainingProgramDAO = new TrainingProgramDAO();
    }

    public TrainingProgramService(TrainingProgramDAO trainingProgramDAO) {
        this.trainingProgramDAO = trainingProgramDAO;
    }

    /**
     * Tạo mới chương trình đào tạo
     * RULE 1 & 2: Mã chương trình là duy nhất, không cho tạo code trùng
     * RULE 4: duration > 0
     * RULE 5: standardTuition >= 0
     */
    public TrainingProgramResponse create(TrainingProgramRequest request) throws SQLException {
        validateRequest(request, true);

        String code = request.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (trainingProgramDAO.existsByCode(code)) {
            throw new ConflictException("PROGRAM_CODE_CONFLICT", trainingProgramDAO.isCodeInTrash(code)
                    ? "Mã chương trình đào tạo '" + code + "' thuộc một chương trình trong thùng rác. Hãy khôi phục chương trình đó."
                    : "Mã chương trình đào tạo '" + code + "' đã tồn tại.");
        }

        TrainingProgram program = new TrainingProgram();
        program.setCode(code);
        program.setName(request.getName().trim());
        program.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        program.setDuration(request.getDuration());
        program.setStandardTuition(request.getStandardTuition());
        program.setStatus(request.getStatus() != null && !request.getStatus().trim().isEmpty()
                ? request.getStatus().trim().toUpperCase(java.util.Locale.ROOT) : "ACTIVE");

        TrainingProgram created = trainingProgramDAO.create(program);
        return TrainingProgramResponse.fromEntity(created);
    }

    /**
     * Lấy danh sách chương trình đào tạo có phân trang và tìm kiếm
     */
    public TrainingProgramPageResponse getList(String keyword, String status, int page, int pageSize) throws SQLException {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;

        Map<String, Object> searchResult = trainingProgramDAO.search(keyword, status, page, pageSize);
        @SuppressWarnings("unchecked")
        List<TrainingProgram> items = (List<TrainingProgram>) searchResult.get("items");
        long total = (long) searchResult.get("total");

        List<TrainingProgramResponse> responseList = new java.util.ArrayList<>();
        for (TrainingProgram p : items) responseList.add(withRunningClasses(p));

        return new TrainingProgramPageResponse(responseList, total, page, pageSize);
    }

    /**
     * Lấy thông tin chi tiết chương trình đào tạo theo ID
     */
    public TrainingProgramResponse getById(int id) throws SQLException {
        TrainingProgram program = trainingProgramDAO.findById(id);
        if (program == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo với ID: " + id);
        }
        return withRunningClasses(program);
    }

    private TrainingProgramResponse withRunningClasses(TrainingProgram program) throws SQLException {
        TrainingProgramResponse resp = TrainingProgramResponse.fromEntity(program);
        resp.setRunningClasses(trainingProgramDAO.countRunningClassesByProgramId(program.getId()));
        return resp;
    }

    /**
     * Cập nhật thông tin chương trình đào tạo
     * RULE 3: Không cho update thành code đã tồn tại ở chương trình khác
     */
    public TrainingProgramResponse update(int id, TrainingProgramRequest request) throws SQLException {
        TrainingProgram existing = trainingProgramDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo với ID: " + id);
        }

        validateRequest(request, false);

        String code = request.getCode().trim().toUpperCase(java.util.Locale.ROOT);
        if (trainingProgramDAO.existsByCodeAndNotId(code, id)) {
            throw new ConflictException("PROGRAM_CODE_CONFLICT", "Mã chương trình đào tạo '" + code + "' đã được sử dụng bởi chương trình khác.");
        }

        existing.setCode(code);
        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        existing.setDuration(request.getDuration());
        existing.setStandardTuition(request.getStandardTuition());
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            existing.setStatus(request.getStatus().trim().toUpperCase(java.util.Locale.ROOT));
        }

        TrainingProgram updated = trainingProgramDAO.update(existing);
        return TrainingProgramResponse.fromEntity(updated);
    }

    /**
     * Xoá chương trình đào tạo
     * RULE 6: Chương trình đang có lớp đang chạy (IN_PROGRESS, RUNNING, ACTIVE) KHÔNG ĐƯỢC XOÁ.
     */
    public void delete(int id) throws SQLException {
        delete(id, null);
    }

    /** Chuyển vào thùng rác; actorId là người xoá (null khi chạy kiểm thử). */
    public void delete(int id, Long actorId) throws SQLException {
        TrainingProgram existing = trainingProgramDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo với ID: " + id);
        }

        int runningClasses = trainingProgramDAO.countRunningClassesByProgramId(id);
        if (runningClasses > 0) {
            throw new ConflictException("PROGRAM_HAS_RUNNING_CLASSES",
                    "Không thể xoá chương trình đào tạo vì đang có lớp học đang chạy.");
        }

        trainingProgramDAO.delete(id, actorId);
    }

    public java.util.List<java.util.Map<String, Object>> listTrash() throws SQLException {
        return trainingProgramDAO.listTrashed();
    }

    public void restore(int id) throws SQLException {
        if (!trainingProgramDAO.restore(id)) {
            throw new ResourceNotFoundException("Chương trình không có trong thùng rác.");
        }
    }

    /** Xoá vĩnh viễn, không hoàn tác được. Còn lớp học gắn với chương trình thì giữ lại trong thùng rác. */
    public void purge(int id) throws SQLException {
        if (trainingProgramDAO.countTotalClassesByProgramId(id) > 0) {
            throw new ConflictException("PROGRAM_HAS_CLASSES",
                    "Không xoá vĩnh viễn được vì chương trình đã có lớp học. Hãy để trong thùng rác.");
        }
        try {
            if (!trainingProgramDAO.purge(id)) {
                throw new ResourceNotFoundException("Chương trình không có trong thùng rác.");
            }
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            throw new ConflictException("PROGRAM_HAS_RELATED_DATA",
                    "Không xoá vĩnh viễn được vì chương trình còn dữ liệu liên quan. Hãy để trong thùng rác.");
        }
    }

    /** Chương trình theo id (số) hoặc mã. */
    private TrainingProgram resolve(String idOrCode) throws SQLException {
        TrainingProgram program = idOrCode.matches("\\d+")
                ? trainingProgramDAO.findById(Integer.parseInt(idOrCode))
                : trainingProgramDAO.findByCode(idOrCode);
        if (program == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo: " + idOrCode);
        }
        return program;
    }

    /** Lộ trình môn của chương trình (S2-06). */
    public java.util.List<java.util.Map<String, Object>> listSubjects(String idOrCode) throws SQLException {
        return trainingProgramDAO.listSubjects(resolve(idOrCode).getId());
    }

    /** Lưu thứ tự môn mới sau khi kéo thả (S2-06). */
    public java.util.List<java.util.Map<String, Object>> reorderSubjects(String idOrCode, java.util.List<String> subjectCodes)
            throws SQLException {
        if (subjectCodes == null) {
            throw new ValidationException("Thiếu danh sách mã môn theo thứ tự mới (subjectCodes).");
        }
        TrainingProgram program = resolve(idOrCode);
        try {
            trainingProgramDAO.reorderSubjects(program.getId(), subjectCodes);
        } catch (IllegalArgumentException e) {
            throw new ConflictException("CURRICULUM_CHANGED", e.getMessage());
        }
        return trainingProgramDAO.listSubjects(program.getId());
    }

    /** Thêm môn vào cuối lộ trình (S2-06, nút +). */
    public java.util.List<java.util.Map<String, Object>> addSubject(String idOrCode, String subjectCode) throws SQLException {
        TrainingProgram program = resolve(idOrCode);
        if (subjectCode == null || subjectCode.isBlank()) {
            throw new ValidationException("Thiếu mã môn cần thêm (subjectCode).");
        }
        Map<String, Object> subject = subjectDAO.findByCode(subjectCode);
        if (subject == null) {
            throw new ResourceNotFoundException("Không tìm thấy môn học: " + subjectCode);
        }
        if (!trainingProgramDAO.addSubject(program.getId(), (Integer) subject.get("id"))) {
            throw new ConflictException("SUBJECT_ALREADY_IN_PROGRAM", "Môn " + subject.get("code") + " đã có trong lộ trình.");
        }
        return trainingProgramDAO.listSubjects(program.getId());
    }

    /** Gỡ môn khỏi lộ trình (S2-06, nút ×). */
    public java.util.List<java.util.Map<String, Object>> removeSubject(String idOrCode, String subjectCode) throws SQLException {
        TrainingProgram program = resolve(idOrCode);
        Map<String, Object> subject = subjectDAO.findByCode(subjectCode);
        if (subject == null || !trainingProgramDAO.removeSubject(program.getId(), (Integer) subject.get("id"))) {
            throw new ResourceNotFoundException("Môn " + subjectCode + " không có trong lộ trình.");
        }
        return trainingProgramDAO.listSubjects(program.getId());
    }

    /** Đặt môn tiên quyết cho một môn; chỉ chọn được các môn đứng trước nó trong lộ trình (S2-06). */
    public java.util.List<java.util.Map<String, Object>> setPrerequisites(String idOrCode, String subjectCode, java.util.List<String> prereqCodes)
            throws SQLException {
        if (prereqCodes == null) {
            throw new ValidationException("Thiếu danh sách mã môn tiên quyết (prereq).");
        }
        TrainingProgram program = resolve(idOrCode);
        java.util.List<java.util.Map<String, Object>> list = trainingProgramDAO.listSubjects(program.getId());
        java.util.List<String> codes = list.stream().map(m -> ((String) m.get("code")).toUpperCase(java.util.Locale.ROOT)).toList();
        int at = codes.indexOf(subjectCode.toUpperCase(java.util.Locale.ROOT));
        if (at < 0) {
            throw new ResourceNotFoundException("Môn " + subjectCode + " không có trong lộ trình.");
        }
        java.util.Set<String> before = new java.util.HashSet<>(codes.subList(0, at));
        java.util.List<String> wanted = prereqCodes.stream().map(c -> c.toUpperCase(java.util.Locale.ROOT)).distinct().toList();
        for (String code : wanted) {
            if (!before.contains(code)) {
                throw new com.ems.exception.ApiException(400, "PREREQ_NOT_BEFORE",
                        "Môn tiên quyết " + code + " phải nằm trong lộ trình và đứng trước môn " + subjectCode + ".");
            }
        }
        trainingProgramDAO.setPrerequisites(program.getId(), codes.get(at), wanted);
        return trainingProgramDAO.listSubjects(program.getId());
    }

    /**
     * Ngừng áp dụng chương trình đào tạo (deactivate)
     * RULE 7: Chuyển trạng thái sang INACTIVE, không xoá dữ liệu
     */
    public TrainingProgramResponse deactivate(int id) throws SQLException {
        TrainingProgram existing = trainingProgramDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo với ID: " + id);
        }

        trainingProgramDAO.updateStatus(id, "INACTIVE");
        existing.setStatus("INACTIVE");
        return TrainingProgramResponse.fromEntity(existing);
    }

    /**
     * Validate dữ liệu đầu vào
     */
    public void validateRequest(TrainingProgramRequest request, boolean isCreate) {
        if (request == null) {
            throw new ValidationException("Dữ liệu yêu cầu không được để trống.");
        }

        // Validate code
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw new ValidationException("Mã chương trình đào tạo không được để trống.");
        }
        String code = request.getCode().trim();
        if (code.length() > 50) {
            throw new ValidationException("Mã chương trình đào tạo không được vượt quá 50 ký tự.");
        }
        if (!code.matches("^[A-Za-z0-9_.-]+$")) {
            throw new ValidationException("Mã chương trình đào tạo chỉ được chứa chữ cái, chữ số, dấu gạch nối (-), gạch dưới (_) hoặc dấu chấm (.).");
        }

        // Validate name
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Tên chương trình đào tạo không được để trống.");
        }
        String name = request.getName().trim();
        if (name.length() > 150) {
            throw new ValidationException("Tên chương trình đào tạo không được vượt quá 150 ký tự.");
        }

        // Validate duration (> 0)
        if (request.getDuration() == null) {
            throw new ValidationException("Thời lượng chương trình đào tạo không được để trống.");
        }
        if (request.getDuration() <= 0) {
            throw new ValidationException("Thời lượng chương trình đào tạo phải lớn hơn 0.");
        }

        // Validate standardTuition (>= 0)
        if (request.getStandardTuition() == null) {
            throw new ValidationException("Học phí chuẩn không được để trống.");
        }
        if (request.getStandardTuition().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Học phí chuẩn không được là số âm.");
        }

        // Validate status
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase(java.util.Locale.ROOT);
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                throw new ValidationException("Trạng thái chỉ có thể là ACTIVE hoặc INACTIVE.");
            }
        }
    }
}
