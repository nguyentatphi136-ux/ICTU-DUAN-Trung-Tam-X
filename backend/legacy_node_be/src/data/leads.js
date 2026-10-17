const {
  FUNNEL_STAGES,
  LEAD_SOURCE,
  LEAD_STATUS,
  LEAD_STATUS_LABELS,
  REJECT_REASONS,
  STAGE_TO_STATUS,
  STATUS_TO_STAGE,
  isValidRejectReason,
  isValidStage,
  normalizeSource,
  normalizeStage,
  normalizeStatus,
} = require('../constants/leads');
const { findUserById } = require('./users');

let leadIdCounter = 1;
let assignmentIdCounter = 1;
let interactionIdCounter = 1;
let stageLogIdCounter = 1;

/**
 * Danh sách Lead trong bộ nhớ (In-memory)
 */
const leads = [];

/**
 * Danh sách lịch sử phân công / chuyển giao Lead (S2-10)
 */
const leadAssignments = [];

/**
 * Danh sách lịch sử chuyển đổi trạng thái phễu (S3-01 Audit Log)
 */
const leadStageLogs = [];

/**
 * Nhật ký chăm sóc khách hàng theo ID lead (S3-01 & S3-02)
 */
const leadCareLogs = {};

/**
 * Tạo mới một Lead (hỗ trợ các trường S2-10, S2-11 và S3-01 Phễu Kanban)
 */
function createLead({
  fullName,
  phone,
  email = null,
  course = null,
  source = LEAD_SOURCE.WEBSITE,
  status = null,
  stage = null,
  callbackDate = null,
  callback = null,
  trialDate = null,
  trial = null,
  rejectReason = null,
  reason = null,
  rejectNote = null,
  studentCode = null,
  student = null,
  notes = null,
  assignedCounselorId = null,
  createdAt = null,
}) {
  const now = createdAt ? new Date(createdAt).toISOString() : new Date().toISOString();
  
  // Xác định trạng thái và giai đoạn phễu
  let normStage = stage ? normalizeStage(stage) : null;
  let normStatus = status ? normalizeStatus(status) : null;

  if (normStage && !normStatus) {
    normStatus = STAGE_TO_STATUS[normStage] || LEAD_STATUS.NEW;
  } else if (normStatus && !normStage) {
    normStage = STATUS_TO_STAGE[normStatus] || 'Mới';
  } else if (!normStage && !normStatus) {
    normStatus = LEAD_STATUS.NEW;
    normStage = 'Mới';
  }

  const lead = {
    id: String(leadIdCounter++),
    fullName: fullName.trim(),
    phone: phone.trim(),
    email: email && typeof email === 'string' && email.trim() ? email.trim().toLowerCase() : null,
    course: course && typeof course === 'string' ? course.trim() : null,
    source: normalizeSource(source),
    status: normStatus,
    stage: normStage,
    callbackDate: callbackDate || callback || null,
    trialDate: trialDate || trial || null,
    rejectReason: rejectReason || reason || null,
    rejectNote: rejectNote || null,
    studentCode: studentCode || student || null,
    notes: notes && typeof notes === 'string' ? notes.trim() : null,
    assignedCounselorId: assignedCounselorId ? String(assignedCounselorId) : null,
    lastInteractionAt: now,
    createdAt: now,
    updatedAt: now,
    timeline: [
      {
        id: String(interactionIdCounter++),
        time: now,
        type: 'CREATED',
        title: 'Tạo mới thông tin Lead',
        content: `Lead được tiếp nhận từ nguồn ${source || 'WEBSITE'}.`,
        author: 'Hệ thống',
      },
    ],
  };

  leads.unshift(lead);
  return lead;
}

/**
 * Tìm Lead theo ID
 */
function findLeadById(id) {
  return leads.find((l) => String(l.id) === String(id)) || null;
}

/**
 * Tính toán mốc thời gian cho datePreset
 */
function getDateRangeFromPreset(preset) {
  const now = new Date();
  const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate(), 0, 0, 0, 0);
  const endOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate(), 23, 59, 59, 999);

  switch (preset) {
    case 'today':
      return { from: startOfDay(now), to: endOfDay(now) };

    case 'yesterday': {
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      return { from: startOfDay(yesterday), to: endOfDay(yesterday) };
    }

    case 'this-week': {
      const current = new Date(now);
      const day = current.getDay();
      const diffToMonday = current.getDate() - day + (day === 0 ? -6 : 1);
      const monday = new Date(current.setDate(diffToMonday));
      return { from: startOfDay(monday), to: endOfDay(now) };
    }

    case 'this-month': {
      const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1, 0, 0, 0, 0);
      return { from: startOfMonth, to: endOfDay(now) };
    }

    case 'last-month': {
      // Đầu tháng trước đến cuối tháng trước (Story S2-11: tìm lại cuộc trao đổi từ tháng trước)
      const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0, 0);
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);
      return { from: startOfLastMonth, to: endOfLastMonth };
    }

    default:
      return null;
  }
}

/**
 * Tìm kiếm và lọc danh sách Lead (S2-11 & S2-10)
 */
function filterLeads({
  search = '',
  status = null,
  source = null,
  assignedCounselorId = null,
  createdFrom = null,
  createdTo = null,
  datePreset = null,
  page = 1,
  limit = 20,
  sortBy = 'createdAt',
  sortOrder = 'desc',
}) {
  let result = [...leads];

  // 1. Phân quyền / Lọc theo người phụ trách (S2-10 & S2-11)
  if (assignedCounselorId !== null && assignedCounselorId !== undefined && assignedCounselorId !== '') {
    if (assignedCounselorId === 'unassigned' || assignedCounselorId === 'null') {
      result = result.filter((l) => !l.assignedCounselorId);
    } else {
      result = result.filter((l) => String(l.assignedCounselorId) === String(assignedCounselorId));
    }
  }

  // 2. Lọc theo trạng thái (status)
  if (status) {
    const rawStatuses = Array.isArray(status) ? status : String(status).split(',');
    const normalizedStatuses = rawStatuses.map(normalizeStatus).filter(Boolean);
    if (normalizedStatuses.length > 0) {
      result = result.filter((l) => normalizedStatuses.includes(l.status));
    }
  }

  // 3. Lọc theo nguồn (source)
  if (source) {
    const rawSources = Array.isArray(source) ? source : String(source).split(',');
    const normalizedSources = rawSources.map(normalizeSource);
    result = result.filter((l) => normalizedSources.includes(l.source));
  }

  // 4. Lọc theo khoảng thời gian (khoảng ngày hoặc datePreset)
  let fromTime = null;
  let toTime = null;

  if (datePreset) {
    const range = getDateRangeFromPreset(datePreset);
    if (range) {
      fromTime = range.from.getTime();
      toTime = range.to.getTime();
    }
  }

  if (createdFrom) {
    fromTime = new Date(createdFrom).getTime();
  }
  if (createdTo) {
    const toDate = new Date(createdTo);
    // Nếu chỉ có ngày YYYY-MM-DD, set đến cuối ngày
    if (!createdTo.includes('T')) {
      toDate.setHours(23, 59, 59, 999);
    }
    toTime = toDate.getTime();
  }

  if (fromTime !== null && !isNaN(fromTime)) {
    result = result.filter((l) => new Date(l.createdAt).getTime() >= fromTime);
  }
  if (toTime !== null && !isNaN(toTime)) {
    result = result.filter((l) => new Date(l.createdAt).getTime() <= toTime);
  }

  // 5. Tìm nhanh theo tên hoặc số điện thoại (S2-11)
  if (search && typeof search === 'string' && search.trim() !== '') {
    const query = search.trim().toLowerCase();
    const cleanPhoneQuery = query.replace(/[\s.-]/g, '');

    result = result.filter((l) => {
      const matchName = l.fullName.toLowerCase().includes(query);
      const matchEmail = l.email ? l.email.toLowerCase().includes(query) : false;
      const matchCourse = l.course ? l.course.toLowerCase().includes(query) : false;
      const cleanLeadPhone = (l.phone || '').replace(/[\s.-]/g, '');
      const matchPhone = cleanLeadPhone.includes(cleanPhoneQuery);
      return matchName || matchPhone || matchEmail || matchCourse;
    });
  }

  // 6. Sắp xếp (Sorting)
  const isAsc = String(sortOrder).toLowerCase() === 'asc';
  result.sort((a, b) => {
    let valA = a[sortBy] || a.createdAt;
    let valB = b[sortBy] || b.createdAt;

    if (sortBy === 'createdAt' || sortBy === 'lastInteractionAt') {
      valA = new Date(valA).getTime();
      valB = new Date(valB).getTime();
    } else if (typeof valA === 'string') {
      return isAsc ? valA.localeCompare(valB) : valB.localeCompare(valA);
    }

    if (valA < valB) return isAsc ? -1 : 1;
    if (valA > valB) return isAsc ? 1 : -1;
    return 0;
  });

  // 7. Phân trang (Pagination)
  const total = result.length;
  const parsedPage = Math.max(1, parseInt(page, 10) || 1);
  const parsedLimit = Math.max(1, parseInt(limit, 10) || 20);
  const totalPages = Math.ceil(total / parsedLimit) || 1;
  const startIndex = (parsedPage - 1) * parsedLimit;
  const pagedData = result.slice(startIndex, startIndex + parsedLimit);

  // Thêm thông tin counselor đầy đủ vào response
  const enrichedData = pagedData.map(enrichLeadInfo);

  return {
    data: enrichedData,
    pagination: {
      total,
      page: parsedPage,
      limit: parsedLimit,
      totalPages,
    },
  };
}

/**
 * Tính số ngày quá hạn gọi lại so với ngày tham chiếu (mặc định '2026-10-06')
 * Nếu không có ngày hẹn, hoặc đã ở trạng thái Chốt / Từ chối thì không tính quá hạn (0)
 */
function calculateOverdueDays(lead, referenceDate = '2026-10-06') {
  const stage = lead.stage || STATUS_TO_STAGE[lead.status] || 'Mới';
  if (
    !lead.callbackDate ||
    stage === 'Chốt' ||
    stage === 'Từ chối' ||
    lead.status === LEAD_STATUS.WON ||
    lead.status === LEAD_STATUS.REJECTED
  ) {
    return 0;
  }
  const refTime = Date.parse(referenceDate);
  const cbTime = Date.parse(lead.callbackDate);
  if (isNaN(refTime) || isNaN(cbTime)) return 0;
  const diffDays = Math.round((refTime - cbTime) / 86400000);
  return Math.max(0, diffDays);
}

/**
 * Đính kèm tên và thông tin người phụ trách, các trường phễu Kanban vào object Lead
 */
function enrichLeadInfo(lead, referenceDate = '2026-10-06') {
  const counselor = lead.assignedCounselorId ? findUserById(lead.assignedCounselorId) : null;
  const stage = lead.stage || STATUS_TO_STAGE[lead.status] || 'Mới';
  const overdue = calculateOverdueDays(lead, referenceDate);

  return {
    ...lead,
    name: lead.fullName, // Alias tương thích frontend React S3-01
    program: lead.course, // Alias tương thích frontend React S3-01
    owner: counselor ? counselor.name : 'Chưa phân công',
    stage,
    stageLabel: stage,
    statusLabel: LEAD_STATUS_LABELS[lead.status] || lead.status,
    assignedCounselorName: counselor ? counselor.name : 'Chưa phân công',
    assignedCounselorEmail: counselor ? counselor.email : null,
    callback: lead.callbackDate || null,
    trial: lead.trialDate || null,
    reason: lead.rejectReason || null,
    student: lead.studentCode || null,
    overdueDays: overdue,
    isOverdue: overdue > 0,
  };
}

/**
 * Phân công một hoặc nhiều lead cùng lúc cho một tư vấn viên (Story S2-10)
 */
function assignLeads({ leadIds, counselorId, assignedBy, note = null }) {
  if (!Array.isArray(leadIds) || leadIds.length === 0) {
    throw new Error('Danh sách ID lead không được để trống');
  }

  const counselor = findUserById(counselorId);
  if (!counselor) {
    throw new Error('Tư vấn viên không tồn tại trong hệ thống');
  }
  if (counselor.status !== 'active') {
    throw new Error('Tài khoản tư vấn viên đang bị khoá');
  }

  const assigner = findUserById(assignedBy);
  const assignerName = assigner ? assigner.name : 'Quản lý';
  const now = new Date().toISOString();
  const updatedLeads = [];
  const createdAssignments = [];

  for (const rawId of leadIds) {
    const lead = findLeadById(rawId);
    if (!lead) continue;

    const previousCounselorId = lead.assignedCounselorId;
    const prevCounselor = previousCounselorId ? findUserById(previousCounselorId) : null;
    const prevName = prevCounselor ? prevCounselor.name : 'Chưa phân công';

    lead.assignedCounselorId = String(counselorId);
    lead.updatedAt = now;
    lead.lastInteractionAt = now;

    // Ghi nhật ký phân công vào timeline của lead (S2-10: Có ghi lịch sử chuyển giao)
    lead.timeline.unshift({
      id: String(interactionIdCounter++),
      time: now,
      type: 'ASSIGNMENT',
      title: 'Phân công tư vấn viên',
      content: `Chuyển người phụ trách từ "${prevName}" sang "${counselor.name}". ${note ? 'Ghi chú: ' + note : ''}`,
      author: assignerName,
    });

    // Lưu vào bảng lịch sử phân công (lead_assignments)
    const assignmentRecord = {
      id: String(assignmentIdCounter++),
      leadId: lead.id,
      previousCounselorId: previousCounselorId || null,
      newCounselorId: String(counselorId),
      assignedBy: String(assignedBy),
      assignedByName: assignerName,
      previousCounselorName: prevName,
      newCounselorName: counselor.name,
      note: note ? note.trim() : null,
      assignedAt: now,
    };
    leadAssignments.unshift(assignmentRecord);
    createdAssignments.push(assignmentRecord);

    updatedLeads.push(enrichLeadInfo(lead));
  }

  return {
    updatedCount: updatedLeads.length,
    leads: updatedLeads,
    assignments: createdAssignments,
  };
}

/**
 * Lấy lịch sử chuyển giao của một lead (S2-10)
 */
function getLeadAssignments(leadId) {
  return leadAssignments.filter((a) => String(a.leadId) === String(leadId));
}

/**
 * Thêm ghi chú hoặc cuộc gọi vào timeline của Lead (S2-11)
 */
function addLeadInteraction(leadId, { counselorId, type = 'NOTE', title, content, durationSeconds = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    throw new Error('Không tìm thấy Lead');
  }

  const counselor = findUserById(counselorId);
  const now = new Date().toISOString();

  const interaction = {
    id: String(interactionIdCounter++),
    time: now,
    type: type || 'NOTE',
    title: title || 'Ghi chú tư vấn',
    content: content || '',
    durationSeconds: durationSeconds || null,
    author: counselor ? counselor.name : 'Tư vấn viên',
  };

  lead.timeline.unshift(interaction);
  lead.lastInteractionAt = now;
  lead.updatedAt = now;

  return interaction;
}

/**
 * Cập nhật trạng thái của Lead (Legacy / S2-11)
 */
function updateLeadStatus(leadId, { status, note = '', counselorId = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    throw new Error('Không tìm thấy Lead');
  }

  // S3-03: Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa
  if (lead.isLocked || lead.student || lead.studentCode || lead.isConverted) {
    const err = new Error('Lead đã chuyển sang trạng thái Đã nhập học và không sửa được nữa');
    err.code = 'LEAD_LOCKED';
    err.statusCode = 400;
    throw err;
  }

  const normalized = normalizeStatus(status);
  if (!normalized) {
    throw new Error('Trạng thái không hợp lệ');
  }

  const oldStatusLabel = LEAD_STATUS_LABELS[lead.status] || lead.status;
  const newStatusLabel = LEAD_STATUS_LABELS[normalized] || normalized;
  const now = new Date().toISOString();
  const user = counselorId ? findUserById(counselorId) : null;

  lead.status = normalized;
  lead.stage = STATUS_TO_STAGE[normalized] || lead.stage;
  lead.updatedAt = now;
  lead.lastInteractionAt = now;

  lead.timeline.unshift({
    id: String(interactionIdCounter++),
    time: now,
    type: 'STATUS_CHANGE',
    title: `Cập nhật trạng thái: ${newStatusLabel}`,
    content: `Chuyển trạng thái từ "${oldStatusLabel}" sang "${newStatusLabel}". ${note ? 'Ghi chú: ' + note : ''}`,
    author: user ? user.name : 'Hệ thống',
  });

  return enrichLeadInfo(lead);
}

/**
 * Chuyển đổi giai đoạn thẻ lead trên Phễu Kanban (Story S3-01)
 * Tiêu chí nghiệm thu:
 * - Kéo thả để đổi trạng thái, mỗi lần đổi được ghi nhật ký (Audit Log)
 * - Chuyển sang Từ chối bắt buộc chọn lý do; nếu "Lý do khác" bắt buộc có ghi chú
 */
function moveLeadStage(leadId, { stage, reason = null, note = null, counselorId = null, referenceDate = '2026-10-06' }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    const err = new Error('Không tìm thấy Lead');
    err.code = 'LEAD_NOT_FOUND';
    err.statusCode = 404;
    throw err;
  }

  // S3-03: Lead chuyển sang trạng thái Đã nhập học và không sửa được nữa
  if (lead.isLocked || lead.student || lead.studentCode || lead.isConverted) {
    const err = new Error('Lead đã chuyển sang trạng thái Đã nhập học và không sửa được nữa');
    err.code = 'LEAD_LOCKED';
    err.statusCode = 400;
    throw err;
  }

  const normalizedStage = normalizeStage(stage);
  if (!normalizedStage) {
    const err = new Error('Giai đoạn phễu không hợp lệ');
    err.code = 'INVALID_STAGE';
    err.statusCode = 400;
    throw err;
  }

  const oldStage = lead.stage || STATUS_TO_STAGE[lead.status] || 'Mới';
  if (oldStage === normalizedStage) {
    return enrichLeadInfo(lead, referenceDate);
  }

  // Tiêu chí S3-01: Chuyển sang Từ chối bắt buộc chọn lý do
  if (normalizedStage === 'Từ chối') {
    if (!reason || typeof reason !== 'string' || !reason.trim()) {
      const err = new Error('Bắt buộc chọn lý do khi chuyển lead sang trạng thái Từ chối');
      err.code = 'REJECT_REASON_REQUIRED';
      err.statusCode = 400;
      throw err;
    }

    const trimmedReason = reason.trim();
    if (trimmedReason === 'Lý do khác' && (!note || typeof note !== 'string' || !note.trim())) {
      const err = new Error('Vui lòng ghi rõ lý do vào ô ghi chú khi chọn Lý do khác');
      err.code = 'REJECT_NOTE_REQUIRED';
      err.statusCode = 400;
      throw err;
    }

    if (!isValidRejectReason(trimmedReason)) {
      const err = new Error('Lý do từ chối không hợp lệ');
      err.code = 'INVALID_REJECT_REASON';
      err.statusCode = 400;
      throw err;
    }

    lead.rejectReason = trimmedReason === 'Lý do khác' && note && note.trim() ? note.trim() : trimmedReason;
    lead.rejectNote = note ? note.trim() : null;
  } else {
    // Chuyển sang các cột khác thì xóa lý do từ chối
    lead.rejectReason = null;
    lead.rejectNote = null;
  }

  const newStatus = STAGE_TO_STATUS[normalizedStage] || LEAD_STATUS.NEW;
  lead.stage = normalizedStage;
  lead.status = newStatus;

  const now = new Date().toISOString();
  lead.updatedAt = now;
  lead.lastInteractionAt = now;

  const counselor = counselorId ? findUserById(counselorId) : null;
  const authorName = counselor ? counselor.name : 'Tư vấn viên';

  // Format nội dung nhật ký đổi trạng thái: "${oldStage} → ${newStage}" kèm lý do nếu có
  const logText = `${oldStage} → ${normalizedStage}` + (lead.rejectReason ? ` · ${lead.rejectReason}` : '');

  // 1. Ghi vào timeline chi tiết của lead
  lead.timeline.unshift({
    id: String(interactionIdCounter++),
    time: now,
    type: 'STATUS_CHANGE',
    title: 'Đổi giai đoạn phễu',
    content: logText + (note && lead.rejectReason !== note ? ` (${note})` : ''),
    author: authorName,
  });

  // 2. Ghi vào leadStageLogs (Audit Log chuyển phễu)
  const stageLog = {
    id: String(stageLogIdCounter++),
    leadId: lead.id,
    fromStage: oldStage,
    toStage: normalizedStage,
    reason: lead.rejectReason,
    note: lead.rejectNote,
    changedByUserId: counselor ? String(counselor.id) : null,
    changedByName: authorName,
    createdAt: now,
  };
  leadStageLogs.unshift(stageLog);

  // 3. Ghi vào Care Logs (Nhật ký chăm sóc S3-01 / S3-02)
  if (!leadCareLogs[String(lead.id)]) {
    leadCareLogs[String(lead.id)] = [];
  }
  leadCareLogs[String(lead.id)].unshift({
    kind: 'Đổi trạng thái',
    at: now,
    by: authorName,
    text: logText,
  });

  return enrichLeadInfo(lead, referenceDate);
}

/**
 * Lấy dữ liệu Bảng phễu Kanban 6 cột (Story S3-01)
 * Hỗ trợ lọc theo từ khóa tìm kiếm (tên, SĐT), chỉ hiện lead quá hạn gọi lại, và theo tư vấn viên phụ trách
 */
function getFunnelData({ search = '', onlyLate = false, counselorId = null, referenceDate = '2026-10-06' } = {}) {
  let targetLeads = leads;

  if (counselorId !== null && counselorId !== undefined && counselorId !== '') {
    if (counselorId === 'unassigned') {
      targetLeads = targetLeads.filter((l) => !l.assignedCounselorId);
    } else {
      targetLeads = targetLeads.filter((l) => String(l.assignedCounselorId) === String(counselorId));
    }
  }

  let enriched = targetLeads.map((l) => enrichLeadInfo(l, referenceDate));

  // Thống kê số lượng lead trên 6 cột phễu (trước khi search/lọc late)
  const counts = {
    'Mới': 0,
    'Đã liên hệ': 0,
    'Đang tư vấn': 0,
    'Hẹn học thử': 0,
    'Chốt': 0,
    'Từ chối': 0,
    NEW: 0,
    CONTACTED: 0,
    CONSULTING: 0,
    TRIAL_SCHEDULED: 0,
    WON: 0,
    REJECTED: 0,
  };

  enriched.forEach((l) => {
    if (counts[l.stage] !== undefined) counts[l.stage]++;
    if (counts[l.status] !== undefined) counts[l.status]++;
  });

  const total = enriched.length;
  const lateCount = enriched.filter((l) => l.overdueDays > 0).length;

  // Lọc tìm kiếm theo tên hoặc số điện thoại (tương thích cả frontend React S3-01)
  if (search && search.trim()) {
    const q = search.trim().toLowerCase();
    const digits = q.replace(/\D/g, '');
    enriched = enriched.filter((l) => {
      const matchName = l.fullName ? l.fullName.toLowerCase().includes(q) : false;
      const cleanPhone = (l.phone || '').replace(/\D/g, '');
      const matchPhone = digits.length > 2 && cleanPhone.includes(digits);
      return matchName || matchPhone;
    });
  }

  // Lọc chỉ hiện lead quá hạn gọi lại (onlyLate)
  if (onlyLate === true || onlyLate === 'true' || onlyLate === 1 || onlyLate === '1') {
    enriched = enriched.filter((l) => l.overdueDays > 0);
  }

  // Phân nhóm thành 6 cột Kanban
  const columns = {
    'Mới': enriched.filter((l) => l.stage === 'Mới'),
    'Đã liên hệ': enriched.filter((l) => l.stage === 'Đã liên hệ'),
    'Đang tư vấn': enriched.filter((l) => l.stage === 'Đang tư vấn'),
    'Hẹn học thử': enriched.filter((l) => l.stage === 'Hẹn học thử'),
    'Chốt': enriched.filter((l) => l.stage === 'Chốt'),
    'Từ chối': enriched.filter((l) => l.stage === 'Từ chối'),
  };

  return {
    total,
    lateCount,
    counts,
    leads: enriched,
    columns,
  };
}

/**
 * Lấy nhật ký chăm sóc của Lead (S3-01 & S3-02)
 */
function getLeadCareLogs(leadId) {
  const lead = findLeadById(leadId);
  if (!lead) return [];
  const counselor = lead.assignedCounselorId ? findUserById(lead.assignedCounselorId) : null;
  const defaultLogs = [
    {
      kind: 'Phân công',
      at: lead.createdAt,
      by: counselor ? counselor.name : 'Quản lý',
      text: counselor ? `Giao cho ${counselor.name}` : 'Tiếp nhận lead từ biểu mẫu web',
    },
  ];
  const list = leadCareLogs[String(leadId)] ?? defaultLogs;
  return [...list].sort((a, b) => (b.at || '').localeCompare(a.at || ''));
}

/**
 * Thêm một dòng nhật ký chăm sóc (S3-01 & S3-02)
 */
function addLeadCareLog(leadId, { kind = 'Ghi chú', text, at = null, by = null, counselorId = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    const err = new Error('Không tìm thấy Lead');
    err.code = 'LEAD_NOT_FOUND';
    err.statusCode = 404;
    throw err;
  }

  // S3-03: Lead chuyển sang trạng thái Đã nhập học và không thêm nhật ký được nữa
  if (lead.isLocked || lead.student || lead.studentCode || lead.isConverted) {
    const err = new Error('Lead đã chuyển sang trạng thái Đã nhập học và không thể thêm nhật ký');
    err.code = 'LEAD_LOCKED';
    err.statusCode = 400;
    throw err;
  }

  const now = at || new Date().toISOString();
  const user = counselorId ? findUserById(counselorId) : null;
  const authorName = by || (user ? user.name : 'Tư vấn viên');

  const logEntry = {
    kind: kind || 'Ghi chú',
    at: now,
    by: authorName,
    text: text.trim(),
  };

  if (!leadCareLogs[String(leadId)]) {
    leadCareLogs[String(leadId)] = [];
  }
  leadCareLogs[String(leadId)].unshift(logEntry);

  lead.timeline.unshift({
    id: String(interactionIdCounter++),
    time: now,
    type: kind === 'Đổi trạng thái' ? 'STATUS_CHANGE' : 'NOTE',
    title: `Nhật ký: ${kind}`,
    content: text.trim(),
    author: authorName,
  });

  lead.lastInteractionAt = now;
  lead.updatedAt = now;

  return logEntry;
}

/**
 * Lấy danh sách lịch sử chuyển đổi giai đoạn phễu
 */
function getLeadStageLogs(leadId = null) {
  if (leadId) {
    return leadStageLogs.filter((l) => String(l.leadId) === String(leadId));
  }
  return leadStageLogs;
}

/**
 * Lấy thống kê phễu tuyển sinh (cho cards / dashboard)
 */
function getLeadStats({ counselorId = null } = {}) {
  let targetLeads = leads;
  if (counselorId) {
    targetLeads = leads.filter((l) => String(l.assignedCounselorId) === String(counselorId));
  }

  return {
    total: targetLeads.length,
    new: targetLeads.filter((l) => l.status === LEAD_STATUS.NEW).length,
    contacted: targetLeads.filter((l) => l.status === LEAD_STATUS.CONTACTED).length,
    consulting: targetLeads.filter((l) => l.status === LEAD_STATUS.CONSULTING).length,
    trialScheduled: targetLeads.filter((l) => l.status === LEAD_STATUS.TRIAL_SCHEDULED).length,
    won: targetLeads.filter((l) => l.status === LEAD_STATUS.WON).length,
    rejected: targetLeads.filter((l) => l.status === LEAD_STATUS.REJECTED).length,
    unassigned: targetLeads.filter((l) => !l.assignedCounselorId).length,
    // Bổ sung các chỉ số S3-01
    stages: {
      'Mới': targetLeads.filter((l) => (l.stage || 'Mới') === 'Mới').length,
      'Đã liên hệ': targetLeads.filter((l) => l.stage === 'Đã liên hệ').length,
      'Đang tư vấn': targetLeads.filter((l) => l.stage === 'Đang tư vấn').length,
      'Hẹn học thử': targetLeads.filter((l) => l.stage === 'Hẹn học thử').length,
      'Chốt': targetLeads.filter((l) => l.stage === 'Chốt').length,
      'Từ chối': targetLeads.filter((l) => l.stage === 'Từ chối').length,
    },
  };
}

/**
 * Xóa trắng và nạp lại dữ liệu mẫu (dùng cho test hoặc khởi động)
 */
function seedSampleLeads() {
  leads.length = 0;
  leadAssignments.length = 0;
  leadStageLogs.length = 0;
  for (const k of Object.keys(leadCareLogs)) {
    delete leadCareLogs[k];
  }
  leadIdCounter = 1;
  assignmentIdCounter = 1;
  interactionIdCounter = 1;
  stageLogIdCounter = 1;

  const now = new Date();

  // 1. Lead tạo từ tháng trước (Phục vụ S2-11 & S3-01: Vũ Minh Anh - Đang tư vấn, có hẹn gọi lại quá hạn)
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 15, 10, 30, 0);
  createLead({
    fullName: 'Vũ Minh Anh',
    phone: '0988123456',
    email: 'minhanh.vu@gmail.com',
    course: 'Lập trình Web Fullstack',
    source: LEAD_SOURCE.FACEBOOK,
    status: LEAD_STATUS.CONSULTING,
    stage: 'Đang tư vấn',
    callbackDate: '2026-10-05', // Quá hạn (với mốc 2026-10-06)
    notes: 'Quan tâm lộ trình từ cơ bản, hỏi về học phí trả góp 0%',
    assignedCounselorId: '5', // Lê Thị Thu Hà
    createdAt: lastMonthDate.toISOString(),
  });

  // 2. Lead tuần trước (Ngô Quốc Bảo - Hẹn học thử)
  const lastWeekDate = new Date(now);
  lastWeekDate.setDate(lastWeekDate.getDate() - 7);
  createLead({
    fullName: 'Ngô Quốc Bảo',
    phone: '0912345679',
    email: 'bao.ngo@hotmail.com',
    course: 'Java Spring Boot',
    source: LEAD_SOURCE.WEBSITE,
    status: LEAD_STATUS.TRIAL_SCHEDULED,
    stage: 'Hẹn học thử',
    trialDate: '2026-10-10',
    notes: 'Hẹn test đầu vào chiều thứ 7 phòng P.102',
    assignedCounselorId: '5',
    createdAt: lastWeekDate.toISOString(),
  });

  // 3. Lead hôm nay - chưa phân công (Bùi Thu Hà - Mới)
  createLead({
    fullName: 'Bùi Thu Hà',
    phone: '0977889900',
    email: 'thuha.bui@gmail.com',
    course: 'Python & Data Analysis',
    source: LEAD_SOURCE.REFERRAL,
    status: LEAD_STATUS.NEW,
    stage: 'Mới',
    notes: 'Bạn học giới thiệu, muốn tư vấn lớp học ca tối',
    assignedCounselorId: null, // Chưa phân công -> Dùng test S2-10
    createdAt: now.toISOString(),
  });

  // 4. Lead hôm nay - chưa phân công (Đặng Hoàng Nam - Mới)
  createLead({
    fullName: 'Đặng Hoàng Nam',
    phone: '0933221100',
    email: 'nam.dang@gmail.com',
    course: 'Thiết kế UI/UX',
    source: LEAD_SOURCE.TIKTOK,
    status: LEAD_STATUS.NEW,
    stage: 'Mới',
    notes: 'Điền form từ link bio TikTok',
    assignedCounselorId: null, // Chưa phân công -> Dùng test S2-10
    createdAt: now.toISOString(),
  });

  // 5. Lead đã chốt đăng ký tháng trước của tư vấn viên 6 (Trần Minh Đức - Chốt)
  createLead({
    fullName: 'Trần Minh Đức',
    phone: '0903456789',
    email: 'duc.tm@gmail.com',
    course: 'Lập trình Frontend React',
    source: LEAD_SOURCE.HOTLINE,
    status: LEAD_STATUS.WON,
    stage: 'Chốt',
    notes: 'Đã đóng học phí đầy đủ',
    assignedCounselorId: '6', // Hoàng Văn Tư
    createdAt: lastMonthDate.toISOString(),
  });

  // 6. Lead Đã liên hệ - quá hạn gọi lại (Trần Văn Hùng - Đã liên hệ, callback: 2026-10-04)
  createLead({
    fullName: 'Trần Văn Hùng',
    phone: '0903664518',
    email: 'hung.tv@gmail.com',
    course: 'Lập trình web Full-stack',
    source: LEAD_SOURCE.REFERRAL,
    status: LEAD_STATUS.CONTACTED,
    stage: 'Đã liên hệ',
    callbackDate: '2026-10-04',
    assignedCounselorId: '5',
    createdAt: lastWeekDate.toISOString(),
  });

  // 7. Lead Từ chối có lý do (Hoàng Mai Lan - Từ chối: Học phí cao)
  createLead({
    fullName: 'Hoàng Mai Lan',
    phone: '0977418902',
    email: 'lan.hm@gmail.com',
    course: 'Phân tích dữ liệu',
    source: LEAD_SOURCE.WEBSITE,
    status: LEAD_STATUS.REJECTED,
    stage: 'Từ chối',
    rejectReason: 'Học phí cao',
    assignedCounselorId: '5',
    createdAt: lastMonthDate.toISOString(),
  });

  // 8. Lead Từ chối có lý do (Kiều Văn Sơn - Từ chối: Đã chọn trung tâm khác)
  createLead({
    fullName: 'Kiều Văn Sơn',
    phone: '0988257613',
    email: 'son.kv@gmail.com',
    course: 'Lập trình Java Backend',
    source: LEAD_SOURCE.WEBSITE,
    status: LEAD_STATUS.REJECTED,
    stage: 'Từ chối',
    rejectReason: 'Đã chọn trung tâm khác',
    assignedCounselorId: '5',
    createdAt: lastMonthDate.toISOString(),
  });
}

// Khởi tạo sẵn dữ liệu demo ban đầu
seedSampleLeads();

module.exports = {
  addLeadCareLog,
  addLeadInteraction,
  assignLeads,
  calculateOverdueDays,
  createLead,
  enrichLeadInfo,
  filterLeads,
  findLeadById,
  getFunnelData,
  getLeadAssignments,
  getLeadCareLogs,
  getLeadStageLogs,
  getLeadStats,
  leadAssignments,
  leadCareLogs,
  leadStageLogs,
  leads,
  moveLeadStage,
  seedSampleLeads,
  updateLeadStatus,
};
