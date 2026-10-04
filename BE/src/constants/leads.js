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
  [LEAD_STATUS.WON]: 'Chốt',
  [LEAD_STATUS.REJECTED]: 'Từ chối',
});

const LEAD_SOURCE = Object.freeze({
  WEBSITE: 'WEBSITE',
  FACEBOOK: 'FACEBOOK',
  REFERRAL: 'REFERRAL',
  EVENT: 'EVENT',
});

function isValidLeadStatus(status) {
  return typeof status === 'string' && Object.values(LEAD_STATUS).includes(status);
}

module.exports = {
  LEAD_SOURCE,
  LEAD_STATUS,
  LEAD_STATUS_LABELS,
  isValidLeadStatus,
};
