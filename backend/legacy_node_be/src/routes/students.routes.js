// Router quản lý hồ sơ học viên (Story S3-03 / IDTTX-211)
const express = require('express');
const { ROLES } = require('../constants/roles');
const { authenticate, authorize } = require('../middleware/auth');
const {
  addStudentNote,
  findStudentByCode,
  getStudents,
  nextStudentCode,
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
 * GET /api/students
 * Danh sách hồ sơ học viên kèm bộ lọc và tìm kiếm
 */
router.get(
  '/',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS, ROLES.INSTRUCTOR),
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
 * Xem chi tiết một hồ sơ học viên theo mã HV
 */
router.get(
  '/:code',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS, ROLES.INSTRUCTOR),
  (req, res) => {
    const student = findStudentByCode(req.params.code);
    if (!student) {
      return res.status(404).json({
        success: false,
        code: 'STUDENT_NOT_FOUND',
        message: 'Không tìm thấy hồ sơ học viên',
      });
    }

    res.status(200).json({
      success: true,
      data: student,
    });
  },
);

/**
 * POST /api/students/:code/notes
 * Thêm ghi chú vào hồ sơ học viên
 */
router.post(
  '/:code/notes',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const { text } = req.body || {};
    if (!text || !text.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Nội dung ghi chú không được để trống',
      });
    }

    try {
      const note = addStudentNote(req.params.code, {
        text,
        by: req.user.name,
      });

      res.status(201).json({
        success: true,
        message: 'Thêm ghi chú thành công',
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
