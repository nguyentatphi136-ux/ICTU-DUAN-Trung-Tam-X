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
 * Thß╗æng k├¬ phß╗àu tuyß╗ân sinh
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
 * Danh s├ích t╞░ vß║Ñn vi├¬n c├│ sß║╡n ─æß╗â Quß║ún l├╜ ─æ├áo tß║ío ph├ón c├┤ng (S2-10)
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
 * T├¼m kiß║┐m v├á lß╗ìc lead theo nhiß╗üu ─æiß╗üu kiß╗çn (S2-11)
 * Ph├ón quyß╗ün: T╞░ vß║Ñn vi├¬n chß╗ë xem ─æ╞░ß╗úc lead ─æ╞░ß╗úc giao cho m├¼nh (S2-10)
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

    // Nghiß╗çp vß╗Ñ S2-10: "T╞░ vß║Ñn vi├¬n chß╗ë nh├¼n thß║Ñy lead ─æ╞░ß╗úc giao cho m├¼nh"
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
 * Ph├ón c├┤ng mß╗Öt hoß║╖c nhiß╗üu lead c├╣ng l├║c cho mß╗Öt t╞░ vß║Ñn vi├¬n (S2-10)
 * Chß╗ë Quß║ún l├╜ ─æ├áo tß║ío (TrainingManager) v├á Quß║ún trß╗ï vi├¬n (Admin) ─æ╞░ß╗úc ph├⌐p thß╗▒c hiß╗çn
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
        message: 'Vui l├▓ng chß╗ìn ├¡t nhß║Ñt mß╗Öt lead ─æß╗â ph├ón c├┤ng',
      });
    }

    if (!counselorId) {
      return res.status(400).json({
        success: false,
        code: 'MISSING_COUNSELOR_ID',
        message: 'Vui l├▓ng chß╗ìn t╞░ vß║Ñn vi├¬n ─æß╗â ph├ón c├┤ng',
      });
    }

    const counselor = findUserById(counselorId);
    if (!counselor) {
      return res.status(404).json({
        success: false,
        code: 'COUNSELOR_NOT_FOUND',
        message: 'Kh├┤ng t├¼m thß║Ñy t╞░ vß║Ñn vi├¬n ─æ╞░ß╗úc chß╗ë ─æß╗ïnh',
      });
    }

    if (counselor.status !== 'active') {
      return res.status(400).json({
        success: false,
        code: 'COUNSELOR_INACTIVE',
        message: 'T├ái khoß║ún t╞░ vß║Ñn vi├¬n n├áy ─æang bß╗ï kho├í hoß║╖c ng╞░ng hoß║ít ─æß╗Öng',
      });
    }

    if (!counselor.roles.includes(ROLES.ADMISSIONS) && !counselor.roles.includes(ROLES.ADMIN)) {
      return res.status(400).json({
        success: false,
        code: 'INVALID_ROLE_FOR_ASSIGNMENT',
        message: 'Ng╞░ß╗¥i ─æ╞░ß╗úc ph├ón c├┤ng phß║úi c├│ vai tr├▓ T╞░ vß║Ñn tuyß╗ân sinh (Admissions)',
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
        message: `─É├ú ph├ón c├┤ng th├ánh c├┤ng ${assignmentResult.updatedCount} lead cho ${counselor.name}`,
        data: assignmentResult,
      });
    } catch (err) {
      return res.status(500).json({
        success: false,
        message: err.message || 'Lß╗ùi khi ph├ón c├┤ng lead',
      });
    }
  },
);

/**
 * POST /api/leads
 * Tß║ío mß╗¢i lead
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
        message: 'Hß╗ì v├á t├¬n kh├┤ng ─æ╞░ß╗úc ─æß╗â trß╗æng',
      });
    }

    if (!phone || typeof phone !== 'string' || !phone.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Sß╗æ ─æiß╗çn thoß║íi kh├┤ng ─æ╞░ß╗úc ─æß╗â trß╗æng',
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
      message: 'Tß║ío th├┤ng tin lead th├ánh c├┤ng',
      data: enrichLeadInfo(newLead),
    });
  },
);

/**
 * GET /api/leads/:id
 * Xem th├┤ng tin chi tiß║┐t mß╗Öt lead (bao gß╗ôm timeline)
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
        message: 'Kh├┤ng t├¼m thß║Ñy Lead',
      });
    }

    const isOnlyAdmissions =
      req.user.roles.includes(ROLES.ADMISSIONS) &&
      !req.user.roles.includes(ROLES.ADMIN) &&
      !req.user.roles.includes(ROLES.TRAINING_MANAGER);

    // Nghiß╗çp vß╗Ñ S2-10: T╞░ vß║Ñn vi├¬n chß╗ë ─æ╞░ß╗úc xem lead cß╗ºa m├¼nh
    if (isOnlyAdmissions && String(lead.assignedCounselorId) !== String(req.user.id)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'T╞░ vß║Ñn vi├¬n chß╗ë c├│ quyß╗ün xem lead ─æ╞░ß╗úc giao cho m├¼nh',
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
 * Lß╗ïch sß╗¡ chuyß╗ân giao cß╗ºa mß╗Öt lead (S2-10)
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
        message: 'Kh├┤ng t├¼m thß║Ñy Lead',
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
        message: 'T╞░ vß║Ñn vi├¬n chß╗ë c├│ quyß╗ün xem lead ─æ╞░ß╗úc giao cho m├¼nh',
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
 * Th├¬m ghi ch├║/cuß╗Öc gß╗ìi v├áo timeline cß╗ºa lead (S2-11)
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
        message: 'Kh├┤ng t├¼m thß║Ñy Lead',
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
        message: 'T╞░ vß║Ñn vi├¬n chß╗ë c├│ quyß╗ün ghi ch├║ cho lead ─æ╞░ß╗úc giao cho m├¼nh',
      });
    }

    const { type, title, content, durationSeconds } = req.body || {};
    if (!content || !content.trim()) {
      return res.status(400).json({
        success: false,
        message: 'Nß╗Öi dung trao ─æß╗òi kh├┤ng ─æ╞░ß╗úc ─æß╗â trß╗æng',
      });
    }

    const interaction = addLeadInteraction(lead.id, {
      counselorId: req.user.id,
      type: type || 'NOTE',
      title: title || 'Ghi ch├║ trao ─æß╗òi',
      content: content.trim(),
      durationSeconds: durationSeconds ? Number(durationSeconds) : null,
    });

    res.status(201).json({
      success: true,
      message: '─É├ú l╞░u t╞░╞íng t├íc mß╗¢i v├áo lß╗ïch sß╗¡ kh├ích h├áng',
      data: interaction,
    });
  },
);

/**
 * PATCH /api/leads/:id/status
 * Cß║¡p nhß║¡t trß║íng th├íi lead
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
        message: 'Kh├┤ng t├¼m thß║Ñy Lead',
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
        message: 'T╞░ vß║Ñn vi├¬n chß╗ë c├│ quyß╗ün cß║¡p nhß║¡t trß║íng th├íi lead ─æ╞░ß╗úc giao cho m├¼nh',
      });
    }

    const { status, note } = req.body || {};
    if (!status) {
      return res.status(400).json({
        success: false,
        message: 'Vui l├▓ng cung cß║Ñp trß║íng th├íi mß╗¢i',
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
        message: 'Cß║¡p nhß║¡t trß║íng th├íi th├ánh c├┤ng',
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
