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

export function samePhone(a?: string, b?: string) {
  if (!a || !b) return false;
  return a.replace(/\D/g, '') === b.replace(/\D/g, '');
}

export function viDate(iso: string) {
  return (iso || '').split('-').reverse().join('/');
}

export const leadTone = (s: LeadStatus): 'info' | 'wait' | 'ok' | undefined =>
  s === 'Mới' ? 'info' : s === 'Đã liên hệ' || s === 'Đang tư vấn' || s === 'Hẹn học thử' ? 'wait' : s === 'Chốt' || s === 'Đã nhập học' ? 'ok' : undefined;

const SEED: Omit<Lead, 'id'>[] = FUNNEL.map((f, i) => ({
  name: f.name,
  phone: f.phone,
  email: f.email,
  source: f.source || 'Biểu mẫu web',
  program: f.program,
  status: (f.student ? 'Đã nhập học' : f.stage) as LeadStatus,
  owner: f.owner || 'TRAN THU HA',
  createdAt: `2026-09-${String(Math.max(1, 28 - (i % 20))).padStart(2, '0')}`,
}));

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
}), true);

/**
 * Đồng bộ hai chiều giữa LEADS (Danh sách) và FUNNEL (Bảng phễu).
 * Bất cứ lead nào có ở một bên mà thiếu ở bên kia đều được tự động bổ sung.
 * Khớp chuẩn xác theo số điện thoại (samePhone) hoặc tên để tránh lệch ID.
 */
export function syncAllLeadsAndFunnel() {
  let changed = false;

  // 1. Đồng bộ và chuẩn hóa từ FUNNEL sang LEADS
  for (const f of FUNNEL) {
    let l = LEADS.find((x) => samePhone(x.phone, f.phone));
    if (!l && f.name) {
      l = LEADS.find((x) => x.name.trim().toLowerCase() === f.name.trim().toLowerCase());
    }
    if (!l) {
      l = LEADS.find((x) => x.id === f.id);
    }

    const targetStatus: LeadStatus = (f.student ? 'Đã nhập học' : f.stage) as LeadStatus;

    if (l) {
      if (l.id !== f.id) {
        const occupant = LEADS.find((x) => x.id === f.id && x !== l);
        if (occupant) {
          occupant.id = Math.max(...LEADS.map((x) => x.id), ...FUNNEL.map((x) => x.id)) + 1;
        }
        l.id = f.id;
        changed = true;
      }
      if (l.status !== targetStatus) {
        l.status = targetStatus;
        changed = true;
      }
      if (l.name !== f.name) {
        l.name = f.name;
        changed = true;
      }
      if (f.owner && l.owner !== f.owner) {
        l.owner = f.owner;
        changed = true;
      }
      if (f.program && l.program !== f.program) {
        l.program = f.program;
        changed = true;
      }
    } else {
      LEADS.push({
        id: f.id,
        name: f.name,
        phone: f.phone,
        email: f.email,
        source: f.source || 'Biểu mẫu web',
        program: f.program,
        status: targetStatus,
        owner: f.owner || 'TRAN THU HA',
        createdAt: TODAY,
      });
      changed = true;
    }
  }

  // 2. Đồng bộ từ LEADS sang FUNNEL
  // Duyệt từ cuối lên đầu để các lead mới nhất (ở đầu LEADS) được unshift lên đầu FUNNEL đúng thứ tự
  for (let i = LEADS.length - 1; i >= 0; i--) {
    const l = LEADS[i];
    let f = FUNNEL.find((x) => samePhone(x.phone, l.phone));
    if (!f && l.name) {
      f = FUNNEL.find((x) => x.name.trim().toLowerCase() === l.name.trim().toLowerCase());
    }
    if (!f) {
      f = FUNNEL.find((x) => x.id === l.id);
    }

    if (f) {
      if (f.id !== l.id) {
        f.id = l.id;
        changed = true;
      }
      if (l.owner && f.owner !== l.owner) {
        f.owner = l.owner;
        changed = true;
      }
    } else {
      FUNNEL.unshift({
        id: l.id,
        name: l.name,
        phone: l.phone,
        email: l.email,
        program: l.program,
        source: l.source || 'Biểu mẫu web',
        owner: l.owner || '',
        stage: (l.status === 'Đã nhập học' ? 'Chốt' : l.status) as Stage || 'Mới',
        student: l.status === 'Đã nhập học' ? 'HV' + l.id : undefined,
      });
      changed = true;
    }
  }

  if (changed) {
    saveAll();
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('tms:lead-updated'));
    }
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
    owner: data.owner ?? '',
  };

  const existingLeadIdx = LEADS.findIndex((l) => (data.id && l.id === data.id) || samePhone(l.phone, lead.phone));
  if (existingLeadIdx >= 0) {
    lead.id = LEADS[existingLeadIdx].id;
    LEADS.splice(existingLeadIdx, 1);
  }
  LEADS.unshift(lead);

  const funnelLead: FunnelLead = {
    id: lead.id,
    name: lead.name,
    phone: lead.phone,
    email: lead.email,
    program: lead.program,
    source: lead.source,
    owner: lead.owner || '',
    stage: (lead.status === 'Đã nhập học' ? 'Chốt' : lead.status) as Stage || 'Mới',
    student: lead.status === 'Đã nhập học' ? 'HV' + lead.id : undefined,
  };

  const existingIdx = FUNNEL.findIndex((f) => f.id === lead.id || samePhone(f.phone, lead.phone));
  if (existingIdx >= 0) {
    FUNNEL[existingIdx] = funnelLead;
  } else {
    FUNNEL.unshift(funnelLead);
  }

  saveAll();
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tms:lead-updated', { detail: { id: lead.id } }));
  }
  return lead;
}

/** Cập nhật thông tin lead ở cả hai bảng */
export function updateLead(data: Lead) {
  const idx = LEADS.findIndex((l) => l.id === data.id || samePhone(l.phone, data.phone));
  if (idx >= 0) {
    LEADS[idx] = { ...data };
  }
  const f = FUNNEL.find((x) => x.id === data.id || samePhone(x.phone, data.phone));
  if (f) {
    f.id = data.id;
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
      f.student = undefined;
    }
  }
  saveAll();
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tms:lead-updated', { detail: { id: data.id } }));
  }
}

/** Xoá lead khỏi cả hai bảng */
export function removeLead(id: number) {
  const target = LEADS.find((l) => l.id === id);
  const lIdx = LEADS.findIndex((l) => l.id === id);
  if (lIdx >= 0) LEADS.splice(lIdx, 1);
  const fIdx = FUNNEL.findIndex((f) => f.id === id || (target && samePhone(f.phone, target.phone)));
  if (fIdx >= 0) FUNNEL.splice(fIdx, 1);
  saveAll();
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tms:lead-updated', { detail: { id } }));
  }
}

/** Đồng bộ trạng thái từ Bảng phễu (Kanban) sang Danh sách lead */
export function syncFunnelStage(id: number, stage: Stage, reason?: string, studentCode?: string) {
  const f = FUNNEL.find((x) => x.id === id);
  if (f) {
    f.stage = stage;
    f.reason = reason;
    if (studentCode) f.student = studentCode;
    else if (stage !== 'Chốt') f.student = undefined;
  }
  // Tìm lead trong LEADS: ưu tiên số điện thoại của f nếu có, sau đó tìm theo id
  const l = LEADS.find((x) => (f && samePhone(x.phone, f.phone)) || x.id === id);
  if (l) {
    if (f && l.id !== f.id) l.id = f.id;
    l.status = studentCode ? 'Đã nhập học' : stage;
  }
  saveAll();
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent('tms:lead-updated', { detail: { id, stage } }));
  }
}
