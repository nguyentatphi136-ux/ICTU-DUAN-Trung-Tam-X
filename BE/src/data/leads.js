const {
  LEAD_SOURCE,
  LEAD_STATUS,
  LEAD_STATUS_LABELS,
  normalizeSource,
  normalizeStatus,
} = require('../constants/leads');
const { findUserById } = require('./users');

let leadIdCounter = 1;
let assignmentIdCounter = 1;
let interactionIdCounter = 1;

/**
 * Danh sách Lead trong bộ nhớ (In-memory)
 */
const leads = [];

/**
 * Danh sách lịch sử phân công / chuyển giao Lead (S2-10)
 */
const leadAssignments = [];

/**
 * Tạo mới một Lead
 */
function createLead({
  fullName,
  phone,
  email = null,
  course = null,
  source = LEAD_SOURCE.WEBSITE,
  status = LEAD_STATUS.NEW,
  notes = null,
  assignedCounselorId = null,
  createdAt = null,
}) {
  const now = createdAt ? new Date(createdAt).toISOString() : new Date().toISOString();
  const lead = {
    id: String(leadIdCounter++),
    fullName: fullName.trim(),
    phone: phone.trim(),
    email: email && typeof email === 'string' && email.trim() ? email.trim().toLowerCase() : null,
    course: course && typeof course === 'string' ? course.trim() : null,
    source: normalizeSource(source),
    status: normalizeStatus(status) || LEAD_STATUS.NEW,
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
 * Đính kèm tên và thông tin người phụ trách vào object Lead
 */
function enrichLeadInfo(lead) {
  const counselor = lead.assignedCounselorId ? findUserById(lead.assignedCounselorId) : null;
  return {
    ...lead,
    statusLabel: LEAD_STATUS_LABELS[lead.status] || lead.status,
    assignedCounselorName: counselor ? counselor.name : 'Chưa phân công',
    assignedCounselorEmail: counselor ? counselor.email : null,
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
 * Cập nhật trạng thái của Lead
 */
function updateLeadStatus(leadId, { status, note = '', counselorId = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    throw new Error('Không tìm thấy Lead');
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
  };
}

/**
 * Xóa trắng và nạp lại dữ liệu mẫu (dùng cho test hoặc khởi động)
 */
function seedSampleLeads() {
  leads.length = 0;
  leadAssignments.length = 0;
  leadIdCounter = 1;
  assignmentIdCounter = 1;
  interactionIdCounter = 1;

  const now = new Date();

  // 1. Lead tạo từ tháng trước (Phục vụ S2-11: "tìm lại được cuộc trao đổi từ tháng trước khi khách gọi lại")
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 15, 10, 30, 0);
  createLead({
    fullName: 'Vũ Minh Anh',
    phone: '0988123456',
    email: 'minhanh.vu@gmail.com',
    course: 'Lập trình Web Fullstack',
    source: LEAD_SOURCE.FACEBOOK,
    status: LEAD_STATUS.CONSULTING,
    notes: 'Quan tâm lộ trình từ cơ bản, hỏi về học phí trả góp 0%',
    assignedCounselorId: '5', // Lê Thị Thu Hà
    createdAt: lastMonthDate.toISOString(),
  });

  // 2. Lead tuần trước
  const lastWeekDate = new Date(now);
  lastWeekDate.setDate(lastWeekDate.getDate() - 7);
  createLead({
    fullName: 'Ngô Quốc Bảo',
    phone: '0912345679',
    email: 'bao.ngo@hotmail.com',
    course: 'Java Spring Boot',
    source: LEAD_SOURCE.WEBSITE,
    status: LEAD_STATUS.TRIAL_SCHEDULED,
    notes: 'Hẹn test đầu vào chiều thứ 7 phòng P.102',
    assignedCounselorId: '5',
    createdAt: lastWeekDate.toISOString(),
  });

  // 3. Lead hôm nay - chưa phân công
  createLead({
    fullName: 'Bùi Thu Hà',
    phone: '0977889900',
    email: 'thuha.bui@gmail.com',
    course: 'Python & Data Analysis',
    source: LEAD_SOURCE.REFERRAL,
    status: LEAD_STATUS.NEW,
    notes: 'Bạn học giới thiệu, muốn tư vấn lớp học ca tối',
    assignedCounselorId: null, // Chưa phân công -> Dùng test S2-10
    createdAt: now.toISOString(),
  });

  // 4. Lead hôm nay - chưa phân công
  createLead({
    fullName: 'Đặng Hoàng Nam',
    phone: '0933221100',
    email: 'nam.dang@gmail.com',
    course: 'Thiết kế UI/UX',
    source: LEAD_SOURCE.TIKTOK,
    status: LEAD_STATUS.NEW,
    notes: 'Điền form từ link bio TikTok',
    assignedCounselorId: null, // Chưa phân công -> Dùng test S2-10
    createdAt: now.toISOString(),
  });

  // 5. Lead đã chốt đăng ký tháng trước của tư vấn viên 6
  createLead({
    fullName: 'Trần Minh Đức',
    phone: '0903456789',
    email: 'duc.tran@yahoo.com',
    course: 'Lập trình Frontend React',
    source: LEAD_SOURCE.HOTLINE,
    status: LEAD_STATUS.WON,
    notes: 'Đã đóng học phí đầy đủ',
    assignedCounselorId: '6', // Hoàng Văn Tư
    createdAt: lastMonthDate.toISOString(),
  });
}

// Khởi tạo sẵn dữ liệu demo ban đầu
seedSampleLeads();

module.exports = {
  addLeadInteraction,
  assignLeads,
  createLead,
  enrichLeadInfo,
  filterLeads,
  findLeadById,
  getLeadAssignments,
  getLeadStats,
  leadAssignments,
  leads,
  seedSampleLeads,
  updateLeadStatus,
};
