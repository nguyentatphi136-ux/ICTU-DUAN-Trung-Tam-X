// Tài khoản mẫu cho S1-08, S1-09, S1-10, S2-01. Khi tích hợp: GET /admin/users?search=&role=&status=&page=.
import { persisted } from './store';
import { digits } from './validate';
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
  /** Thông tin chi tiết (S1-08): họ tên viết như người nhập, ngày sinh yyyy-mm-dd, quê quán, địa chỉ hiện tại. */
  displayName?: string;
  birth?: string;
  hometown?: string;
  address?: string;
  code?: string;
  gender?: string;
  createdAt?: string;
  tempPassword?: string;
  contactEmail?: string;
};

const SEED: Omit<UserRow, 'id'>[] = [
  {
    name: 'QUẢN TRỊ VIÊN',
    displayName: 'Trần Quốc Bảo',
    email: 'admin@tms.vn',
    phone: '0901 234 567',
    roles: [6],
    status: 'Hoạt động',
    code: 'AD202401',
    gender: 'Nam',
    birth: '1986-12-01',
    hometown: 'Đà Nẵng',
    address: 'P.1205 toà nhà Keangnam, Nam Từ Liêm, Hà Nội',
    contactEmail: 'bao.tq@tms.vn',
  },
  {
    name: 'QUẢN LÝ ĐÀO TẠO',
    displayName: 'Lê Hoàng Nam',
    email: 'daotao@tms.vn',
    phone: '0912 345 678',
    roles: [5],
    status: 'Hoạt động',
    code: 'QL202401',
    gender: 'Nam',
    birth: '1985-04-18',
    hometown: 'Hà Nội',
    address: 'Số 102 Giải Phóng, Đống Đa, Hà Nội',
    contactEmail: 'nam.lh@tms.vn',
    classes: ['WEB-K16 · Lập trình web Full-stack', 'DATA-K07 · Phân tích dữ liệu', 'DATA-K06 · Phân tích dữ liệu'],
  },
  {
    name: 'GIẢNG VIÊN VŨ ĐỨC LONG',
    displayName: 'Vũ Đức Long',
    email: 'giangvien@tms.vn',
    phone: '0909 456 123',
    roles: [2],
    status: 'Hoạt động',
    code: 'GV202401',
    gender: 'Nam',
    birth: '1988-10-15',
    hometown: 'Hà Nội',
    address: 'Số 45 ngõ 120 Hoàng Quốc Việt, Cầu Giấy, Hà Nội',
    contactEmail: 'longvd@ictu.edu.vn',
    classes: ['WEB-K15 · Lập trình web Full-stack', 'WEB-K16 · Lập trình web Full-stack', 'WEB-K14 · Lập trình web Full-stack'],
  },
  {
    name: 'TRỢ GIẢNG PHẠM GIA HUY',
    displayName: 'Phạm Gia Huy',
    email: 'trogiang@tms.vn',
    phone: '0938 221 144',
    roles: [1],
    status: 'Hoạt động',
    code: 'TA202401',
    gender: 'Nam',
    birth: '1998-05-12',
    hometown: 'Bắc Ninh',
    address: 'Số 18 đường Cầu Giấy, Hà Nội',
    contactEmail: 'huy.pg@tms.vn',
    classes: ['WEB-K15 · Lập trình web Full-stack', 'WEB-K16 · Lập trình web Full-stack'],
  },
  {
    name: 'TƯ VẤN TRẦN THU HÀ',
    displayName: 'Trần Thu Hà',
    email: 'tuvan@tms.vn',
    phone: '0987 654 321',
    roles: [3],
    status: 'Hoạt động',
    code: 'TV202401',
    gender: 'Nữ',
    birth: '1995-11-28',
    hometown: 'Hà Nội',
    address: 'Số 88 phố Huế, Hai Bà Trưng, Hà Nội',
    contactEmail: 'ha.tt@tms.vn',
  },
  {
    name: 'KẾ TOÁN ĐỖ THỊ MAI',
    displayName: 'Đỗ Thị Mai',
    email: 'ketoan@tms.vn',
    phone: '0977 889 900',
    roles: [4],
    status: 'Hoạt động',
    code: 'KT202401',
    gender: 'Nữ',
    birth: '1990-09-02',
    hometown: 'Hải Phòng',
    address: 'Số 25 phố Vọng, Hai Bà Trưng, Hà Nội',
    contactEmail: 'mai.dt@tms.vn',
  },
  {
    name: 'HỌC VIÊN NGUYỄN MINH ANH',
    displayName: 'Nguyễn Minh Anh',
    email: 'hocvien@tms.vn',
    phone: '0912 345 678',
    roles: [0],
    status: 'Hoạt động',
    code: 'HV260101',
    gender: 'Nữ',
    birth: '2004-03-15',
    hometown: 'Hà Nội',
    address: '12 Nguyễn Trãi, Thanh Xuân, Hà Nội',
    contactEmail: 'minhanh.ng@gmail.com',
    classes: ['WEB-K15 · Lập trình web Full-stack'],
  },
  {
    name: 'HỌC VIÊN ĐỖ TUẤN KIỆT',
    displayName: 'Đỗ Tuấn Kiệt',
    email: 'kiet.dt@gmail.com',
    phone: '0971 846 320',
    roles: [0],
    status: 'Hoạt động',
    code: 'HV260142',
    gender: 'Nam',
    birth: '2003-08-20',
    hometown: 'Thái Nguyên',
    address: 'Ký túc xá Đại học CNTT & TT, Thái Nguyên',
    contactEmail: 'kiet.dt@gmail.com',
    classes: ['WEB-K16 · Lập trình web Full-stack'],
  },
];

const LAST = ['AN', 'BINH', 'CHI', 'DUNG', 'GIANG', 'HANH', 'KHANH', 'LINH', 'MINH', 'NGOC', 'PHUONG', 'QUAN', 'SON', 'THAO', 'TUAN', 'VY'];
const FIRST = ['NGUYEN', 'TRAN', 'LE', 'PHAM', 'HOANG', 'VU', 'DANG', 'BUI', 'DO', 'NGO'];
const MID = ['VAN', 'THI', 'MINH', 'THANH', 'HUU', 'NGOC'];

// Phần còn lại sinh tự động cho đủ 300 tài khoản như trong thiết kế (đa số là học viên, 4 tài khoản bị khoá).
export const USERS: UserRow[] = persisted('users', [...Array(300)].map((_, i): UserRow => {
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
}), true);

/** Tài khoản khác (không phải exceptId) đang dùng số điện thoại này. Máy chủ kiểm lại khi lưu (409 PHONE_DUPLICATE). */
export const phoneOwner = (phone: string, exceptId?: number) =>
  digits(phone) ? USERS.find((u) => u.id !== exceptId && digits(u.phone) === digits(phone)) : undefined;

/** Thông tin hồ sơ ngoài danh sách tài khoản (S2-02), theo email. Họ tên và số điện thoại nằm trong USERS. */
export const PROFILES: Record<string, { birth: string; address: string }> = persisted('profiles', {});

export const statusTone = (s: Status): 'ok' | 'off' | 'wait' => (s === 'Hoạt động' ? 'ok' : s === 'Đã khoá' ? 'off' : 'wait');
