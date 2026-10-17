// Lead mẫu cho S2-08 đến S2-11 và đồng bộ trực tiếp với Bảng phễu (S3-01).
import { FUNNEL, STAGES, TODAY, type FunnelLead, type Stage } from './funnel';
import { persisted, saveAll } from './store';

/** Sáu giai đoạn của S3-01 (dùng chung với bảng phễu); 'Đã nhập học' là lead đã chuyển thành học viên (S3-03). */
export type LeadStatus = Stage | 'Đã nhập học';
export type Lead = {
  id: number;
  name: string;
  phone: string;
  email?: string;
  source: string;
  program: string;
  status: LeadStatus;
  /** Tên tư vấn viên phụ trách, rỗng là chưa phân công. ownerId chỉ có khi chạy với backend. */
  ownerId?: number;
  owner: string;
  createdAt: string; // yyyy-mm-dd
  note?: string;
};

export const LEAD_STATUSES: LeadStatus[] = [...STAGES];
export const SOURCES = ['Biểu mẫu web', 'Facebook', 'Zalo', 'Giới thiệu'];
export const COUNSELORS = ['TRAN THU HA', 'NGO THANH TU', 'DANG BAO VY'];

export const leadTone = (s: LeadStatus): 'info' | 'wait' | 'ok' | undefined =>
  s === 'Mới' ? 'info' : s === 'Đã liên hệ' || s === 'Đang tư vấn' || s === 'Hẹn học thử' ? 'wait' : s === 'Chốt' || s === 'Đã nhập học' ? 'ok' : undefined;

const SEED: Omit<Lead, 'id'>[] = [
  { name: 'PHAM THI HANH', phone: '0912 480 227', source: 'Biểu mẫu web', program: 'Lập trình web Full-stack', status: 'Mới', owner: 'TRAN THU HA', createdAt: '2026-09-28' },
  { name: 'DO QUANG MINH', phone: '0987 115 630', source: 'Facebook', program: 'Phân tích dữ liệu', status: 'Mới', owner: 'TRAN THU HA', createdAt: '2026-09-28' },
  { name: 'LE NGOC ANH', phone: '0938 772 041', source: 'Biểu mẫu web', program: 'Thiết kế UI/UX', status: 'Mới', owner: 'TRAN THU HA', createdAt: '2026-09-27' },
  { name: 'TRAN VAN HUNG', phone: '0903 664 518', source: 'Giới thiệu', program: 'Lập trình web Full-stack', status: 'Đang tư vấn', owner: 'TRAN THU HA', createdAt: '2026-09-25' },
  { name: 'NGUYEN BAO CHAU', phone: '0965 209 384', source: 'Zalo', program: 'Lập trình Java Backend', status: 'Đã liên hệ', owner: 'TRAN THU HA', createdAt: '2026-09-24' },
  { name: 'VU THANH DAT', phone: '0919 530 276', source: 'Facebook', program: 'Kiểm thử phần mềm', status: 'Đã nhập học', owner: 'TRAN THU HA', createdAt: '2026-09-20' },
  { name: 'HOANG MAI LAN', phone: '0977 418 902', source: 'Biểu mẫu web', program: 'Phân tích dữ liệu', status: 'Từ chối', owner: 'DANG BAO VY', createdAt: '2026-09-18' },
  { name: 'BUI DUC ANH', phone: '0944 861 357', source: 'Giới thiệu', program: 'Thiết kế UI/UX', status: 'Đang tư vấn', owner: 'TRAN THU HA', createdAt: '2026-09-15' },
];

const NAMES = ['NGUYEN THU TRANG', 'TRAN MINH KHOA', 'LE THANH HUONG', 'PHAM DUC LONG', 'HOANG BAO NGOC', 'VU QUANG HUY', 'DANG THI MAI', 'BUI GIA HAN'];
const PROGRAMS = ['Lập trình web Full-stack', 'Phân tích dữ liệu', 'Thiết kế UI/UX', 'Lập trình Java Backend', 'Kiểm thử phần mềm'];

// Sinh thêm cho đủ 128 lead như thiết kế, trải đều tháng 8 và tháng 9/2026.
export const LEADS: Lead[] = persisted('leads.v2', [...Array(128)].map((_, i): Lead => {
  if (i < SEED.length) return { id: i + 1, ...SEED[i] };
  const day = 1 + ((i * 7) % 28);
  return {
    id: i + 1,
    name: NAMES[i % NAMES.length],
    phone: `09${String(30000000 + i * 104729).slice(-8).replace(/(\d{2})(\d{3})(\d{3})/, '$1 $2 $3')}`,
    source: SOURCES[i % SOURCES.length],
    program: PROGRAMS[i % PROGRAMS.length],
    status: LEAD_STATUSES[(i % 4) + 1],
    owner: COUNSELORS[i % COUNSELORS.length],
    createdAt: `2026-${i % 2 ? '08' : '09'}-${String(day).padStart(2, '0')}`,
  };
}));

/**
 * Đồng bộ hai chiều giữa LEADS (Danh sách) và FUNNEL (Bảng phễu).
 * Bất cứ lead nào có ở một bên mà thiếu ở bên kia đều được tự động bổ sung.
 */
export function syncAllLeadsAndFunnel() {
  let changed = false;

  // 1. Đồng bộ từ LEADS sang FUNNEL
  for (const l of LEADS) {
    const found = FUNNEL.find((f) => f.id === l.id);
    if (!found) {
      FUNNEL.push({
        id: l.id,
        name: l.name,
        phone: l.phone,
        email: l.email,
        program: l.program,
        source: l.source || 'Biểu mẫu web',
        owner: l.owner || 'TRAN THU HA',
        stage: (l.status === 'Đã nhập học' ? 'Chốt' : l.status) as Stage || 'Mới',
        student: l.status === 'Đã nhập học' ? 'HV' + l.id : undefined,
      });
      changed = true;
    }
  }

  // 2. Đồng bộ từ FUNNEL sang LEADS
  for (const f of FUNNEL) {
    const found = LEADS.find((l) => l.id === f.id);
    if (!found) {
      LEADS.push({
        id: f.id,
        name: f.name,
        phone: f.phone,
        email: f.email,
        source: f.source || 'Biểu mẫu web',
        program: f.program,
        status: f.student ? 'Đã nhập học' : f.stage,
        owner: f.owner || 'TRAN THU HA',
        createdAt: TODAY,
      });
      changed = true;
    }
  }

  if (changed) {
    saveAll();
  }
}

// Chạy đồng bộ ngay khi nạp module
syncAllLeadsAndFunnel();

/** Thêm một lead mới: tự động đồng bộ vào cả LEADS và FUNNEL */
export function addLead(data: Omit<Lead, 'id'> & { id?: number }): Lead {
  const newId = data.id && data.id > 0
    ? data.id
    : Math.max(0, ...LEADS.map((x) => x.id), ...FUNNEL.map((x) => x.id)) + 1;
  const lead: Lead = {
    ...data,
    id: newId,
    createdAt: data.createdAt || TODAY,
    source: data.source || 'Biểu mẫu web',
    status: data.status || 'Mới',
  };
  LEADS.unshift(lead);

  const funnelLead: FunnelLead = {
    id: newId,
    name: lead.name,
    phone: lead.phone,
    email: lead.email,
    program: lead.program,
    source: lead.source,
    owner: lead.owner || 'TRAN THU HA',
    stage: (lead.status === 'Đã nhập học' ? 'Chốt' : lead.status) as Stage || 'Mới',
    student: lead.status === 'Đã nhập học' ? 'HV' + newId : undefined,
  };

  const existingIdx = FUNNEL.findIndex((f) => f.id === newId);
  if (existingIdx >= 0) {
    FUNNEL[existingIdx] = funnelLead;
  } else {
    FUNNEL.unshift(funnelLead);
  }

  saveAll();
  return lead;
}

/** Cập nhật thông tin lead ở cả hai bảng */
export function updateLead(data: Lead) {
  const idx = LEADS.findIndex((l) => l.id === data.id);
  if (idx >= 0) {
    LEADS[idx] = { ...data };
  }
  const f = FUNNEL.find((x) => x.id === data.id);
  if (f) {
    f.name = data.name;
    f.phone = data.phone;
    f.email = data.email;
    f.program = data.program;
    f.source = data.source;
    f.owner = data.owner;
    if (data.status === 'Đã nhập học') {
      f.stage = 'Chốt';
      f.student = f.student || ('HV' + data.id);
    } else {
      f.stage = data.status as Stage;
    }
  }
  saveAll();
}

/** Xoá lead khỏi cả hai bảng */
export function removeLead(id: number) {
  const lIdx = LEADS.findIndex((l) => l.id === id);
  if (lIdx >= 0) LEADS.splice(lIdx, 1);
  const fIdx = FUNNEL.findIndex((f) => f.id === id);
  if (fIdx >= 0) FUNNEL.splice(fIdx, 1);
  saveAll();
}

/** Đồng bộ trạng thái từ Bảng phễu (Kanban) sang Danh sách lead */
export function syncFunnelStage(id: number, stage: Stage, reason?: string, studentCode?: string) {
  const f = FUNNEL.find((x) => x.id === id);
  if (f) {
    f.stage = stage;
    f.reason = reason;
    if (studentCode) f.student = studentCode;
  }
  const l = LEADS.find((x) => x.id === id);
  if (l) {
    l.status = studentCode ? 'Đã nhập học' : stage;
  }
  saveAll();
}

export const samePhone = (a: string, b: string) => a.replace(/\D/g, '') === b.replace(/\D/g, '');
export const viDate = (iso: string) => iso.split('-').reverse().join('/');
