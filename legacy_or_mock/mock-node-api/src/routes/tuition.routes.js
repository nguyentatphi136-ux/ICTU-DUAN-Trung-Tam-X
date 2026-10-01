const express = require('express');
const { PERMISSIONS } = require('../constants/permissions');
const { getTuitionByStudentId, updateTuition } = require('../data/users');
const { authenticate, checkTuitionPermission, requirePermission } = require('../middleware/auth');

const router = express.Router();

// Tất cả các API Học phí đều yêu cầu đăng nhập
router.use(authenticate);

// Tra cứu học phí (Kế toán, Admin, Học viên)
router.get('/:studentId', requirePermission(PERMISSIONS.TUITION_VIEW), (req, res) => {
  const { studentId } = req.params;
  const tuition = getTuitionByStudentId(studentId);
  if (!tuition) {
    return res.status(404).json({
      success: false,
      message: 'Không tìm thấy hồ sơ học phí của học viên này',
    });
  }
  return res.status(200).json({
    success: true,
    studentId,
    tuition,
  });
});

// Cập nhật học phí
// YÊU CẦU STORY IDTTX-20: Kế toán sửa được học phí, Giảng viên KHÔNG sửa được học phí
router.put('/:studentId', checkTuitionPermission('edit'), (req, res) => {
  const { studentId } = req.params;
  const { paidAmount, status } = req.body;

  if (paidAmount === undefined && status === undefined) {
    return res.status(400).json({
      success: false,
      message: 'Vui lòng cung cấp số tiền đã nộp (paidAmount) hoặc trạng thái (status)',
    });
  }

  if (paidAmount !== undefined && (isNaN(Number(paidAmount)) || Number(paidAmount) < 0)) {
    return res.status(400).json({
      success: false,
      message: 'Số tiền thanh toán phải là số không âm',
    });
  }

  const updated = updateTuition(studentId, paidAmount, status, req.user.id);
  return res.status(200).json({
    success: true,
    message: 'Cập nhật thông tin học phí thành công',
    tuition: updated,
  });
});

module.exports = router;
