const express = require('express');
const { ROLES } = require('../constants/roles');
const { FUNNEL_STAGES, REJECT_REASONS } = require('../constants/leads');
const { authenticate, authorize } = require('../middleware/auth');
const {
  addLeadCareLog,
  findLeadById,
  getFunnelData,
  getLeadCareLogs,
  getLeadStageLogs,
  getLeadStats,
  moveLeadStage,
} = require('../data/leads');
const { convertLeadToStudent } = require('../data/students');

const router = express.Router();

/**
 * Hàm trợ giúp kiểm tra quyền đối với Lead cụ thể:
 * - Tư vấn viên (Admissions) chỉ được thao tác trên lead được phân công cho mình
 * - Admin và Quản lý đào tạo (TrainingManager) có toàn quyền
 */
function checkLeadPermission(lead, req, res) {
  const isOnlyAdmissions =
    req.user.roles.includes(ROLES.ADMISSIONS) &&
    !req.user.roles.includes(ROLES.ADMIN) &&
    !req.user.roles.includes(ROLES.TRAINING_MANAGER);

  if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
    res.status(403).json({
      success: false,
      code: 'FORBIDDEN',
      message: 'Tư vấn viên chỉ có quyền thao tác trên lead được giao cho mình',
    });
    return false;
  }
  return true;
}

/**
 * Xử lý lấy dữ liệu Bảng phễu Kanban (Story S3-01)
 * Dùng chung cho GET / và GET /funnel
 */
function handleGetFunnel(req, res) {
  const isOnlyAdmissions =
    req.user.roles.includes(ROLES.ADMISSIONS) &&
    !req.user.roles.includes(ROLES.ADMIN) &&
    !req.user.roles.includes(ROLES.TRAINING_MANAGER);

  const { search, q, onlyLate, late, counselorId, date, referenceDate } = req.query || {};

  const funnelResult = getFunnelData({
    search: search || q || '',
    onlyLate: onlyLate !== undefined ? onlyLate : late,
    counselorId: isOnlyAdmissions ? req.user.id : counselorId,
    referenceDate: referenceDate || date || '2026-10-06',
  });

  res.status(200).json({
    success: true,
    total: funnelResult.total,
    lateCount: funnelResult.lateCount,
    counts: funnelResult.counts,
    data: funnelResult.leads,
    leads: funnelResult.leads,
    columns: funnelResult.columns,
  });
}

/**
 * GET /api/funnel hoặc /api/funnel/
 * Lấy dữ liệu 6 cột phễu Kanban, hiển thị số lượng lead mỗi cột
 */
router.get(
  '/',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  handleGetFunnel,
);

/**
 * GET /admissions/funnel
 */
router.get(
  '/funnel',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  handleGetFunnel,
);

/**
 * GET /api/funnel/stages
 * Danh sách 6 cột phễu chuẩn
 */
router.get(
  '/stages',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    res.status(200).json({
      success: true,
      data: FUNNEL_STAGES,
    });
  },
);

/**
 * GET /api/funnel/reasons
 * Danh mục các lý do từ chối chuẩn khi chuyển thẻ sang cột Từ chối
 */
router.get(
  '/reasons',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    res.status(200).json({
      success: true,
      data: REJECT_REASONS,
    });
  },
);

/**
 * GET /api/funnel/stats
 * Thống kê số lượng lead theo 6 cột phễu Kanban
 */
router.get(
  '/stats',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    const stats = getLeadStats({
      counselorId: isOnlyAdmissions ? req.user.id : req.query.counselorId,
    });

    res.status(200).json({
      success: true,
      data: stats,
    });
  },
);

/**
 * PATCH /api/funnel/leads/:id/stage hoặc /admissions/leads/:id/stage
 * Kéo thả để đổi trạng thái lead trên bảng phễu (Story S3-01)
 * Tiêu chí nghiệm thu:
 * - Chuyển sang Từ chối bắt buộc chọn lý do
 * - Mỗi lần đổi được tự động ghi nhật ký
 */
router.patch(
  '/leads/:id/stage',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const lead = findLeadById(req.params.id);
    if (!lead) {
      return res.status(404).json({
        success: false,
        code: 'LEAD_NOT_FOUND',
        message: 'Không tìm thấy Lead',
      });
    }

    if (!checkLeadPermission(lead, req, res)) return;

    const { stage, reason, note, referenceDate } = req.body || {};
    if (!stage) {
      return res.status(400).json({
        success: false,
        code: 'MISSING_STAGE',
        message: 'Vui lòng cung cấp giai đoạn phễu cần chuyển tới',
      });
    }

    try {
      const updatedLead = moveLeadStage(lead.id, {
        stage,
        reason,
        note,
        counselorId: req.user.id,
        referenceDate: referenceDate || '2026-10-06',
      });

      res.status(200).json({
        success: true,
        message: `Đã chuyển giai đoạn lead sang ${updatedLead.stage}`,
        data: updatedLead,
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

/**
 * GET /api/funnel/leads/:id/logs hoặc /admissions/leads/:id/logs
 * Lấy toàn bộ nhật ký chăm sóc và chuyển đổi trạng thái của lead (S3-01 & S3-02)
 */
router.get(
  '/leads/:id/logs',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const lead = findLeadById(req.params.id);
    if (!lead) {
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy Lead',
      });
    }

    if (!checkLeadPermission(lead, req, res)) return;

    const logs = getLeadCareLogs(lead.id);

    res.status(200).json({
      success: true,
      data: logs,
    });
  },
);

/**
 * POST /api/funnel/leads/:id/logs hoặc /admissions/leads/:id/logs
 * Thêm một dòng nhật ký chăm sóc (Gọi, Nhắn, Gặp, Đổi trạng thái)
 */
router.post(
  '/leads/:id/logs',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const lead = findLeadById(req.params.id);
    if (!lead) {
      return res.status(404).json({
        success: false,
        message: 'Không tìm thấy Lead',
      });
    }

    if (!checkLeadPermission(lead, req, res)) return;

    const { kind, text, at } = req.body || {};
    if (!text || !text.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Nội dung nhật ký không được để trống',
      });
    }

    try {
      const logEntry = addLeadCareLog(lead.id, {
        kind: kind || 'Ghi chú',
        text: text.trim(),
        at,
        counselorId: req.user.id,
      });

      res.status(201).json({
        success: true,
        message: 'Ghi nhật ký thành công',
        data: logEntry,
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

/**
 * POST /api/funnel/leads/:id/convert hoặc /admissions/leads/:id/convert
 * Chuyển một lead đã chốt thành hồ sơ học viên (Story S3-03 / IDTTX-211)
 */
router.post(
  '/leads/:id/convert',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const lead = findLeadById(req.params.id);
    if (!lead) {
      return res.status(404).json({
        success: false,
        code: 'LEAD_NOT_FOUND',
        message: 'Không tìm thấy Lead',
      });
    }

    if (!checkLeadPermission(lead, req, res)) return;

    const { sendTo } = req.body || {};

    try {
      const result = convertLeadToStudent({
        leadId: lead.id,
        sendTo: sendTo || lead.email,
        counselorId: req.user.id,
      });

      return res.status(201).json(result);
    } catch (err) {
      return res.status(err.statusCode || 400).json({
        success: false,
        code: err.code || 'BAD_REQUEST',
        message: err.message,
      });
    }
  },
);

module.exports = router;
