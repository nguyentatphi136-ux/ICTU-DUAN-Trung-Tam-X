const express = require('express');
const { PERMISSIONS } = require('../constants/permissions');
const { getGradesByStudentId, updateGrade } = require('../data/users');
const { authenticate, checkGradePermission, requirePermission } = require('../middleware/auth');

const router = express.Router();

// Tất cả các API Điểm số đều yêu cầu đăng nhập
router.use(authenticate);

// Tra cứu điểm số (Giảng viên, Đào tạo, Admin, Kế toán, Học viên)
router.get('/:studentId', requirePermission(PERMISSIONS.GRADE_VIEW), (req, res) => {
  const { studentId } = req.params;
  const grades = getGradesByStudentId(studentId);
  return res.status(200).json({
    success: true,
    studentId,
    grades,
  });
});

// Cập nhật điểm số
// YÊU CẦU STORY IDTTX-20: Giảng viên sửa được điểm, Kế toán KHÔNG sửa được điểm
router.put('/:studentId', checkGradePermission('edit'), (req, res) => {
  const { studentId } = req.params;
  const { componentName, score } = req.body;

  if (!componentName || score === undefined) {
    return res.status(400).json({
      success: false,
      message: 'Vui lòng cung cấp tên đầu điểm (componentName) và điểm số (score)',
    });
  }

  const numScore = Number(score);
  if (isNaN(numScore) || numScore < 0 || numScore > 10) {
    return res.status(400).json({
      success: false,
      message: 'Điểm số phải là số hợp lệ từ 0 đến 10',
    });
  }

  const updated = updateGrade(studentId, componentName, numScore, req.user.id);
  return res.status(200).json({
    success: true,
    message: 'Cập nhật điểm số thành công',
    grade: updated,
  });
});

module.exports = router;
