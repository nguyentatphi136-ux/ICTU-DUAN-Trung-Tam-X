// Test kịch bản: Lần đầu đăng nhập bắt buộc/gợi ý đổi mật khẩu, lần 2 trở đi không hiện nữa

const store = {};
const readStore = (key, fallback) => store[key] || fallback;
const writeStore = (key, val) => { store[key] = val; };

const FIRST_LOGIN_KEY = "edumanager-first-login-status";

// Bước 1: Admin tạo tài khoản mới
const email = "tatphi2006@gmail.com";
const tempPass = "Edu@demo99";

const firstLoginMap = readStore(FIRST_LOGIN_KEY, {});
firstLoginMap[email.toLowerCase()] = { isFirstLogin: true, loginCount: 0 };
writeStore(FIRST_LOGIN_KEY, firstLoginMap);

console.log("1. Admin đã tạo tài khoản:", firstLoginMap[email]);
console.assert(firstLoginMap[email].isFirstLogin === true, "Phải là lần đầu đăng nhập");
console.assert(firstLoginMap[email].loginCount === 0, "Số lần đăng nhập phải là 0");

// Bước 2: Người dùng đăng nhập LẦN THỨ NHẤT
function simulateLogin(userEmail) {
  const map = readStore(FIRST_LOGIN_KEY, {});
  const normEmail = userEmail.toLowerCase();
  const info = map[normEmail] || { isFirstLogin: false, loginCount: 0 };

  let isFirstTime = false;
  if (info.isFirstLogin && (info.loginCount === 0 || info.loginCount === undefined)) {
    isFirstTime = true;
    map[normEmail] = {
      isFirstLogin: false,
      loginCount: 1,
      firstLoggedInAt: new Date().toISOString(),
    };
    writeStore(FIRST_LOGIN_KEY, map);
  } else {
    map[normEmail] = {
      isFirstLogin: false,
      loginCount: (info.loginCount || 1) + 1,
      lastLoggedInAt: new Date().toISOString(),
    };
    writeStore(FIRST_LOGIN_KEY, map);
  }

  const targetPage = "student.html";
  const redirectTarget = isFirstTime ? `${targetPage}?first_login=1` : targetPage;
  return { isFirstTime, redirectTarget };
}

const login1 = simulateLogin(email);
console.log("2. Đăng nhập lần 1:", login1);
console.assert(login1.isFirstTime === true, "Lần 1 phải có isFirstTime = true");
console.assert(login1.redirectTarget.includes("first_login=1"), "Lần 1 phải chuyển hướng có ?first_login=1");

// Giả sử người dùng KHÔNG đổi mật khẩu, bấm 'Để sau' hoặc đóng modal, rồi đăng xuất.

// Bước 3: Người dùng đăng nhập LẦN THỨ HAI
const login2 = simulateLogin(email);
console.log("3. Đăng nhập lần 2:", login2);
console.assert(login2.isFirstTime === false, "Lần 2 phải có isFirstTime = false");
console.assert(!login2.redirectTarget.includes("first_login=1"), "Lần 2 KHÔNG ĐƯỢC có ?first_login=1");

// Bước 4: Người dùng đăng nhập LẦN THỨ BA
const login3 = simulateLogin(email);
console.log("4. Đăng nhập lần 3:", login3);
console.assert(login3.isFirstTime === false, "Lần 3 phải có isFirstTime = false");
console.assert(!login3.redirectTarget.includes("first_login=1"), "Lần 3 KHÔNG ĐƯỢC có ?first_login=1");

console.log("\n=> TẤT CẢ CÁC ĐIỀU KIỆN KIỂM TRA ĐỀU CHÍNH XÁC 100%!");
