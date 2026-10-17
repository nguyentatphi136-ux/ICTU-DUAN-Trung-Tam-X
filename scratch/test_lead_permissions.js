function normalizeName(str) {
  return (str || '')
    .replace(/[đĐ]/g, 'd')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');
}

function isUserMatch(target, u) {
  if (!target || !u) return false;
  const tNorm = normalizeName(target);
  const uNorm = normalizeName(u.name || '');
  const uEmail = (u.email || '').toLowerCase().trim();
  const tEmail = target.toLowerCase().trim();

  if (target === u.name || tEmail === uEmail) return true;

  if (tNorm && uNorm) {
    if (tNorm === uNorm || uNorm.includes(tNorm) || tNorm.includes(uNorm)) return true;
  }

  if (uEmail === 'tuvan@tms.vn' && tNorm.includes('tranthuha')) return true;

  return false;
}

function isLeadAssignedToUser(lead, user) {
  if (!lead || !user) return false;
  if (lead.owner && isUserMatch(lead.owner, user)) return true;
  if (lead.logs) {
    for (const log of lead.logs) {
      if (log.by && isUserMatch(log.by, user)) return true;
    }
  }
  return false;
}

const lead1 = { id: 1, name: 'Lead 1', owner: 'TRAN THU HA', callback: '2026-10-04' };
const lead2 = { id: 2, name: 'Lead 2', owner: 'DANG BAO VY', callback: '2026-10-04' };

const userHa = { name: 'TƯ VẤN TRẦN THU HÀ', email: 'tuvan@tms.vn' };
const userVy = { name: 'TƯ VẤN ĐẶNG BẢO VY', email: 'vy@tms.vn' };
const userAdmin = { name: 'QUẢN TRỊ VIÊN', email: 'admin@tms.vn' };
const userTeacher = { name: 'GIẢNG VIÊN VŨ ĐỨC LONG', email: 'giangvien@tms.vn' };

console.log('Lead 1 (Trần Thu Hà) -> user Hà:', isLeadAssignedToUser(lead1, userHa)); // Expected: true
console.log('Lead 1 (Trần Thu Hà) -> user Vy:', isLeadAssignedToUser(lead1, userVy)); // Expected: false
console.log('Lead 1 (Trần Thu Hà) -> user Admin:', isLeadAssignedToUser(lead1, userAdmin)); // Expected: false
console.log('Lead 1 (Trần Thu Hà) -> user Giảng viên:', isLeadAssignedToUser(lead1, userTeacher)); // Expected: false

console.log('Lead 2 (Đặng Bảo Vy) -> user Vy:', isLeadAssignedToUser(lead2, userVy)); // Expected: true
console.log('Lead 2 (Đặng Bảo Vy) -> user Hà:', isLeadAssignedToUser(lead2, userHa)); // Expected: false
