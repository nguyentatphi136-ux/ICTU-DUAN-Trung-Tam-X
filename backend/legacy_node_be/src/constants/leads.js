const LEAD_STATUS = Object.freeze({
  NEW: 'NEW',
  CONTACTED: 'CONTACTED',
  CONSULTING: 'CONSULTING',
  TRIAL_SCHEDULED: 'TRIAL_SCHEDULED',
  WON: 'WON',
  REJECTED: 'REJECTED',
});

const LEAD_STATUS_LABELS = Object.freeze({
  [LEAD_STATUS.NEW]: 'Mới',
  [LEAD_STATUS.CONTACTED]: 'Đã liên hệ',
  [LEAD_STATUS.CONSULTING]: 'Đang tư vấn',
  [LEAD_STATUS.TRIAL_SCHEDULED]: 'Hẹn học thử',
  [LEAD_STATUS.WON]: 'Đã chuyển đổi',
  [LEAD_STATUS.REJECTED]: 'Không tiềm năng / Từ chối',
});

const LEAD_SOURCE = Object.freeze({
  WEBSITE: 'WEBSITE',
  FACEBOOK: 'FACEBOOK',
  REFERRAL: 'REFERRAL',
  HOTLINE: 'HOTLINE',
  EVENT: 'EVENT',
  TIKTOK: 'TIKTOK',
});

const LEAD_SOURCE_LABELS = Object.freeze({
  [LEAD_SOURCE.WEBSITE]: 'Website Landing',
  [LEAD_SOURCE.FACEBOOK]: 'Facebook Ads',
  [LEAD_SOURCE.REFERRAL]: 'Người quen giới thiệu',
  [LEAD_SOURCE.HOTLINE]: 'Hotline tư vấn',
  [LEAD_SOURCE.EVENT]: 'Sự kiện Offline',
  [LEAD_SOURCE.TIKTOK]: 'Kênh TikTok',
});

// Bản đồ ánh xạ các từ khoá trạng thái từ Frontend prototype sang chuẩn Backend
const STATUS_ALIAS_MAP = Object.freeze({
  new: LEAD_STATUS.NEW,
  mới: LEAD_STATUS.NEW,
  contacted: LEAD_STATUS.CONTACTED,
  'đã liên hệ': LEAD_STATUS.CONTACTED,
  consulting: LEAD_STATUS.CONSULTING,
  counseling: LEAD_STATUS.CONSULTING,
  'đang tư vấn': LEAD_STATUS.CONSULTING,
  'đang chăm sóc': LEAD_STATUS.CONSULTING,
  trial_scheduled: LEAD_STATUS.TRIAL_SCHEDULED,
  'hẹn test đầu vào': LEAD_STATUS.TRIAL_SCHEDULED,
  'hẹn học thử': LEAD_STATUS.TRIAL_SCHEDULED,
  won: LEAD_STATUS.WON,
  registered: LEAD_STATUS.WON,
  converted: LEAD_STATUS.WON,
  'đã đăng ký': LEAD_STATUS.WON,
  'đã nhập học': LEAD_STATUS.WON,
  chốt: LEAD_STATUS.WON,
  rejected: LEAD_STATUS.REJECTED,
  unreachable: LEAD_STATUS.REJECTED,
  'not-interested': LEAD_STATUS.REJECTED,
  'không tiềm năng': LEAD_STATUS.REJECTED,
  'từ chối': LEAD_STATUS.REJECTED,
});

// S3-01: 6 cột trạng thái của phễu Kanban
const FUNNEL_STAGES = Object.freeze([
  'Mới',
  'Đã liên hệ',
  'Đang tư vấn',
  'Hẹn học thử',
  'Chốt',
  'Từ chối',
]);

// S3-01: Danh mục lý do từ chối chuẩn bắt buộc khi thả vào cột Từ chối
const REJECT_REASONS = Object.freeze([
  'Học phí cao',
  'Lịch học không phù hợp',
  'Đã chọn trung tâm khác',
  'Không liên lạc được',
  'Chưa có nhu cầu lúc này',
  'Lý do khác',
]);

const STAGE_TO_STATUS = Object.freeze({
  'Mới': LEAD_STATUS.NEW,
  'Đã liên hệ': LEAD_STATUS.CONTACTED,
  'Đang tư vấn': LEAD_STATUS.CONSULTING,
  'Hẹn học thử': LEAD_STATUS.TRIAL_SCHEDULED,
  'Chốt': LEAD_STATUS.WON,
  'Từ chối': LEAD_STATUS.REJECTED,
});

const STATUS_TO_STAGE = Object.freeze({
  [LEAD_STATUS.NEW]: 'Mới',
  [LEAD_STATUS.CONTACTED]: 'Đã liên hệ',
  [LEAD_STATUS.CONSULTING]: 'Đang tư vấn',
  [LEAD_STATUS.TRIAL_SCHEDULED]: 'Hẹn học thử',
  [LEAD_STATUS.WON]: 'Chốt',
  [LEAD_STATUS.REJECTED]: 'Từ chối',
});

function normalizeStatus(status) {
  if (!status || typeof status !== 'string') return null;
  const upper = status.trim().toUpperCase();
  if (Object.values(LEAD_STATUS).includes(upper)) {
    return upper;
  }
  const lower = status.trim().toLowerCase();
  return STATUS_ALIAS_MAP[lower] || null;
}

function normalizeStage(stageOrStatus) {
  if (!stageOrStatus || typeof stageOrStatus !== 'string') return null;
  const trimmed = stageOrStatus.trim();
  if (FUNNEL_STAGES.includes(trimmed)) {
    return trimmed;
  }
  const status = normalizeStatus(trimmed);
  if (status && STATUS_TO_STAGE[status]) {
    return STATUS_TO_STAGE[status];
  }
  return null;
}

function isValidLeadStatus(status) {
  return normalizeStatus(status) !== null;
}

function isValidStage(stage) {
  return normalizeStage(stage) !== null;
}

function isValidRejectReason(reason) {
  if (!reason || typeof reason !== 'string') return false;
  return REJECT_REASONS.includes(reason.trim()) || reason.trim().length > 0;
}

function normalizeSource(source) {
  if (!source || typeof source !== 'string') return LEAD_SOURCE.WEBSITE;
  const upper = source.trim().toUpperCase();
  if (Object.values(LEAD_SOURCE).includes(upper)) {
    return upper;
  }
  const lower = source.trim().toLowerCase();
  if (lower.includes('fb') || lower.includes('facebook')) return LEAD_SOURCE.FACEBOOK;
  if (lower.includes('web') || lower.includes('landing')) return LEAD_SOURCE.WEBSITE;
  if (lower.includes('referral') || lower.includes('giới thiệu')) return LEAD_SOURCE.REFERRAL;
  if (lower.includes('hotline') || lower.includes('gọi')) return LEAD_SOURCE.HOTLINE;
  if (lower.includes('event') || lower.includes('sự kiện')) return LEAD_SOURCE.EVENT;
  if (lower.includes('tiktok')) return LEAD_SOURCE.TIKTOK;
  return upper;
}

module.exports = {
  FUNNEL_STAGES,
  LEAD_SOURCE,
  LEAD_SOURCE_LABELS,
  LEAD_STATUS,
  LEAD_STATUS_LABELS,
  REJECT_REASONS,
  STAGE_TO_STATUS,
  STATUS_ALIAS_MAP,
  STATUS_TO_STAGE,
  isValidLeadStatus,
  isValidRejectReason,
  isValidStage,
  normalizeSource,
  normalizeStage,
  normalizeStatus,
};

