// Lead mẫu cho S2-08 đến S2-11. Khi tích hợp: /admissions/leads (GET có bộ lọc, POST, PUT, POST /assign).
export type LeadStatus = 'Mới' | 'Đang chăm sóc' | 'Hẹn gọi lại' | 'Đã nhập học' | 'Không quan tâm';
export type Lead = {
  id: number;
  name: string;
  phone: string;
  email?: string;
  source: string;
  program: string;
  status: LeadStatus;
  /** Tên tư vấn viên phụ trách, rỗng là chưa phân công. */
  owner: string;
  createdAt: string; // yyyy-mm-dd
  note?: string;
};

export const LEAD_STATUSES: LeadStatus[] = ['Mới', 'Đang chăm sóc', 'Hẹn gọi lại', 'Đã nhập học', 'Không quan tâm'];
export const SOURCES = ['Biểu mẫu web', 'Facebook', 'Zalo', 'Giới thiệu'];
export const COUNSELORS = ['TRAN THU HA', 'NGO THANH TU', 'DANG BAO VY'];
export const CONSULTANTS = COUNSELORS;
export const LEAD_SOURCES = SOURCES;

export const leadTone = (s: LeadStatus): 'info' | 'wait' | 'ok' | undefined =>
  s === 'Mới' ? 'info' : s === 'Đang chăm sóc' || s === 'Hẹn gọi lại' ? 'wait' : s === 'Đã nhập học' ? 'ok' : undefined;

const SEED: Omit<Lead, 'id'>[] = [
  { name: 'PHAM THI HANH', phone: '0912 480 227', source: 'Biểu mẫu web', program: 'Lập trình web Full-stack', status: 'Mới', owner: '', createdAt: '2026-09-28' },
  { name: 'DO QUANG MINH', phone: '0987 115 630', source: 'Facebook', program: 'Phân tích dữ liệu', status: 'Mới', owner: '', createdAt: '2026-09-28' },
  { name: 'LE NGOC ANH', phone: '0938 772 041', source: 'Biểu mẫu web', program: 'Thiết kế UI/UX', status: 'Mới', owner: '', createdAt: '2026-09-27' },
  { name: 'TRAN VAN HUNG', phone: '0903 664 518', source: 'Giới thiệu', program: 'Lập trình web Full-stack', status: 'Đang chăm sóc', owner: 'TRAN THU HA', createdAt: '2026-09-25' },
  { name: 'NGUYEN BAO CHAU', phone: '0965 209 384', source: 'Zalo', program: 'Lập trình Java Backend', status: 'Hẹn gọi lại', owner: 'NGO THANH TU', createdAt: '2026-09-24' },
  { name: 'VU THANH DAT', phone: '0919 530 276', source: 'Facebook', program: 'Kiểm thử phần mềm', status: 'Đã nhập học', owner: 'TRAN THU HA', createdAt: '2026-09-20' },
  { name: 'HOANG MAI LAN', phone: '0977 418 902', source: 'Biểu mẫu web', program: 'Phân tích dữ liệu', status: 'Không quan tâm', owner: 'DANG BAO VY', createdAt: '2026-09-18' },
  { name: 'BUI DUC ANH', phone: '0944 861 357', source: 'Giới thiệu', program: 'Thiết kế UI/UX', status: 'Đang chăm sóc', owner: 'NGO THANH TU', createdAt: '2026-09-15' },
];

const NAMES = ['NGUYEN THU TRANG', 'TRAN MINH KHOA', 'LE THANH HUONG', 'PHAM DUC LONG', 'HOANG BAO NGOC', 'VU QUANG HUY', 'DANG THI MAI', 'BUI GIA HAN'];
const PROGRAMS = ['Lập trình web Full-stack', 'Phân tích dữ liệu', 'Thiết kế UI/UX', 'Lập trình Java Backend', 'Kiểm thử phần mềm'];

// Sinh thêm cho đủ 128 lead như thiết kế, trải đều tháng 8 và tháng 9/2026.
export const LEADS: Lead[] = [...Array(128)].map((_, i) => {
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
});

export const samePhone = (a: string, b: string) => a.replace(/\D/g, '') === b.replace(/\D/g, '');
export const viDate = (iso: string) => iso.split('-').reverse().join('/');
