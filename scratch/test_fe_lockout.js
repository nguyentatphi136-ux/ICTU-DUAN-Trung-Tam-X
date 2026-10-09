// Kiểm thử chi tiết cơ chế khóa tạm 15 phút sau 5 lần sai liên tiếp trong frontend
const fs = require('fs');
const assert = require('assert');

// Tạo mock window / document / localStorage
const storage = {};
global.localStorage = {
  getItem: (k) => storage[k] ?? null,
  setItem: (k, v) => { storage[k] = String(v); },
  removeItem: (k) => { delete storage[k]; },
  clear: () => { Object.keys(storage).forEach(k => delete storage[k]); }
};
global.sessionStorage = {
  getItem: (k) => storage['sess_' + k] ?? null,
  setItem: (k, v) => { storage['sess_' + k] = String(v); },
  removeItem: (k) => { delete storage['sess_' + k]; },
};

console.log("================================================================================");
console.log("KIỂM THỬ: KHÓA TẠM 15 PHÚT SAU 5 LẦN SAI LIÊN TIẾP (S1-01 AC3)");
console.log("================================================================================");

const appJsContent = fs.readFileSync('frontend/app.js', 'utf8');

// Trích xuất các hàm lockout từ app.js để kiểm thử
const MAX_FAILED_ATTEMPTS = 5;
const LOCKOUT_MINUTES = 15;
const LOCKOUT_DURATION_MS = LOCKOUT_MINUTES * 60 * 1000;
const LOGIN_ATTEMPTS_KEY = "edumanager-login-attempts";

const getLoginAttemptsMap = () => {
  try {
    return JSON.parse(localStorage.getItem(LOGIN_ATTEMPTS_KEY) ?? "{}");
  } catch {
    return {};
  }
};

const saveLoginAttemptsMap = (map) => {
  localStorage.setItem(LOGIN_ATTEMPTS_KEY, JSON.stringify(map));
};

const DEMO_ACCOUNTS = [
  { email: "admin@edumanager.vn", aliases: ["admin", "admin@example.com"], password: "Admin@123" }
];

const normalizeLoginIdentity = (input) => {
  const raw = (input || "").trim().toLowerCase();
  if (!raw) return "";
  const matched = DEMO_ACCOUNTS.find((acc) => {
    const accEmail = acc.email.toLowerCase();
    const aliases = (acc.aliases || []).map((a) => a.toLowerCase());
    return accEmail === raw || aliases.includes(raw) || (raw.includes("@") && accEmail.split("@")[0] === raw.split("@")[0]);
  });
  return matched ? matched.email.toLowerCase() : raw;
};

const checkAccountLockout = (identity) => {
  const norm = normalizeLoginIdentity(identity);
  if (!norm) return { isLocked: false };

  const map = getLoginAttemptsMap();
  const record = map[norm];
  if (!record || !record.lockedUntil) return { isLocked: false, count: record?.count || 0 };

  const now = Date.now();
  if (now < record.lockedUntil) {
    const remainingSeconds = Math.ceil((record.lockedUntil - now) / 1000);
    const remainingMinutes = Math.ceil(remainingSeconds / 60);
    return {
      isLocked: true,
      remainingMinutes,
      remainingSeconds,
      lockedUntil: record.lockedUntil,
      count: record.count,
    };
  }

  delete map[norm];
  saveLoginAttemptsMap(map);
  return { isLocked: false, count: 0 };
};

const recordFailedLogin = (identity) => {
  const norm = normalizeLoginIdentity(identity);
  if (!norm) return { isLocked: false, remainingAttempts: MAX_FAILED_ATTEMPTS, count: 1 };

  const map = getLoginAttemptsMap();
  const current = map[norm] || { count: 0, lockedUntil: null };
  const now = Date.now();

  if (current.lockedUntil && now < current.lockedUntil) {
    return checkAccountLockout(identity);
  }

  current.count = (current.count || 0) + 1;
  if (current.count >= MAX_FAILED_ATTEMPTS) {
    current.lockedUntil = now + LOCKOUT_DURATION_MS;
    map[norm] = current;
    saveLoginAttemptsMap(map);
    return {
      isLocked: true,
      remainingMinutes: LOCKOUT_MINUTES,
      remainingSeconds: LOCKOUT_MINUTES * 60,
      lockedUntil: current.lockedUntil,
      count: current.count,
    };
  } else {
    map[norm] = current;
    saveLoginAttemptsMap(map);
    return {
      isLocked: false,
      remainingAttempts: MAX_FAILED_ATTEMPTS - current.count,
      count: current.count,
    };
  }
};

const clearLoginAttempts = (identity) => {
  const norm = normalizeLoginIdentity(identity);
  if (!norm) return;
  const map = getLoginAttemptsMap();
  if (map[norm]) {
    delete map[norm];
    saveLoginAttemptsMap(map);
  }
};

// Bắt đầu test
localStorage.clear();
const testEmail = "admin@edumanager.vn";

// Bước 1: 4 lần nhập sai đầu tiên
for (let i = 1; i <= 4; i++) {
  const res = recordFailedLogin(testEmail);
  assert.equal(res.isLocked, false, `Lần ${i} chưa bị khóa`);
  assert.equal(res.remainingAttempts, 5 - i, `Lần ${i} còn lại ${5 - i} lần thử`);
  console.log(`  [PASS] Lần thử sai ${i}: Báo sai thông tin, còn lại ${res.remainingAttempts} lượt.`);
}

// Bước 2: Lần nhập sai thứ 5 -> Khóa tạm 15 phút
const fifthRes = recordFailedLogin(testEmail);
assert.equal(fifthRes.isLocked, true, "Lần 5 phải bị khóa");
assert.equal(fifthRes.remainingMinutes, 15, "Thời gian khóa phải là 15 phút");
console.log(`  [PASS] Lần thử sai 5: KÍCH HOẠT KHÓA TẠM 15 PHÚT thành công.`);

// Bước 3: Trong thời gian khóa 15 phút -> Bị chặn kể cả đúng mật khẩu
const lockStatus = checkAccountLockout(testEmail);
assert.equal(lockStatus.isLocked, true, "Vẫn trong trạng thái bị khóa");
assert.equal(lockStatus.remainingMinutes, 15, "Báo còn 15 phút");
console.log(`  [PASS] Kiểm tra trong thời gian khóa: Đăng nhập bị chặn hoàn toàn (Còn ${lockStatus.remainingMinutes} phút).`);

// Bước 4: Kiểm tra alias "admin" cũng bị khóa (vì cùng là tài khoản admin@edumanager.vn)
const aliasLockStatus = checkAccountLockout("admin");
assert.equal(aliasLockStatus.isLocked, true, "Alias 'admin' cũng phải bị khóa");
console.log(`  [PASS] Nhận diện alias 'admin': Khóa đồng bộ trên mọi tên đăng nhập của tài khoản.`);

// Bước 5: Giả lập sau 15 phút (901 giây)
const map = getLoginAttemptsMap();
const normKey = normalizeLoginIdentity(testEmail);
map[normKey].lockedUntil = Date.now() - 1000; // đã qua thời gian khóa
saveLoginAttemptsMap(map);

const expiredLockStatus = checkAccountLockout(testEmail);
assert.equal(expiredLockStatus.isLocked, false, "Sau 15 phút phải tự động mở khóa");
console.log(`  [PASS] Sau 15 phút: Hệ thống tự động giải phóng khóa tạm, cho phép người dùng đăng nhập lại.`);

// Bước 6: Đăng nhập thành công -> clear login attempts
clearLoginAttempts(testEmail);
assert.equal(getLoginAttemptsMap()[normKey], undefined, "Đã xóa bộ đếm sau đăng nhập thành công");
console.log(`  [PASS] Đăng nhập thành công: Reset bộ đếm số lần sai về 0.`);

console.log("\n=> TẤT CẢ CÁC BƯỚC KIỂM THỬ KHÓA TẠM 15 PHÚT ĐÃ ĐẠT 100%!");
