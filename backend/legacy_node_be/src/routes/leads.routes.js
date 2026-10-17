const express = require('express');
const { ROLES } = require('../constants/roles');
const { isValidLeadStatus, isValidLeadSource } = require('../constants/leads');
const { authenticate, authorize } = require('../middleware/auth');
const { findUserById, findUsersByRole } = require('../data/users');
const {
  addLeadCareLog,
  addLeadInteraction,
  assignLeads,
  createLead,
  enrichLeadInfo,
  filterLeads,
  findLeadById,
  getFunnelData,
  getLeadAssignments,
  getLeadCareLogs,
  getLeadStats,
  moveLeadStage,
  updateLeadStatus,
} = require('../data/leads');
const { convertLeadToStudent } = require('../data/students');

const router = express.Router();


/**
 * GET /api/leads/stats
 * Thống kê phễu tuyển sinh
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
 * GET /api/leads/counselors
 * Danh sách tư vấn viên có sẵn để Quản lý đào tạo phân công (S2-10)
 */
router.get(
  '/counselors',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER),
  (req, res) => {
    const counselors = findUsersByRole(ROLES.ADMISSIONS).map((u) => ({
      id: u.id,
      name: u.name,
      email: u.email,
      phone: u.phone,
    }));

    res.status(200).json({
      success: true,
      data: counselors,
    });
  },
);

/**
 * GET /api/leads
 * Tìm kiếm và lọc lead theo nhiều điều kiện (S2-11)
 * Phân quyền: Tư vấn viên chỉ xem được lead được giao cho mình (S2-10)
 */
router.get(
  '/',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const {
      search,
      q,
      status,
      source,
      counselorId,
      assignedCounselorId,
      createdFrom,
      createdTo,
      datePreset,
      page,
      limit,
      sortBy,
      sortOrder,
    } = req.query;

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    // Nghiệp vụ S2-10: "Tư vấn viên chỉ nhìn thấy lead được giao cho mình"
    const effectiveCounselorId = isOnlyAdmissions
      ? req.user.id
      : counselorId || assignedCounselorId || null;

    const result = filterLeads({
      search: search || q || '',
      status: status || null,
      source: source || null,
      assignedCounselorId: effectiveCounselorId,
      createdFrom: createdFrom || null,
      createdTo: createdTo || null,
      datePreset: datePreset || null,
      page: page || 1,
      limit: limit || 20,
      sortBy: sortBy || 'createdAt',
      sortOrder: sortOrder || 'desc',
    });

    res.status(200).json({
      success: true,
      data: result.data,
      pagination: result.pagination,
    });
  },
);

/**
 * POST /api/leads/assign
 * Phân công một hoặc nhiều lead cùng lúc cho một tư vấn viên (S2-10)
 * Chỉ Quản lý đào tạo (TrainingManager) và Quản trị viên (Admin) được phép thực hiện
 */
router.post(
  '/assign',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER),
  (req, res) => {
    const { leadIds, counselorId, note } = req.body || {};

    if (!leadIds || !Array.isArray(leadIds) || leadIds.length === 0) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_LEAD_IDS',
        message: 'Vui lòng chọn ít nhất một lead để phân công',
      });
    }

    if (!counselorId) {
      return res.status(400).json({
        success: false,
        code: 'MISSING_COUNSELOR_ID',
        message: 'Vui lòng chọn tư vấn viên để phân công',
      });
    }

    const counselor = findUserById(counselorId);
    if (!counselor) {
      return res.status(404).json({
        success: false,
        code: 'COUNSELOR_NOT_FOUND',
        message: 'Không tìm thấy tư vấn viên được chỉ định',
      });
    }

    if (counselor.status !== 'active') {
      return res.status(400).json({
        success: false,
        code: 'COUNSELOR_INACTIVE',
        message: 'Tài khoản tư vấn viên này đang bị khoá hoặc ngưng hoạt động',
      });
    }

    if (!counselor.roles.includes(ROLES.ADMISSIONS) && !counselor.roles.includes(ROLES.ADMIN)) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_ROLE_FOR_ASSIGNMENT',
        message: 'Người được phân công phải có vai trò Tư vấn tuyển sinh (Admissions)',
      });
    }

    try {
      const assignmentResult = assignLeads({
        leadIds,
        counselorId,
        assignedBy: req.user.id,
        note,
      });

      return res.status(200).json({
        success: true,
        message: `Đã phân công thành công ${assignmentResult.updatedCount} lead cho ${counselor.name}`,
        data: assignmentResult,
      });
    } catch (err) {
      return res.status(500).json({
        success: false,
        message: err.message || 'Lỗi khi phân công lead',
      });
    }
  },
);

/**
 * POST /api/leads
 * Tạo mới lead
 */
router.post(
  '/',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
    const { fullName, phone, email, course, source, notes, status, assignedCounselorId } =
      req.body || {};

    if (!fullName || typeof fullName !== 'string' || !fullName.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Họ và tên không được để trống',
      });
    }

    if (!phone || typeof phone !== 'string' || !phone.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Số điện thoại không được để trống',
      });
    }

    const newLead = createLead({
      fullName,
      phone,
      email,
      course,
      source,
      notes,
      status,
      assignedCounselorId,
    });

    res.status(201).json({
      success: true,
      message: 'Tạo thông tin lead thành công',
      data: enrichLeadInfo(newLead),
    });
  },
);

/**
 * GET /api/leads/:id
 * Xem thông tin chi tiết một lead (bao gồm timeline)
 */
router.get(
  '/:id',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    // Nghiệp vụ S2-10: Tư vấn viên chỉ được xem lead của mình
    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền xem lead được giao cho mình',
      });
    }

    const assignments = getLeadAssignments(lead.id);

    res.status(200).json({
      success: true,
      data: enrichLeadInfo(lead),
      assignments,
    });
  },
);

/**
 * GET /api/leads/:id/assignments
 * Lịch sử chuyển giao của một lead (S2-10)
 */
router.get(
  '/:id/assignments',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền xem lead được giao cho mình',
      });
    }

    const assignments = getLeadAssignments(lead.id);

    res.status(200).json({
      success: true,
      data: assignments,
    });
  },
);

/**
 * POST /api/leads/:id/interactions
 * Thêm ghi chú/cuộc gọi vào timeline của lead (S2-11)
 */
router.post(
  '/:id/interactions',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền ghi chú cho lead được giao cho mình',
      });
    }

    const { type, title, content, durationSeconds } = req.body || {};
    if (!content || !content.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Nội dung trao đổi không được để trống',
      });
    }

    const interaction = addLeadInteraction(lead.id, {
      counselorId: req.user.id,
      type: type || 'NOTE',
      title: title || 'Ghi chú trao đổi',
      content: content.trim(),
      durationSeconds: durationSeconds ? Number(durationSeconds) : null,
    });

    res.status(201).json({
      success: true,
      message: 'Đã lưu tương tác mới vào lịch sử khách hàng',
      data: interaction,
    });
  },
);

/**
 * PATCH /api/leads/:id/status
 * Cập nhật trạng thái lead
 */
router.patch(
  '/:id/status',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền cập nhật trạng thái lead được giao cho mình',
      });
    }

    const { status, note } = req.body || {};
    if (!status) {
      return res.status(400).json({
        success: false,
        message: 'Vui lòng cung cấp trạng thái mới',
      });
    }

    try {
      const updated = updateLeadStatus(lead.id, {
        status,
        note: note || '',
        counselorId: req.user.id,
      });

      res.status(200).json({
        success: true,
        message: 'Cập nhật trạng thái thành công',
        data: updated,
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
 * GET /api/leads/funnel
 * Lấy dữ liệu Bảng phễu Kanban 6 cột (S3-01)
 */
router.get(
  '/funnel',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
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
  },
);

/**
 * PATCH /api/leads/:id/stage
 * Kéo thả để đổi trạng thái lead trên Bảng phễu (Story S3-01)
 * Tiêu chí nghiệm thu:
 * - Kéo thả để đổi trạng thái, mỗi lần đổi được ghi nhật ký (Audit Log)
 * - Chuyển sang Từ chối bắt buộc chọn lý do; nếu "Lý do khác" bắt buộc có ghi chú
 */
router.patch(
  '/:id/stage',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền chuyển trạng thái lead được giao cho mình',
      });
    }

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
 * GET /api/leads/:id/logs
 * Lấy danh sách nhật ký chăm sóc của lead (S3-01 & S3-02)
 */
router.get(
  '/:id/logs',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền xem nhật ký lead được giao cho mình',
      });
    }

    const logs = getLeadCareLogs(lead.id);

    res.status(200).json({
      success: true,
      data: logs,
    });
  },
);

/**
 * POST /api/leads/:id/logs
 * Thêm một dòng nhật ký chăm sóc cho lead (S3-01 & S3-02)
 */
router.post(
  '/:id/logs',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền thêm nhật ký cho lead được giao cho mình',
      });
    }

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
 * POST /api/leads/:id/convert
 * Chuyển một lead đã chốt thành hồ sơ học viên (Story S3-03 / IDTTX-211)
 * Tiêu chí nghiệm thu:
 * - Dữ liệu lead được chuyển sang hồ sơ học viên, không phải nhập lại
 * - Hệ thống sinh mã học viên tự động (HV26xxxx)
 * - Tài khoản đăng nhập cho học viên được tạo và gửi email (S25200212xxx@tms.vn)
 * - Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa
 */
router.post(
  '/:id/convert',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền chuyển đổi lead được giao cho mình',
      });
    }

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

/**
 * GET /api/leads/funnel
 * Lấy dữ liệu Bảng phễu Kanban 6 cột (S3-01)
 */
router.get(
  '/funnel',
  authenticate,
  authorize(ROLES.ADMIN, ROLES.TRAINING_MANAGER, ROLES.ADMISSIONS),
  (req, res) => {
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
  },
);

/**
 * PATCH /api/leads/:id/stage
 * Kéo thả để đổi trạng thái lead trên Bảng phễu (Story S3-01)
 * Tiêu chí nghiệm thu:
 * - Kéo thả để đổi trạng thái, mỗi lần đổi được ghi nhật ký (Audit Log)
 * - Chuyển sang Từ chối bắt buộc chọn lý do; nếu "Lý do khác" bắt buộc có ghi chú
 */
router.patch(
  '/:id/stage',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền chuyển trạng thái lead được giao cho mình',
      });
    }

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
 * GET /api/leads/:id/logs
 * Lấy danh sách nhật ký chăm sóc của lead (S3-01 & S3-02)
 */
router.get(
  '/:id/logs',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền xem nhật ký lead được giao cho mình',
      });
    }

    const logs = getLeadCareLogs(lead.id);

    res.status(200).json({
      success: true,
      data: logs,
    });
  },
);

/**
 * POST /api/leads/:id/logs
 * Thêm một dòng nhật ký chăm sóc cho lead (S3-01 & S3-02)
 */
router.post(
  '/:id/logs',
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

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tư vấn viên chỉ có quyền thêm nhật ký cho lead được giao cho mình',
      });
    }

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
      res.status(400).json({
        success: false,
        message: err.message,
      });
    }
  },
);

module.exports = router;

