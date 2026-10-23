// Router quản lý lớp học (Story S3-04 & S3-05)
const express = require('express');
const { ROLES } = require('../constants/roles');
const { authenticate } = require('../middleware/auth');
const {
  classes,
  classSize,
  findClass,
  totalSessions,
} = require('../data/classes');
const { students } = require('../data/students');

const router = express.Router();

/**
 * GET /api/classes
 * Danh sách lớp học kèm thông tin sĩ số và tiến độ buổi học
 */
router.get('/', authenticate, (req, res) => {
  const { status, program, search } = req.query || {};
  let list = classes;

  if (status && status.trim()) {
    list = list.filter((c) => c.status.toLowerCase() === status.trim().toLowerCase());
  }

  if (program && program.trim()) {
    list = list.filter((c) => c.program.toLowerCase().includes(program.trim().toLowerCase()));
  }

  if (search && search.trim()) {
    const q = search.trim().toLowerCase();
    list = list.filter(
      (c) => c.code.toLowerCase().includes(q) || c.program.toLowerCase().includes(q),
    );
  }

  const enriched = list.map((c) => ({
    ...c,
    totalSessions: totalSessions(c),
    classSize: classSize(c.code, students),
  }));

  res.status(200).json({
    success: true,
    total: enriched.length,
    data: enriched,
  });
});

/**
 * GET /api/classes/:code
 * Xem chi tiết thông tin một lớp học
 */
router.get('/:code', authenticate, (req, res) => {
  const k = findClass(req.params.code);
  if (!k) {
    return res.status(404).json({
      success: false,
      code: 'CLASS_NOT_FOUND',
      message: 'Không tìm thấy lớp học',
    });
  }

  res.status(200).json({
    success: true,
    data: {
      ...k,
      totalSessions: totalSessions(k),
      classSize: classSize(k.code, students),
    },
  });
});

module.exports = router;
