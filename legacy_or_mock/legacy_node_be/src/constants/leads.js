const LEAD_STATUS = Object.freeze({
  NEW: 'NEW',
  CONTACTED: 'CONTACTED',
  CONSULTING: 'CONSULTING',
  TRIAL_SCHEDULED: 'TRIAL_SCHEDULED',
  WON: 'WON',
  REJECTED: 'REJECTED',
});

const LEAD_STATUS_LABELS = Object.freeze({
  [LEAD_STATUS.NEW]: 'Mß╗¢i',
  [LEAD_STATUS.CONTACTED]: '─É├ú li├¬n hß╗ç',
  [LEAD_STATUS.CONSULTING]: '─Éang t╞░ vß║Ñn',
  [LEAD_STATUS.TRIAL_SCHEDULED]: 'Hß║╣n hß╗ìc thß╗¡',
  [LEAD_STATUS.WON]: '─É├ú chuyß╗ân ─æß╗òi',
  [LEAD_STATUS.REJECTED]: 'Kh├┤ng tiß╗üm n─âng / Tß╗½ chß╗æi',
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
  [LEAD_SOURCE.REFERRAL]: 'Ng╞░ß╗¥i quen giß╗¢i thiß╗çu',
  [LEAD_SOURCE.HOTLINE]: 'Hotline t╞░ vß║Ñn',
  [LEAD_SOURCE.EVENT]: 'Sß╗▒ kiß╗çn Offline',
  [LEAD_SOURCE.TIKTOK]: 'K├¬nh TikTok',
});

// Bß║ún ─æß╗ô ├ính xß║í c├íc tß╗½ kho├í trß║íng th├íi tß╗½ Frontend prototype sang chuß║⌐n Backend
const STATUS_ALIAS_MAP = Object.freeze({
  new: LEAD_STATUS.NEW,
  mß╗¢i: LEAD_STATUS.NEW,
  contacted: LEAD_STATUS.CONTACTED,
  '─æ├ú li├¬n hß╗ç': LEAD_STATUS.CONTACTED,
  consulting: LEAD_STATUS.CONSULTING,
  counseling: LEAD_STATUS.CONSULTING,
  '─æang t╞░ vß║Ñn': LEAD_STATUS.CONSULTING,
  '─æang ch─âm s├│c': LEAD_STATUS.CONSULTING,
  trial_scheduled: LEAD_STATUS.TRIAL_SCHEDULED,
  'hß║╣n test ─æß║ºu v├áo': LEAD_STATUS.TRIAL_SCHEDULED,
  'hß║╣n hß╗ìc thß╗¡': LEAD_STATUS.TRIAL_SCHEDULED,
  won: LEAD_STATUS.WON,
  registered: LEAD_STATUS.WON,
  converted: LEAD_STATUS.WON,
  '─æ├ú ─æ─âng k├╜': LEAD_STATUS.WON,
  '─æ├ú nhß║¡p hß╗ìc': LEAD_STATUS.WON,
  rejected: LEAD_STATUS.REJECTED,
  unreachable: LEAD_STATUS.REJECTED,
  'not-interested': LEAD_STATUS.REJECTED,
  'kh├┤ng tiß╗üm n─âng': LEAD_STATUS.REJECTED,
  'tß╗½ chß╗æi': LEAD_STATUS.REJECTED,
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

function isValidLeadStatus(status) {
  return normalizeStatus(status) !== null;
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
  if (lower.includes('referral') || lower.includes('giß╗¢i thiß╗çu')) return LEAD_SOURCE.REFERRAL;
  if (lower.includes('hotline') || lower.includes('gß╗ìi')) return LEAD_SOURCE.HOTLINE;
  if (lower.includes('event') || lower.includes('sß╗▒ kiß╗çn')) return LEAD_SOURCE.EVENT;
  if (lower.includes('tiktok')) return LEAD_SOURCE.TIKTOK;
  return upper;
}

module.exports = {
  LEAD_SOURCE,
  LEAD_SOURCE_LABELS,
  LEAD_STATUS,
  LEAD_STATUS_LABELS,
  STATUS_ALIAS_MAP,
  isValidLeadStatus,
  normalizeSource,
  normalizeStatus,
};
