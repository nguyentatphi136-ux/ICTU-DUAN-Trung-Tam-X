// Phễu lead cho S3-01 trở đi. Khi tích hợp: GET /admissions/funnel, PATCH /admissions/leads/:id/stage (kèm lý do khi Từ chối).
// Mảng FUNNEL được sửa trực tiếp để các trang dùng chung một nguồn khi chưa có API.
export type Stage = 'Mới' | 'Đã liên hệ' | 'Đang tư vấn' | 'Hẹn học thử' | 'Chốt' | 'Từ chối';

export type FunnelLead = {
  id: number;
  name: string;
  phone: string;
  email?: string;
  program: string;
  source: string;
  /** Tư vấn viên phụ trách. */
  owner: string;
  stage: Stage;
  /** Ngày hẹn gọi lại, yyyy-mm-dd. */
  callback?: string;
  /** Ngày học thử, yyyy-mm-dd. */
  trial?: string;
  /** Lý do khi ở cột Từ chối. */
  reason?: string;
};

/** Một dòng trong nhật ký chăm sóc (S3-02). at là yyyy-mm-ddThh:mm. */
export type LogKind = 'Gọi' | 'Nhắn' | 'Gặp' | 'Đổi trạng thái' | 'Phân công';
export type CareLog = { kind: LogKind; at: string; by: string; text: string };

export const STAGES: Stage[] = ['Mới', 'Đã liên hệ', 'Đang tư vấn', 'Hẹn học thử', 'Chốt', 'Từ chối'];

export const REJECT_REASONS = ['Học phí cao', 'Lịch học không phù hợp', 'Đã chọn trung tâm khác', 'Không liên lạc được', 'Chưa có nhu cầu lúc này', 'Lý do khác'];

/** Ngày "hôm nay" của dữ liệu mẫu. Khi có API: lấy ngày máy chủ. */
export const TODAY = '2026-10-06';

export const stageTone = (s: Stage): 'ok' | 'wait' | 'off' | 'info' | undefined =>
  s === 'Mới' ? 'info' : s === 'Đang tư vấn' || s === 'Hẹn học thử' ? 'wait' : s === 'Chốt' ? 'ok' : s === 'Từ chối' ? 'off' : undefined;

const days = (from: string, to: string) => Math.round((Date.parse(to) - Date.parse(from)) / 86400000);

/** Số ngày quá hạn gọi lại, 0 nếu chưa quá hạn. Lead đã chốt hoặc từ chối không tính. */
export function overdueDays(l: FunnelLead) {
  if (!l.callback || l.stage === 'Chốt' || l.stage === 'Từ chối') return 0;
  return Math.max(0, days(l.callback, TODAY));
}

export const ddmm = (iso: string) => iso.slice(8, 10) + '/' + iso.slice(5, 7);

type Seed = Omit<FunnelLead, 'id' | 'source' | 'owner'> & Partial<Pick<FunnelLead, 'source' | 'owner'>>;

const SEED: Seed[] = [
  { name: 'PHAM THI HANH', phone: '0912 480 227', program: 'Lập trình web Full-stack', stage: 'Mới', source: 'Biểu mẫu web' },
  { name: 'DO QUANG MINH', phone: '0987 115 630', program: 'Phân tích dữ liệu', stage: 'Mới', source: 'Facebook' },
  { name: 'LE NGOC ANH', phone: '0938 772 041', program: 'Thiết kế UI/UX', stage: 'Mới' },
  { name: 'NGUYEN HOAI THU', phone: '0916 204 559', program: 'Kiểm thử phần mềm', stage: 'Mới' },
  { name: 'TRINH VAN QUANG', phone: '0985 331 760', program: 'Lập trình Java Backend', stage: 'Mới' },
  { name: 'TRAN VAN HUNG', phone: '0903 664 518', program: 'Lập trình web Full-stack', stage: 'Đã liên hệ', callback: '2026-10-04', source: 'Giới thiệu' },
  { name: 'NGUYEN THU TRANG', phone: '0976 220 148', program: 'Kiểm thử phần mềm', stage: 'Đã liên hệ', callback: '2026-10-08' },
  { name: 'HOANG GIA BAO', phone: '0915 337 902', program: 'Phân tích dữ liệu', stage: 'Đã liên hệ', callback: '2026-10-09' },
  { name: 'DANG THU HUONG', phone: '0932 845 116', program: 'Thiết kế UI/UX', stage: 'Đã liên hệ', callback: '2026-10-13' },
  { name: 'MAI DUC THANG', phone: '0968 470 395', program: 'Lập trình web Full-stack', stage: 'Đã liên hệ' },
  { name: 'LY THANH VAN', phone: '0907 512 683', program: 'Phân tích dữ liệu', stage: 'Đã liên hệ' },
  { name: 'NGUYEN BAO CHAU', phone: '0965 209 384', program: 'Lập trình Java Backend', stage: 'Đang tư vấn', callback: '2026-10-05', source: 'Zalo' },
  { name: 'BUI DUC ANH', phone: '0944 861 357', program: 'Thiết kế UI/UX', stage: 'Đang tư vấn', callback: '2026-10-06', source: 'Giới thiệu' },
  { name: 'VU KHANH LINH', phone: '0982 660 715', program: 'Lập trình web Full-stack', stage: 'Đang tư vấn', callback: '2026-10-12' },
  { name: 'TA MINH CHAU', phone: '0913 778 024', program: 'Kiểm thử phần mềm', stage: 'Đang tư vấn', callback: '2026-10-14' },
  { name: 'PHAN QUOC BINH', phone: '0978 136 502', program: 'Lập trình Java Backend', stage: 'Đang tư vấn' },
  { name: 'DUONG THI NGA', phone: '0936 905 271', program: 'Phân tích dữ liệu', stage: 'Đang tư vấn' },
  { name: 'CAO VAN TOAN', phone: '0919 622 840', program: 'Lập trình web Full-stack', stage: 'Đang tư vấn' },
  { name: 'LE MINH KHOI', phone: '0909 518 264', program: 'Lập trình web Full-stack', stage: 'Hẹn học thử', trial: '2026-10-10' },
  { name: 'PHAM NGOC HAN', phone: '0933 704 186', program: 'Thiết kế UI/UX', stage: 'Hẹn học thử', callback: '2026-10-03' },
  { name: 'VO THANH NHAN', phone: '0962 381 457', program: 'Phân tích dữ liệu', stage: 'Hẹn học thử', trial: '2026-10-11' },
  { name: 'HA GIA LINH', phone: '0917 064 228', program: 'Kiểm thử phần mềm', stage: 'Hẹn học thử', trial: '2026-10-15' },
  { name: 'VU THANH DAT', phone: '0919 530 276', email: 'dat.vt@gmail.com', program: 'Kiểm thử phần mềm', stage: 'Chốt', source: 'Facebook' },
  { name: 'DO TUAN KIET', phone: '0971 846 320', program: 'Lập trình web Full-stack', stage: 'Chốt' },
  { name: 'HOANG MAI LAN', phone: '0977 418 902', program: 'Phân tích dữ liệu', stage: 'Từ chối', reason: 'Học phí cao', owner: 'DANG BAO VY' },
  { name: 'KIEU VAN SON', phone: '0988 257 613', program: 'Lập trình Java Backend', stage: 'Từ chối', reason: 'Đã chọn trung tâm khác' },
  { name: 'LUONG THI HOA', phone: '0925 640 389', program: 'Thiết kế UI/UX', stage: 'Từ chối', reason: 'Không liên lạc được' },
];

export const FUNNEL: FunnelLead[] = SEED.map((s, i) => ({ id: i + 1, source: 'Biểu mẫu web', owner: 'TRAN THU HA', ...s }));

const MANAGER = 'LE HOANG NAM';

// Nhật ký mẫu theo id lead. Lead nào chưa có thì chỉ có dòng phân công. Khi tích hợp: GET/POST /admissions/leads/:id/logs.
export const LOGS: Record<number, CareLog[]> = {
  12: [
    { kind: 'Gọi', at: '2026-09-29T14:30', by: 'TRAN THU HA', text: 'Khách quan tâm lớp tối, xin gửi lịch khai giảng tháng 10.' },
    { kind: 'Đổi trạng thái', at: '2026-09-29T14:32', by: 'TRAN THU HA', text: 'Đã liên hệ → Đang tư vấn' },
    { kind: 'Nhắn', at: '2026-09-26T09:05', by: 'TRAN THU HA', text: 'Gửi tin giới thiệu chương trình Java Backend qua Zalo.' },
    { kind: 'Đổi trạng thái', at: '2026-09-26T09:06', by: 'TRAN THU HA', text: 'Mới → Đã liên hệ' },
    { kind: 'Phân công', at: '2026-09-24T08:00', by: MANAGER, text: 'Giao cho TRAN THU HA' },
  ],
};

export function logsOf(l: FunnelLead): CareLog[] {
  const list = LOGS[l.id] ?? [{ kind: 'Phân công', at: '2026-09-24T08:00', by: MANAGER, text: 'Giao cho ' + l.owner }];
  return [...list].sort((a, b) => b.at.localeCompare(a.at));
}

export function addLog(l: FunnelLead, log: CareLog) {
  LOGS[l.id] = [log, ...logsOf(l)];
}

/** "29/09/2026 14:30" từ "2026-09-29T14:30". */
export const viDateTime = (at: string) => at.slice(0, 10).split('-').reverse().join('/') + (at.length > 10 ? ' ' + at.slice(11, 16) : '');
