package com.ems.service;

import com.ems.config.DBConnection;
import com.ems.dao.ClassDAO;
import com.ems.dao.ProgramDAO;
import com.ems.dto.ClassResponse;
import com.ems.dto.CreateClassRequest;
import com.ems.exception.BusinessRuleException;
import com.ems.exception.DuplicateResourceException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.exception.ValidationException;
import com.ems.model.ClassSubject;
import com.ems.model.Clazz;
import com.ems.model.Program;
import com.ems.model.Subject;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service xử lý nghiệp vụ Mở lớp học (User Story S3-05)
 * Đảm bảo:
 * - Mã lớp duy nhất
 * - Vòng đời trạng thái khởi tạo là Chuẩn bị (PLANNING)
 * - Tự động sao chép danh sách môn học từ chương trình đào tạo
 * - Không yêu cầu khai báo lại môn học
 * - Lưu lớp và môn học trong cùng một Transaction (Atomicity)
 */
public class ClassService {

    private static final Set<String> VALID_STUDY_MODES = Set.of(
            Clazz.MODE_OFFLINE, Clazz.MODE_ONLINE, Clazz.MODE_HYBRID
    );

    private final ClassDAO classDAO;
    private final ProgramDAO programDAO;
    private final com.ems.config.ConnectionProvider connectionProvider;

    public ClassService() {
        this(new ClassDAO(), new ProgramDAO(), DBConnection::getConnection);
    }

    public ClassService(ClassDAO classDAO, ProgramDAO programDAO) {
        this(classDAO, programDAO, DBConnection::getConnection);
    }

    public ClassService(ClassDAO classDAO, ProgramDAO programDAO, com.ems.config.ConnectionProvider connectionProvider) {
        this.classDAO = classDAO;
        this.programDAO = programDAO;
        this.connectionProvider = connectionProvider != null ? connectionProvider : DBConnection::getConnection;
    }

    /**
     * Mở lớp học mới từ chương trình đào tạo
     */
    public ClassResponse openClass(CreateClassRequest request) throws SQLException {
        // 1. Validate dữ liệu đầu vào
        validateCreateRequest(request);

        String classCode = request.getClassCode().trim();
        int programId = request.getProgramId();
        String studyMode = request.getStudyMode().trim().toUpperCase();
        int maxStudents = request.getMaxStudents();
        String expectedStartDate = request.getExpectedStartDate().trim();

        // 2. Kiểm tra chương trình đào tạo có tồn tại hay không
        Program program = programDAO.findById(programId);
        if (program == null || !"ACTIVE".equalsIgnoreCase(program.getStatus())) {
            throw new ResourceNotFoundException("Chương trình đào tạo không tồn tại hoặc đã ngừng hoạt động (ID: " + programId + ").");
        }

        // 3. Kiểm tra mã lớp đã tồn tại hay chưa ở tầng Service
        if (classDAO.existsByClassCode(classCode)) {
            throw new DuplicateResourceException("Mã lớp '" + classCode + "' đã tồn tại trong hệ thống.");
        }

        // 4. Lấy danh sách môn học thuộc chương trình đào tạo
        List<Subject> subjects = programDAO.findSubjectsByProgramId(programId);
        if (subjects == null || subjects.isEmpty()) {
            throw new BusinessRuleException("Chương trình đào tạo '" + program.getProgramName()
                    + "' chưa có môn học hợp lệ nào, không thể mở lớp.");
        }

        // 5. Chuẩn bị thông tin thực thể Clazz với trạng thái ban đầu là Chuẩn bị (PLANNING)
        Clazz clazz = new Clazz();
        clazz.setClassCode(classCode);
        String className = request.getClassName();
        if (className == null || className.isBlank()) {
            clazz.setClassName(program.getProgramName() + " - " + classCode);
        } else {
            clazz.setClassName(className.trim());
        }
        clazz.setProgramId(programId);
        clazz.setProgramName(program.getProgramName());
        clazz.setStartDate(expectedStartDate);
        clazz.setMaxCapacity(maxStudents);
        clazz.setStudyMode(studyMode);
        clazz.setStatus(Clazz.STATUS_PLANNING); // Bắt buộc là Chuẩn bị

        // 6. Thực thi Transaction nguyên tử: Tạo lớp và sao chép toàn bộ môn học
        try (Connection connection = this.connectionProvider.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Kiểm tra lại trong transaction phòng trường hợp 2 request đồng thời
                if (classDAO.existsByClassCode(classCode, connection)) {
                    throw new DuplicateResourceException("Mã lớp '" + classCode + "' đã tồn tại trong hệ thống.");
                }

                // Lưu lớp và sao chép môn học
                classDAO.createClassWithSubjects(clazz, subjects, connection);

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                // Bắt lỗi MySQL Duplicate Entry (Error Code 1062)
                if (ex.getErrorCode() == 1062 || (ex.getSQLState() != null && ex.getSQLState().startsWith("23"))) {
                    throw new DuplicateResourceException("Mã lớp '" + classCode + "' đã tồn tại trong hệ thống.");
                }
                throw ex;
            } catch (RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {}
            }
        }

        // 7. Chuyển đổi sang Response DTO
        return mapToResponse(clazz, program.getProgramName());
    }

    /**
     * Tra cứu thông tin lớp học theo ID
     */
    public ClassResponse getById(long classId) throws SQLException {
        Clazz clazz = classDAO.findById(classId);
        if (clazz == null) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học có ID: " + classId);
        }
        return mapToResponse(clazz, clazz.getProgramName());
    }

    /**
     * Lấy danh sách tất cả lớp học
     */
    public List<ClassResponse> listClasses() throws SQLException {
        List<Clazz> classes = classDAO.listAll();
        List<ClassResponse> responses = new ArrayList<>();
        for (Clazz clazz : classes) {
            responses.add(mapToResponse(clazz, clazz.getProgramName()));
        }
        return responses;
    }

    private void validateCreateRequest(CreateClassRequest request) {
        if (request == null) {
            throw new ValidationException("Dữ liệu yêu cầu mở lớp học không được để trống.");
        }

        // Validate ClassCode
        String classCode = request.getClassCode();
        if (classCode == null || classCode.isBlank()) {
            throw new ValidationException("Mã lớp học không được để trống.");
        }
        classCode = classCode.trim();
        if (classCode.length() > 50) {
            throw new ValidationException("Mã lớp học không được vượt quá 50 ký tự.");
        }
        if (!classCode.matches("^[A-Za-z0-9_-]+$")) {
            throw new ValidationException("Mã lớp học chỉ được chứa các ký tự chữ cái, chữ số, dấu gạch nối (-) và gạch dưới (_).");
        }

        // Validate ProgramId
        Integer programId = request.getProgramId();
        if (programId == null || programId <= 0) {
            throw new ValidationException("ID chương trình đào tạo không hợp lệ.");
        }

        // Validate ExpectedStartDate
        String expectedStartDate = request.getExpectedStartDate();
        if (expectedStartDate == null || expectedStartDate.isBlank()) {
            throw new ValidationException("Ngày khai giảng dự kiến không được để trống.");
        }
        try {
            LocalDate.parse(expectedStartDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Ngày khai giảng dự kiến không hợp lệ. Định dạng yêu cầu là YYYY-MM-DD.");
        }

        // Validate MaxStudents
        Integer maxStudents = request.getMaxStudents();
        if (maxStudents == null || maxStudents <= 0) {
            throw new ValidationException("Sĩ số tối đa phải là số nguyên dương lớn hơn 0.");
        }
        if (maxStudents > 500) {
            throw new ValidationException("Sĩ số tối đa không được vượt quá 500 học viên.");
        }

        // Validate StudyMode
        String studyMode = request.getStudyMode();
        if (studyMode == null || studyMode.isBlank()) {
            throw new ValidationException("Hình thức học không được để trống.");
        }
        if (!VALID_STUDY_MODES.contains(studyMode.trim().toUpperCase())) {
            throw new ValidationException("Hình thức học không hợp lệ. Các giá trị được chấp nhận: OFFLINE, ONLINE, HYBRID.");
        }
    }

    private ClassResponse mapToResponse(Clazz clazz, String programName) {
        ClassResponse response = new ClassResponse();
        response.setId(clazz.getId());
        response.setClassCode(clazz.getClassCode());
        response.setClassName(clazz.getClassName());
        response.setProgramId(clazz.getProgramId());
        response.setProgramName(programName);
        response.setExpectedStartDate(clazz.getStartDate());
        response.setMaxStudents(clazz.getMaxCapacity());
        response.setStudyMode(clazz.getStudyMode());
        response.setStatus(clazz.getStatus());
        response.setStatusLabel(Clazz.getStatusLabel(clazz.getStatus()));

        List<ClassResponse.SubjectItem> subjectItems = new ArrayList<>();
        if (clazz.getSubjects() != null) {
            for (ClassSubject cs : clazz.getSubjects()) {
                subjectItems.add(new ClassResponse.SubjectItem(
                        cs.getId(),
                        cs.getSubjectId(),
                        cs.getSubjectCode(),
                        cs.getSubjectName(),
                        cs.getOrderIndex(),
                        cs.getStatus()
                ));
            }
        }
        response.setSubjects(subjectItems);
        response.setSubjectCount(subjectItems.size());
        if (clazz.getCreatedAt() != null) {
            response.setCreatedAt(clazz.getCreatedAt().toString());
        }
        return response;
    }
}
