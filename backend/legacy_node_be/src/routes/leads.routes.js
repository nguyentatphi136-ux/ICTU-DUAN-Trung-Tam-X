const express = require('express');
const { ROLES } = require('../constants/roles');
const { isValidLeadStatus, isValidLeadSource } = require('../constants/leads');
const { authenticate, authorize } = require('../middleware/auth');
const { findUserById, findUsersByRole } = require('../data/users');
const {
  addLeadInteraction,
  assignLeads,
  createLead,
  enrichLeadInfo,
  filterLeads,
  findLeadById,
  getLeadAssignments,
  getLeadStats,
  updateLeadStatus,
} = require('../data/leads');

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
      res.status(400).json({
        success: false,
        message: err.message,
      });
    }
  },
);

module.exports = router;
