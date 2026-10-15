// Router quản lý hồ sơ học viên (Story S3-03 & S3-04 / IDTTX-212 / Subtask IDTTX-241)
const express = require('express');
const { ROLES } = require('../constants/roles');
const { authenticate, authorize } = require('../middleware/auth');
const {
  addStudentNote,
  findStudentByCode,
  getStudents,
  nextStudentCode,
  updateStudentProfile,
} = require('../data/students');
const { loginOf, seq } = require('../constants/students');

const router = express.Router();

/**
 * GET /api/students/next-code
 * Lấy trước mã học viên và email đăng nhập tự sinh tiếp theo (Story S3-03)
 */
router.get(
  '/next-code',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const code = nextStudentCode('26');
    const sequenceNumber = seq(code);
    const login = loginOf(sequenceNumber);

    res.status(200).json({
      success: true,
      nextCode: code,
      nextLogin: login,
      code,
      login,
    });
  },
);

/**
 * GET /api/students/me
 * Tra cứu hồ sơ học viên của chính người dùng đang đăng nhập
 */
router.get('/me', authenticate, (req, res) => {
  const email = (req.user.email || '').toLowerCase();
  const userId = String(req.user.id || '');

  const { students } = require('../data/students');
  const me = students.find(
    (s) =>
      (s.login && s.login.toLowerCase() === email) ||
      (s.email && s.email.toLowerCase() === email) ||
      (s.userId && String(s.userId) === userId),
  );

  if (!me) {
    return res.status(404).json({
      success: false,
      code: 'STUDENT_NOT_FOUND',
      message: 'Không tìm thấy hồ sơ học viên tương ứng với tài khoản này',
    });
  }

  const enriched = findStudentByCode(me.code);
  res.status(200).json({
    success: true,
    data: enriched,
  });
});

/**
 * GET /api/students
 * Danh sách hồ sơ học viên kèm bộ lọc và tìm kiếm (Story S3-04)
 * - Quản lý đào tạo, Admin, Tuyển sinh, Giảng viên: toàn quyền tra cứu
 * - Học viên: bị chặn truy cập danh sách toàn trường (403 Forbidden)
 */
router.get(
  '/',
  authenticate,
  (req, res, next) => {
    const isStaff = req.user.roles.some((r) =>
      [ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS, ROLES.INSTRUCTOR].includes(r),
    );

    if (!isStaff) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN_STUDENT_LIST',
        message: 'Học viên không có quyền xem danh sách học viên toàn trường',
      });
    }
    next();
  },
  (req, res) => {
    const { search, q, status, program, cls, page, limit } = req.query || {};

    const result = getStudents({
      search: search || q || '',
      status: status || '',
      program: program || '',
      cls: cls || '',
      page: page || 1,
      limit: limit || 20,
    });

    res.status(200).json({
      success: true,
      ...result,
    });
  },
);

/**
 * GET /api/students/:code
 * Xem chi tiết một hồ sơ học viên theo mã HV kèm lớp đang học và lịch sử học tập
 * - Học viên chỉ được xem hồ sơ của chính mình
 * - Cán bộ nhân viên (Admin, Quản lý đào tạo, Tuyển sinh, Giảng viên) được xem hồ sơ
 */
router.get(
  '/:code',
  authenticate,
  (req, res) => {
    const student = findStudentByCode(req.params.code);
    if (!student) {
      return res.status(404).json({
        success: false,
        code: 'STUDENT_NOT_FOUND',
        message: 'Không tìm thấy hồ sơ học viên',
      });
    }

    // Kiểm tra phân quyền: Học viên chỉ xem được hồ sơ của chính mình
    const isStaff = req.user.roles.some((r) =>
      [ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS, ROLES.INSTRUCTOR].includes(r),
    );

    if (!isStaff) {
      const email = (req.user.email || '').toLowerCase();
      const userId = String(req.user.id || '');
      const isOwner =
        (student.login && student.login.toLowerCase() === email) ||
        (student.email && student.email.toLowerCase() === email) ||
        (student.userId && String(student.userId) === userId);

      if (!isOwner) {
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN_STUDENT_PROFILE',
          message: 'Học viên chỉ được xem hồ sơ của chính mình',
        });
      }
    }

    res.status(200).json({
      success: true,
      data: student,
    });
  },
);

/**
 * PUT /api/students/:code hoặc PATCH /api/students/:code
 * Sửa thông tin hồ sơ học viên (Story S3-04 / Subtask IDTTX-241)
 * - Thông tin cá nhân: số điện thoại, ngày sinh, email, địa chỉ
 * - Trình độ đầu vào: điểm kiểm tra đầu vào, xếp loại, nền tảng
 * - Ghi chú mới (nếu gửi kèm)
 * - Chỉ Quản trị hệ thống, Quản lý đào tạo hoặc Tư vấn tuyển sinh mới có quyền sửa
 */
function handleUpdateStudent(req, res) {
  try {
    const updated = updateStudentProfile(req.params.code, req.body || {}, req.user);
    res.status(200).json({
      success: true,
      message: `Đã cập nhật hồ sơ học viên ${updated.code} · ${updated.name}`,
      data: updated,
    });
  } catch (err) {
    res.status(err.statusCode || 400).json({
      success: false,
      code: err.code || 'BAD_REQUEST',
      message: err.message,
    });
  }
}

router.put(
  '/:code',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  handleUpdateStudent,
);

router.patch(
  '/:code',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  handleUpdateStudent,
);

/**
 * POST /api/students/:code/notes
 * Thêm ghi chú vào hồ sơ học viên (Story S3-04)
 */
router.post(
  '/:code/notes',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const text = req.body?.text || req.body?.note;
    if (!text || !String(text).trim()) {
      return res.status(400).json({
        success: false,
        code: 'NOTE_REQUIRED',
        message: 'Nội dung ghi chú không được để trống',
      });
    }

    try {
      const note = addStudentNote(req.params.code, {
        text: String(text).trim(),
        by: req.user.name || 'Quản lý đào tạo',
      });

      res.status(201).json({
        success: true,
        message: 'Thêm ghi chú vào hồ sơ học viên thành công',
        data: note,
      });
    } catch (err) {
      res.status(err.statusCode || 400).json({
        success: false,
        code: err.code || 'BAD_REQUEST',
        message: err.message,
      });
    }
  },
);

module.exports = router;
