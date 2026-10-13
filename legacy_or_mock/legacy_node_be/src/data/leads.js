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
 * Danh s├ích Lead trong bß╗Ö nhß╗¢ (In-memory)
 */
const leads = [];

/**
 * Danh s├ích lß╗ïch sß╗¡ ph├ón c├┤ng / chuyß╗ân giao Lead (S2-10)
 */
const leadAssignments = [];

/**
 * Tß║ío mß╗¢i mß╗Öt Lead
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
        title: 'Tß║ío mß╗¢i th├┤ng tin Lead',
        content: `Lead ─æ╞░ß╗úc tiß║┐p nhß║¡n tß╗½ nguß╗ôn ${source || 'WEBSITE'}.`,
        author: 'Hß╗ç thß╗æng',
      },
    ],
  };

  leads.unshift(lead);
  return lead;
}

/**
 * T├¼m Lead theo ID
 */
function findLeadById(id) {
  return leads.find((l) => String(l.id) === String(id)) || null;
}

/**
 * T├¡nh to├ín mß╗æc thß╗¥i gian cho datePreset
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
      // ─Éß║ºu th├íng tr╞░ß╗¢c ─æß║┐n cuß╗æi th├íng tr╞░ß╗¢c (Story S2-11: t├¼m lß║íi cuß╗Öc trao ─æß╗òi tß╗½ th├íng tr╞░ß╗¢c)
      const startOfLastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1, 0, 0, 0, 0);
      const endOfLastMonth = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59, 999);
      return { from: startOfLastMonth, to: endOfLastMonth };
    }

    default:
      return null;
  }
}

/**
 * T├¼m kiß║┐m v├á lß╗ìc danh s├ích Lead (S2-11 & S2-10)
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

  // 1. Ph├ón quyß╗ün / Lß╗ìc theo ng╞░ß╗¥i phß╗Ñ tr├ích (S2-10 & S2-11)
  if (assignedCounselorId !== null && assignedCounselorId !== undefined && assignedCounselorId !== '') {
    if (assignedCounselorId === 'unassigned' || assignedCounselorId === 'null') {
      result = result.filter((l) => !l.assignedCounselorId);
    } else {
      result = result.filter((l) => String(l.assignedCounselorId) === String(assignedCounselorId));
    }
  }

  // 2. Lß╗ìc theo trß║íng th├íi (status)
  if (status) {
    const rawStatuses = Array.isArray(status) ? status : String(status).split(',');
    const normalizedStatuses = rawStatuses.map(normalizeStatus).filter(Boolean);
    if (normalizedStatuses.length > 0) {
      result = result.filter((l) => normalizedStatuses.includes(l.status));
    }
  }

  // 3. Lß╗ìc theo nguß╗ôn (source)
  if (source) {
    const rawSources = Array.isArray(source) ? source : String(source).split(',');
    const normalizedSources = rawSources.map(normalizeSource);
    result = result.filter((l) => normalizedSources.includes(l.source));
  }

  // 4. Lß╗ìc theo khoß║úng thß╗¥i gian (khoß║úng ng├áy hoß║╖c datePreset)
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
    // Nß║┐u chß╗ë c├│ ng├áy YYYY-MM-DD, set ─æß║┐n cuß╗æi ng├áy
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

  // 5. T├¼m nhanh theo t├¬n hoß║╖c sß╗æ ─æiß╗çn thoß║íi (S2-11)
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

  // 6. Sß║»p xß║┐p (Sorting)
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

  // 7. Ph├ón trang (Pagination)
  const total = result.length;
  const parsedPage = Math.max(1, parseInt(page, 10) || 1);
  const parsedLimit = Math.max(1, parseInt(limit, 10) || 20);
  const totalPages = Math.ceil(total / parsedLimit) || 1;
  const startIndex = (parsedPage - 1) * parsedLimit;
  const pagedData = result.slice(startIndex, startIndex + parsedLimit);

  // Th├¬m th├┤ng tin counselor ─æß║ºy ─æß╗º v├áo response
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
 * ─É├¡nh k├¿m t├¬n v├á th├┤ng tin ng╞░ß╗¥i phß╗Ñ tr├ích v├áo object Lead
 */
function enrichLeadInfo(lead) {
  const counselor = lead.assignedCounselorId ? findUserById(lead.assignedCounselorId) : null;
  return {
    ...lead,
    statusLabel: LEAD_STATUS_LABELS[lead.status] || lead.status,
    assignedCounselorName: counselor ? counselor.name : 'Ch╞░a ph├ón c├┤ng',
    assignedCounselorEmail: counselor ? counselor.email : null,
  };
}

/**
 * Ph├ón c├┤ng mß╗Öt hoß║╖c nhiß╗üu lead c├╣ng l├║c cho mß╗Öt t╞░ vß║Ñn vi├¬n (Story S2-10)
 */
function assignLeads({ leadIds, counselorId, assignedBy, note = null }) {
  if (!Array.isArray(leadIds) || leadIds.length === 0) {
    throw new Error('Danh s├ích ID lead kh├┤ng ─æ╞░ß╗úc ─æß╗â trß╗æng');
  }

  const counselor = findUserById(counselorId);
  if (!counselor) {
    throw new Error('T╞░ vß║Ñn vi├¬n kh├┤ng tß╗ôn tß║íi trong hß╗ç thß╗æng');
  }
  if (counselor.status !== 'active') {
    throw new Error('T├ái khoß║ún t╞░ vß║Ñn vi├¬n ─æang bß╗ï kho├í');
  }

  const assigner = findUserById(assignedBy);
  const assignerName = assigner ? assigner.name : 'Quß║ún l├╜';
  const now = new Date().toISOString();
  const updatedLeads = [];
  const createdAssignments = [];

  for (const rawId of leadIds) {
    const lead = findLeadById(rawId);
    if (!lead) continue;

    const previousCounselorId = lead.assignedCounselorId;
    const prevCounselor = previousCounselorId ? findUserById(previousCounselorId) : null;
    const prevName = prevCounselor ? prevCounselor.name : 'Ch╞░a ph├ón c├┤ng';

    lead.assignedCounselorId = String(counselorId);
    lead.updatedAt = now;
    lead.lastInteractionAt = now;

    // Ghi nhß║¡t k├╜ ph├ón c├┤ng v├áo timeline cß╗ºa lead (S2-10: C├│ ghi lß╗ïch sß╗¡ chuyß╗ân giao)
    lead.timeline.unshift({
      id: String(interactionIdCounter++),
      time: now,
      type: 'ASSIGNMENT',
      title: 'Ph├ón c├┤ng t╞░ vß║Ñn vi├¬n',
      content: `Chuyß╗ân ng╞░ß╗¥i phß╗Ñ tr├ích tß╗½ "${prevName}" sang "${counselor.name}". ${note ? 'Ghi ch├║: ' + note : ''}`,
      author: assignerName,
    });

    // L╞░u v├áo bß║úng lß╗ïch sß╗¡ ph├ón c├┤ng (lead_assignments)
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
 * Lß║Ñy lß╗ïch sß╗¡ chuyß╗ân giao cß╗ºa mß╗Öt lead (S2-10)
 */
function getLeadAssignments(leadId) {
  return leadAssignments.filter((a) => String(a.leadId) === String(leadId));
}

/**
 * Th├¬m ghi ch├║ hoß║╖c cuß╗Öc gß╗ìi v├áo timeline cß╗ºa Lead (S2-11)
 */
function addLeadInteraction(leadId, { counselorId, type = 'NOTE', title, content, durationSeconds = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    throw new Error('Kh├┤ng t├¼m thß║Ñy Lead');
  }

  const counselor = findUserById(counselorId);
  const now = new Date().toISOString();

  const interaction = {
    id: String(interactionIdCounter++),
    time: now,
    type: type || 'NOTE',
    title: title || 'Ghi ch├║ t╞░ vß║Ñn',
    content: content || '',
    durationSeconds: durationSeconds || null,
    author: counselor ? counselor.name : 'T╞░ vß║Ñn vi├¬n',
  };

  lead.timeline.unshift(interaction);
  lead.lastInteractionAt = now;
  lead.updatedAt = now;

  return interaction;
}

/**
 * Cß║¡p nhß║¡t trß║íng th├íi cß╗ºa Lead
 */
function updateLeadStatus(leadId, { status, note = '', counselorId = null }) {
  const lead = findLeadById(leadId);
  if (!lead) {
    throw new Error('Kh├┤ng t├¼m thß║Ñy Lead');
  }

  const normalized = normalizeStatus(status);
  if (!normalized) {
    throw new Error('Trß║íng th├íi kh├┤ng hß╗úp lß╗ç');
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
    title: `Cß║¡p nhß║¡t trß║íng th├íi: ${newStatusLabel}`,
    content: `Chuyß╗ân trß║íng th├íi tß╗½ "${oldStatusLabel}" sang "${newStatusLabel}". ${note ? 'Ghi ch├║: ' + note : ''}`,
    author: user ? user.name : 'Hß╗ç thß╗æng',
  });

  return enrichLeadInfo(lead);
}

/**
 * Lß║Ñy thß╗æng k├¬ phß╗àu tuyß╗ân sinh (cho cards / dashboard)
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
 * X├│a trß║»ng v├á nß║íp lß║íi dß╗» liß╗çu mß║½u (d├╣ng cho test hoß║╖c khß╗ƒi ─æß╗Öng)
 */
function seedSampleLeads() {
  leads.length = 0;
  leadAssignments.length = 0;
  leadIdCounter = 1;
  assignmentIdCounter = 1;
  interactionIdCounter = 1;

  const now = new Date();

  // 1. Lead tß║ío tß╗½ th├íng tr╞░ß╗¢c (Phß╗Ñc vß╗Ñ S2-11: "t├¼m lß║íi ─æ╞░ß╗úc cuß╗Öc trao ─æß╗òi tß╗½ th├íng tr╞░ß╗¢c khi kh├ích gß╗ìi lß║íi")
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 15, 10, 30, 0);
  createLead({
    fullName: 'V┼⌐ Minh Anh',
    phone: '0988123456',
    email: 'minhanh.vu@gmail.com',
    course: 'Lß║¡p tr├¼nh Web Fullstack',
    source: LEAD_SOURCE.FACEBOOK,
    status: LEAD_STATUS.CONSULTING,
    notes: 'Quan t├óm lß╗Ö tr├¼nh tß╗½ c╞í bß║ún, hß╗Åi vß╗ü hß╗ìc ph├¡ trß║ú g├│p 0%',
    assignedCounselorId: '5', // L├¬ Thß╗ï Thu H├á
    createdAt: lastMonthDate.toISOString(),
  });

  // 2. Lead tuß║ºn tr╞░ß╗¢c
  const lastWeekDate = new Date(now);
  lastWeekDate.setDate(lastWeekDate.getDate() - 7);
  createLead({
    fullName: 'Ng├┤ Quß╗æc Bß║úo',
    phone: '0912345679',
    email: 'bao.ngo@hotmail.com',
    course: 'Java Spring Boot',
    source: LEAD_SOURCE.WEBSITE,
    status: LEAD_STATUS.TRIAL_SCHEDULED,
    notes: 'Hß║╣n test ─æß║ºu v├áo chiß╗üu thß╗⌐ 7 ph├▓ng P.102',
    assignedCounselorId: '5',
    createdAt: lastWeekDate.toISOString(),
  });

  // 3. Lead h├┤m nay - ch╞░a ph├ón c├┤ng
  createLead({
    fullName: 'B├╣i Thu H├á',
    phone: '0977889900',
    email: 'thuha.bui@gmail.com',
    course: 'Python & Data Analysis',
    source: LEAD_SOURCE.REFERRAL,
    status: LEAD_STATUS.NEW,
    notes: 'Bß║ín hß╗ìc giß╗¢i thiß╗çu, muß╗æn t╞░ vß║Ñn lß╗¢p hß╗ìc ca tß╗æi',
    assignedCounselorId: null, // Ch╞░a ph├ón c├┤ng -> D├╣ng test S2-10
    createdAt: now.toISOString(),
  });

  // 4. Lead h├┤m nay - ch╞░a ph├ón c├┤ng
  createLead({
    fullName: '─Éß║╖ng Ho├áng Nam',
    phone: '0933221100',
    email: 'nam.dang@gmail.com',
    course: 'Thiß║┐t kß║┐ UI/UX',
    source: LEAD_SOURCE.TIKTOK,
    status: LEAD_STATUS.NEW,
    notes: '─Éiß╗ün form tß╗½ link bio TikTok',
    assignedCounselorId: null, // Ch╞░a ph├ón c├┤ng -> D├╣ng test S2-10
    createdAt: now.toISOString(),
  });

  // 5. Lead ─æ├ú chß╗æt ─æ─âng k├╜ th├íng tr╞░ß╗¢c cß╗ºa t╞░ vß║Ñn vi├¬n 6
  createLead({
    fullName: 'Trß║ºn Minh ─Éß╗⌐c',
    phone: '0903456789',
    email: 'duc.tran@yahoo.com',
    course: 'Lß║¡p tr├¼nh Frontend React',
    source: LEAD_SOURCE.HOTLINE,
    status: LEAD_STATUS.WON,
    notes: '─É├ú ─æ├│ng hß╗ìc ph├¡ ─æß║ºy ─æß╗º',
    assignedCounselorId: '6', // Ho├áng V─ân T╞░
    createdAt: lastMonthDate.toISOString(),
  });
}

// Khß╗ƒi tß║ío sß║╡n dß╗» liß╗çu demo ban ─æß║ºu
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
