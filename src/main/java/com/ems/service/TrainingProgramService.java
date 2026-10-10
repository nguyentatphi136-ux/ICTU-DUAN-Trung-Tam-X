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
            throw new ConflictException("PROGRAM_CODE_CONFLICT", "Mã chương trình đào tạo '" + code + "' đã tồn tại.");
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

        List<TrainingProgramResponse> responseList = items.stream()
                .map(TrainingProgramResponse::fromEntity)
                .toList();

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
        return TrainingProgramResponse.fromEntity(program);
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
        TrainingProgram existing = trainingProgramDAO.findById(id);
        if (existing == null) {
            throw new ResourceNotFoundException("Không tìm thấy chương trình đào tạo với ID: " + id);
        }

        int runningClasses = trainingProgramDAO.countRunningClassesByProgramId(id);
        if (runningClasses > 0) {
            throw new ConflictException("PROGRAM_HAS_RUNNING_CLASSES",
                    "Không thể xoá chương trình đào tạo vì đang có lớp học đang chạy.");
        }

        trainingProgramDAO.delete(id);
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
