// Tài khoản mẫu cho S1-08, S1-09, S1-10, S2-01. Khi tích hợp: GET /admin/users?search=&role=&status=&page=.
export type Status = 'Hoạt động' | 'Chờ kích hoạt' | 'Đã khoá';
export type UserRow = {
  id: number;
  name: string;
  email: string;
  phone: string;
  /** Chỉ số trong ROLES. */
  roles: number[];
  status: Status;
  /** Lớp đang phụ trách, cần bàn giao khi khoá. */
  classes?: string[];
  lockReason?: string;
};

const SEED: Omit<UserRow, 'id'>[] = [
  { name: 'NGUYEN MINH ANH', email: 'S25200212101@tms.vn', phone: '0912 345 678', roles: [0], status: 'Hoạt động' },
  { name: 'TRAN THU HA', email: 'AMS25200212012@tms.vn', phone: '0987 654 321', roles: [3], status: 'Hoạt động' },
  { name: 'LE HOANG NAM', email: 'TM25200212002@tms.vn', phone: '0903 112 233', roles: [2, 5], status: 'Hoạt động' },
  { name: 'PHAM GIA HUY', email: 'TA25200212007@tms.vn', phone: '0938 221 144', roles: [1], status: 'Chờ kích hoạt' },
  { name: 'DO THI MAI', email: 'ACT25200212003@tms.vn', phone: '0977 889 900', roles: [4], status: 'Hoạt động' },
  {
    name: 'VU DUC LONG',
    email: 'IST25200212045@tms.vn',
    phone: '0909 456 123',
    roles: [2],
    status: 'Hoạt động',
    classes: ['WEB-K15 · Lập trình web cơ bản', 'DB-K14 · Cơ sở dữ liệu'],
  },
  { name: 'HOANG YEN NHI', email: 'S25200212102@tms.vn', phone: '0965 778 210', roles: [0], status: 'Hoạt động' },
  { name: 'BUI QUANG KHAI', email: 'S25200212103@tms.vn', phone: '0944 310 562', roles: [0], status: 'Hoạt động' },
  { name: 'TRAN QUOC BAO', email: 'A25200212001@tms.vn', phone: '0918 200 300', roles: [6], status: 'Hoạt động' },
];

const LAST = ['AN', 'BINH', 'CHI', 'DUNG', 'GIANG', 'HANH', 'KHANH', 'LINH', 'MINH', 'NGOC', 'PHUONG', 'QUAN', 'SON', 'THAO', 'TUAN', 'VY'];
const FIRST = ['NGUYEN', 'TRAN', 'LE', 'PHAM', 'HOANG', 'VU', 'DANG', 'BUI', 'DO', 'NGO'];
const MID = ['VAN', 'THI', 'MINH', 'THANH', 'HUU', 'NGOC'];

// Phần còn lại sinh tự động cho đủ 300 tài khoản như trong thiết kế (đa số là học viên, 4 tài khoản bị khoá).
export const USERS: UserRow[] = [...Array(300)].map((_, i) => {
  if (i < SEED.length) return { id: i + 1, ...SEED[i] };
  const n = 104 + i;
  return {
    id: i + 1,
    name: `${FIRST[i % FIRST.length]} ${MID[i % MID.length]} ${LAST[i % LAST.length]}`,
    email: `S25200212${n}@tms.vn`,
    phone: `09${String(10000000 + i * 7919).slice(-8).replace(/(\d{2})(\d{3})(\d{3})/, '$1 $2 $3')}`,
    roles: [0],
    status: i % 97 === 0 ? 'Đã khoá' : i % 41 === 0 ? 'Chờ kích hoạt' : 'Hoạt động',
  };
});

export const statusTone = (s: Status): 'ok' | 'off' | 'wait' => (s === 'Hoạt động' ? 'ok' : s === 'Đã khoá' ? 'off' : 'wait');
