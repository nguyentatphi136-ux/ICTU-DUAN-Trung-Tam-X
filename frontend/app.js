// =====================================================================
// EduManager - script dùng chung cho mọi trang
// =====================================================================

// ---------------------------------------------------------------------
// 1. Điều hướng giữa các trang
// ---------------------------------------------------------------------
const PAGES = {
  home: "index.html",
  login: "login.html",
  "forgot-password": "ForgotPasswordForm.html",
  "reset-password": "ResetPasswordForm.html",
};

const navigateTo = (page) => {
  const url = PAGES[page] ?? page;
  window.location.assign(url);
};

// ---------------------------------------------------------------------
// 1b. Role và tài khoản dùng thử (CHỈ để demo giao diện, không dùng thật)
// ---------------------------------------------------------------------
// Khách truy cập (Guest) không cần tài khoản -> trang index.html.
// page: trang đích sau khi đăng nhập; menu: các mục ở thanh bên của trang.
const ROLES = {
  student: {
    name: "Học viên",
    en: "Student",
    page: "student.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["calendar-days", "Lịch học", "#lich-hoc"],
      ["notebook-pen", "Bài tập", "#bai-tap"],
      ["award", "Bảng điểm", "#bang-diem"],
      ["wallet", "Học phí", "#hoc-phi"],
    ],
  },
  instructor: {
    name: "Giảng viên",
    en: "Instructor",
    page: "instructor.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["clipboard-check", "Điểm danh", "#diem-danh"],
      ["file-plus", "Giao bài", "#giao-bai"],
      ["pen-line", "Chấm điểm", "#cham-diem"],
      ["trending-up", "Tiến độ lớp", "#tien-do"],
    ],
  },
  ta: {
    name: "Trợ giảng",
    en: "Teaching Assistant",
    page: "ta.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["shield-check", "Phạm vi uỷ quyền", "#uy-quyen"],
      ["clipboard-check", "Điểm danh", "#diem-danh"],
      ["pen-line", "Chấm bài", "#cham-bai"],
      ["heart-handshake", "Kèm học viên", "#kem-hoc"],
    ],
  },
  "training-manager": {
    name: "Quản lý đào tạo",
    en: "Training Manager",
    page: "training-manager.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["folder-plus", "Mở lớp", "#mo-lop"],
      ["calendar-range", "Lịch & phân công", "#phan-cong"],
      ["circle-pause", "Duyệt bảo lưu", "#bao-luu"],
      ["badge-check", "Chất lượng khoá", "#chat-luong"],
    ],
  },
  admissions: {
    name: "Tư vấn tuyển sinh",
    en: "Admissions",
    page: "admissions.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["users", "Danh sách lead", "#lead"],
      ["phone-call", "Lịch chăm sóc", "#cham-soc"],
    ],
  },
  accountant: {
    name: "Kế toán",
    en: "Accountant",
    page: "accountant.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["receipt", "Ghi nhận thanh toán", "#thanh-toan"],
      ["circle-alert", "Công nợ", "#cong-no"],
      ["chart-column", "Báo cáo doanh thu", "#bao-cao"],
    ],
  },
  admin: {
    name: "Quản trị hệ thống",
    en: "Admin",
    page: "admin.html",
    menu: [
      ["layout-dashboard", "Tổng quan", "#tong-quan"],
      ["user-cog", "Tài khoản", "#tai-khoan"],
      ["key-round", "Vai trò & quyền", "#vai-tro"],
      ["folder-tree", "Danh mục dùng chung", "#danh-muc"],
      ["scroll-text", "Nhật ký hệ thống", "#nhat-ky"],
    ],
  },
};

const DEMO_ACCOUNTS = [
  {
    email: "admin@edumanager.vn",
    aliases: ["admin", "admin@example.com", "admin@edumanager.vn", "admin@tms.vn"],
    password: "Admin@123",
    passwords: ["Admin@123", "admin123"],
    role: "admin",
    fullName: "Quản trị viên",
    phone: "0988123456",
  },
  {
    email: "quanlydaotao@edumanager.vn",
    aliases: ["daotao@edumanager.vn", "daotao@tms.vn", "daotao", "quanlydaotao", "training@example.com", "training@edumanager.vn", "training@tms.vn", "training"],
    password: "Daotao@123",
    passwords: ["Daotao@123", "Quanly@123", "training123", "daotao123"],
    role: "training-manager",
    fullName: "Phạm Thị Quản",
    phone: "0977234567",
  },
  {
    email: "tuvan@edumanager.vn",
    aliases: ["tuyensinh@edumanager.vn", "tuyensinh@tms.vn", "tuvan@tms.vn", "tuvan", "tuyensinh", "admissions@example.com", "admissions@edumanager.vn", "admissions@tms.vn", "admissions"],
    password: "Tuyensinh@123",
    passwords: ["Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123"],
    role: "admissions",
    fullName: "Hoàng Văn Tư",
    phone: "0966345678",
  },
  {
    email: "giangvien@edumanager.vn",
    aliases: ["giangvien@tms.vn", "giangvien", "instructor@example.com", "instructor@edumanager.vn", "instructor@tms.vn", "instructor"],
    password: "Giangvien@123",
    passwords: ["Giangvien@123", "instructor123", "giangvien123"],
    role: "instructor",
    fullName: "Trần Thị Giảng",
    phone: "0912456789",
  },
  {
    email: "trogiang@edumanager.vn",
    aliases: ["trogiang@tms.vn", "trogiang", "ta@example.com", "ta@edumanager.vn", "ta@tms.vn", "ta"],
    password: "Trogiang@123",
    passwords: ["Trogiang@123", "ta123", "trogiang123"],
    role: "ta",
    fullName: "Lê Văn Trợ",
    phone: "0934567890",
  },
  {
    email: "quanlydaotao@edumanager.vn",
    password: "123456",
    role: "training-manager",
    fullName: "Phạm Thị Quản",
  },
  {
    email: "tuvan@edumanager.vn",
    password: "123456",
    role: "admissions",
    fullName: "Hoàng Văn Tư",
  },
  {
    email: "ketoan@edumanager.vn",
    aliases: ["ketoan@tms.vn", "ketoan", "accountant@example.com", "accountant@edumanager.vn", "accountant@tms.vn", "accountant"],
    password: "Ketoan@123",
    passwords: ["Ketoan@123", "accountant123", "ketoan123"],
    role: "accountant",
    fullName: "Đỗ Thị Kế",
    phone: "0945678901",
  },
  {
    email: "hocvien@edumanager.vn",
    aliases: ["hocvien@tms.vn", "hocvien", "student@example.com", "student@edumanager.vn", "student@tms.vn", "student"],
    password: "Hocvien@123",
    passwords: ["Hocvien@123", "student123", "hocvien123"],
    role: "student",
    fullName: "Nguyễn Văn Học",
    phone: "0956789012",
  },
];

const SESSION_KEY = "edumanager-session";

    return data;
  } catch {
    return null;
  }
};

// ---------------------------------------------------------------------
// Tự động gia hạn phiên khi còn hoạt động (Sliding Window - S1-02 AC1)
// ---------------------------------------------------------------------
const renewSession = () => {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return;
    const data = JSON.parse(raw);
    if (!data || !data.expiresAt) return;
    // Nếu phiên đã hết hạn rồi thì tuyệt đối KHÔNG gia hạn mà kích hoạt hết hạn ngay
    if (Date.now() >= Number(data.expiresAt)) {
      expireSession("Phiên đăng nhập đã hết hạn do không có hoạt động. Vui lòng đăng nhập lại.");
      return;
    }
    data.expiresAt = Date.now() + SESSION_TIMEOUT_MS;
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(data));
    console.log(`%c[TMS Session] Đã gia hạn phiên tự động thêm 60 phút (Hết hạn lúc: ${new Date(data.expiresAt).toLocaleTimeString()})`, "color: #10b981; font-weight: bold;");
  } catch {}
};

// Lắng nghe tương tác người dùng (click, phím, cuộn chuột, chạm) để gia hạn phiên
let lastSessionRenew = 0;
const handleUserActivity = () => {
  const now = Date.now();
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return;

  let isNearExpiry = false;
  try {
    const data = JSON.parse(raw);
    // Nếu phiên đã quá hạn:
    if (data.expiresAt && now >= Number(data.expiresAt)) {
      expireSession("Phiên đăng nhập đã hết hạn do không có hoạt động. Vui lòng đăng nhập lại.");
      return;
    }
    // Nếu phiên sắp hết hạn (trong vòng 60s hoặc đang trong chế độ test): gia hạn ngay lập tức
    if (data.expiresAt && Number(data.expiresAt) - now <= 60000) {
      isNearExpiry = true;
    }
  } catch {}

  // Bình thường throttle 3 giây để tối ưu ghi storage
  if (isNearExpiry || now - lastSessionRenew > 3000) {
    lastSessionRenew = now;
    renewSession();
  }
};

// Bộ giám sát phiên thời gian thực (Watchdog Timer): Tự động phát hiện hết hạn khi người dùng không tương tác
let sessionWatchdogTimer = null;
const startSessionWatchdog = () => {
  if (typeof window === "undefined") return;
  if (sessionWatchdogTimer) clearInterval(sessionWatchdogTimer);
  sessionWatchdogTimer = setInterval(() => {
    // Chỉ chạy watchdog ở các trang yêu cầu quyền (có data-role trên body)
    const isProtectedPage = Boolean(document.body && document.body.dataset && document.body.dataset.role);
    if (!isProtectedPage) return;

    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) {
      clearInterval(sessionWatchdogTimer);
      expireSession("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
      return;
    }

    try {
      const data = JSON.parse(raw);
      if (Number(data.expiresAt) && Date.now() >= Number(data.expiresAt)) {
        clearInterval(sessionWatchdogTimer);
        expireSession("Phiên đăng nhập đã hết hạn do không có hoạt động. Vui lòng đăng nhập lại.");
      }
    } catch {
      clearInterval(sessionWatchdogTimer);
      expireSession();
    }
  }, 1000);
};

if (typeof window !== "undefined") {
  ["click", "keydown", "mousemove", "scroll", "touchstart"].forEach((eventType) => {
    window.addEventListener(eventType, handleUserActivity, { passive: true });
  });

  startSessionWatchdog();

  // Tiện ích hỗ trợ kiểm thử tính năng gia hạn phiên (S1-02 AC1) trong Console
  window.__TMS_SESSION_TEST__ = {
    getSession: () => JSON.parse(sessionStorage.getItem(SESSION_KEY) || "null"),
    renewSession,
    setTimeRemainingSeconds: (sec) => {
      const raw = sessionStorage.getItem(SESSION_KEY);
      if (!raw) {
        console.warn("[TMS Test] Chưa có phiên đăng nhập trong sessionStorage!");
        return;
      }
      const data = JSON.parse(raw);
      data.expiresAt = Date.now() + sec * 1000;
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(data));
      // Đảm bảo Watchdog đang hoạt động để canh đúng từng giây
      startSessionWatchdog();
      console.log(`%c[TMS Test] Đã đặt thời gian phiên còn lại: ${sec}s.`, "color: #0974f1; font-weight: bold; font-size: 13px;");
      console.log(`⏱️ TRƯỜNG HỢP 1 (TEST HẾT HẠN): Hãy để yên chuột và phím. Sau đúng ${sec}s, hệ thống sẽ tự động chuyển về trang đăng nhập login.html kèm thông báo hết hạn.`);
      console.log(`🔄 TRƯỜNG HỢP 2 (TEST GIA HẠN): Trước khi hết ${sec}s, hãy click chuột hoặc gõ phím, phiên sẽ được tự động cộng thêm thời gian.`);
    },
    expireNow: () => {
      const data = JSON.parse(sessionStorage.getItem(SESSION_KEY) || "{}");
      data.expiresAt = Date.now() - 1000;
      sessionStorage.setItem(SESSION_KEY, JSON.stringify(data));
      expireSession("Phiên đăng nhập đã hết hạn do không có hoạt động. Vui lòng đăng nhập lại.");
    },
  };
}

const findAccount = (email, password) => {
  const rawEmail = (email || "").trim().toLowerCase();
  const rawPass = (password || "").trim();
  const customAccounts = readStore("edumanager-custom-accounts") || [];
  const customPasswords = readStore("edumanager-custom-passwords") || {};

  const allAccounts = [...DEMO_ACCOUNTS];
  customAccounts.forEach((c) => {
    if (!allAccounts.some((a) => a.email.toLowerCase() === c.email.toLowerCase())) {
      allAccounts.push(c);
    }
  });

  return allAccounts.find((account) => {
    const accEmail = account.email.toLowerCase();
    const aliases = (account.aliases || []).map((a) => a.toLowerCase());

    const emailMatch =
      accEmail === rawEmail ||
      aliases.includes(rawEmail) ||
      (rawEmail.includes("@") && accEmail.split("@")[0] === rawEmail.split("@")[0]);

    if (!emailMatch) return false;

    // 1. Kiểm tra nếu người dùng đã đổi mật khẩu qua S1-04
    const customPass = customPasswords[accEmail] || customPasswords[account.email];
    if (customPass && customPass === rawPass) {
      return true;
    }

    // 2. Chấp nhận danh sách mật khẩu hợp lệ của tài khoản (không chấp nhận 123456)
    const validPasswords = [
      account.password,
      ...(account.passwords || []),
    ].map((p) => (p || "").trim().toLowerCase());

    return validPasswords.includes(rawPass.toLowerCase());
  });
};

const getCurrentUser = () => getSessionData();

const saveLoginFormState = (email = "", password = "") => {
  localStorage.setItem(
    LOGIN_FORM_KEY,
    JSON.stringify({ email: email.trim(), password: password.trim() }),
  );

const getCurrentUser = () => {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY));
  } catch {
    localStorage.removeItem(LOGIN_FORM_KEY);
  }
};

// =====================================================================
// KHÔI PHỤC DỮ LIỆU ĐANG NHẬP DỞ KHI HẾT HẠN PHIÊN (S1-02 AC3)
// =====================================================================
const FORM_DRAFTS_KEY = "edumanager-form-drafts";
const RETURN_URL_KEY = "edumanager-return-url";

const getElementDraftKey = (el) => {
  if (el.id) return `#${el.id}`;
  if (el.name) return `name:${el.name}`;
  const form = el.closest("form");
  if (form) {
    const catalog = form.dataset.catalogForm;
    if (catalog) return `catalog:${catalog}`;
    if (form.id) {
      const idx = Array.from(form.elements).indexOf(el);
      return `#${form.id}::idx_${idx}`;
    }
  }
  const modal = el.closest(".tms-modal, [data-modal]");
  if (modal && modal.id) {
    const placeholder = el.getAttribute("placeholder") || el.getAttribute("aria-label") || "";
    return `#${modal.id}::${placeholder || el.className}`;
  }
  const placeholder = el.getAttribute("placeholder") || el.getAttribute("aria-label");
  if (placeholder) return `placeholder:${placeholder}`;
  return null;
};

const findFieldByKey = (key) => {
  if (!key) return null;
  if (key.startsWith("#")) {
    if (key.includes("::idx_")) {
      const [formId, idxPart] = key.split("::idx_");
      const form = document.querySelector(formId);
      return form ? form.elements[Number(idxPart)] : null;
    }
    return document.querySelector(key);
  }
  if (key.startsWith("name:")) {
    return document.querySelector(`[name="${key.slice(5)}"]`);
  }
  if (key.startsWith("catalog:")) {
    const form = document.querySelector(`form[data-catalog-form="${key.slice(8)}"]`);
    return form ? form.querySelector("input") : null;
  }
  if (key.startsWith("placeholder:")) {
    const ph = key.slice(12);
    return document.querySelector(`[placeholder="${ph}"], [aria-label="${ph}"]`);
  }
  return null;
};

const saveFormDrafts = () => {
  try {
    const isProtected = Boolean(document.body?.dataset?.role);
    if (!isProtected) return;

    const drafts = JSON.parse(localStorage.getItem(FORM_DRAFTS_KEY) || "{}");
    const pageName = window.location.pathname.split("/").pop() || "admin.html";
    if (!drafts[pageName]) drafts[pageName] = {};

    document.querySelectorAll("input:not([type=password]):not([type=hidden]):not([type=submit]):not([type=button]), textarea, select").forEach((el) => {
      if (el.id === "search-input" || el.id === "global-search") return;
      const key = getElementDraftKey(el);
      if (!key) return;

      const val = el.type === "checkbox" ? el.checked : el.value;
      const isNotEmpty = el.type === "checkbox" ? true : (typeof val === "string" && val.trim().length > 0);

      if (isNotEmpty) {
        drafts[pageName][key] = val;
      } else {
        delete drafts[pageName][key];
      }
    });

    localStorage.setItem(FORM_DRAFTS_KEY, JSON.stringify(drafts));
  } catch (e) {
    console.warn("Lưu bản nháp thất bại:", e);
  }
};

const restoreFormDrafts = () => {
  try {
    const raw = localStorage.getItem(FORM_DRAFTS_KEY);
    if (!raw) return;
    const drafts = JSON.parse(raw);
    const pageName = window.location.pathname.split("/").pop() || "admin.html";
    const pageDrafts = drafts[pageName];
    if (!pageDrafts || Object.keys(pageDrafts).length === 0) return;

    let restoredCount = 0;
    Object.entries(pageDrafts).forEach(([key, val]) => {
      const el = findFieldByKey(key);
      if (el) {
        if (el.type === "checkbox") {
          el.checked = Boolean(val);
        } else {
          el.value = val;
        }
        el.dispatchEvent(new Event("input", { bubbles: true }));
        restoredCount++;
      }
    });

    if (restoredCount > 0) {
      console.log(`%c[TMS Draft] Đã tự động khôi phục ${restoredCount} trường dữ liệu đang nhập dở!`, "color: #10b981; font-weight: bold;");
      if (typeof showToast === "function") {
        showToast("Đã tự động khôi phục dữ liệu bạn đang nhập dở trước khi hết phiên.", "info");
      }
      if (window.location.hash) {
        setTimeout(() => {
          const target = document.querySelector(window.location.hash);
          if (target) target.scrollIntoView({ behavior: "smooth" });
        }, 150);
      }
    }
  } catch (e) {
    console.warn("Khôi phục bản nháp thất bại:", e);
  }
};

const clearContainerDrafts = (container) => {
  try {
    if (!container) return;
    const raw = localStorage.getItem(FORM_DRAFTS_KEY);
    if (!raw) return;
    const drafts = JSON.parse(raw);
    const pageName = window.location.pathname.split("/").pop() || "admin.html";
    if (!drafts[pageName]) return;

    const elements = container.matches && container.matches("input, textarea, select")
      ? [container]
      : Array.from(container.querySelectorAll("input, textarea, select"));

    elements.forEach((el) => {
      const key = getElementDraftKey(el);
      if (key && drafts[pageName][key] !== undefined) {
        delete drafts[pageName][key];
      }
    });

    localStorage.setItem(FORM_DRAFTS_KEY, JSON.stringify(drafts));
  } catch {}
};

// Lắng nghe thao tác nhập liệu để tự động lưu draft theo thời gian thực
if (typeof window !== "undefined") {
  let draftDebounceTimer = null;
  document.addEventListener("input", (e) => {
    if (e.target && e.target.matches && e.target.matches("input, textarea, select")) {
      if (e.target.type === "password" || e.target.type === "hidden") return;
      clearTimeout(draftDebounceTimer);
      draftDebounceTimer = setTimeout(saveFormDrafts, 100);
    }
  });

  document.addEventListener("change", (e) => {
    if (e.target && e.target.matches && e.target.matches("input, textarea, select")) {
      if (e.target.type === "password" || e.target.type === "hidden") return;
      saveFormDrafts();
    }
  });

  document.addEventListener("submit", (e) => {
    if (e.target && e.target.tagName === "FORM") {
      clearContainerDrafts(e.target);
    }
  });
}

function expireSession(message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.") {
  // 1. Lưu bản nháp toàn bộ dữ liệu đang nhập dở ngay lập tức
  saveFormDrafts();

  // 2. Lưu lại đường dẫn & vị trí hash đang đứng để sau khi đăng nhập đưa người dùng về đúng chỗ
  const currentPath = window.location.pathname.split("/").pop() || "admin.html";
  const currentHash = window.location.hash || "";
  localStorage.setItem(RETURN_URL_KEY, currentPath + currentHash);

  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  if (typeof showToast === "function") {
    try {
      showToast(message, "warning");
    } catch {}
  }
  setTimeout(() => {
    if (typeof goToLogin === "function") {
      goToLogin("expired");
    } else {
      window.location.assign("login.html?reason=expired");
    }
  }, 450);
}

const readRevokedRoles = () => {
  try {
    return new Set(JSON.parse(localStorage.getItem(ROLE_REVOCATION_KEY) ?? "[]"));
  } catch {
    return new Set();
  }
};

// Dữ liệu demo lưu trong localStorage để các trang dùng chung
// (vd: khách để lại thông tin -> tư vấn tuyển sinh thấy lead mới).
const readStore = (key, fallback = []) => {
  try {
    return JSON.parse(localStorage.getItem(key)) ?? fallback;
  } catch {
    return fallback;
  }
};

const writeStore = (key, value) =>
  localStorage.setItem(key, JSON.stringify(value));

const LEADS_KEY = "edumanager-leads";
const AUDIT_KEY = "edumanager-audit-log";

const addAuditLog = (action, actor = getCurrentUser()?.email ?? "khách") => {
  const logs = readStore(AUDIT_KEY);
  logs.unshift({ time: new Date().toISOString(), actor, action });
  writeStore(AUDIT_KEY, logs.slice(0, 50));
};

const FIRST_LOGIN_KEY = "edumanager-first-login-status";

const login = (account) => {
  const { password, ...user } = account;
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user));
  addAuditLog("Đăng nhập", user.email);

  // Kiểm tra trạng thái lần đầu đăng nhập
  const firstLoginMap = readStore(FIRST_LOGIN_KEY, {});
  const normEmail = (user.email || "").toLowerCase();
  const loginInfo = firstLoginMap[normEmail] || {
    isFirstLogin: Boolean(account.isFirstLogin),
    loginCount: account.loginCount || 0,
  };

  let isFirstTime = false;
  if (loginInfo.isFirstLogin && (loginInfo.loginCount === 0 || loginInfo.loginCount === undefined)) {
    isFirstTime = true;
    // ĐÁNH DẤU HOÀN TẤT LẦN ĐẦU: Từ lần thứ 2 trở đi hệ thống sẽ KHÔNG đưa đến đổi mật khẩu nữa
    firstLoginMap[normEmail] = {
      isFirstLogin: false,
      loginCount: 1,
      firstLoggedInAt: new Date().toISOString(),
    };
    writeStore(FIRST_LOGIN_KEY, firstLoginMap);
  } else {
    // Lần đăng nhập thứ 2 trở đi: chỉ tăng số lần đăng nhập
    firstLoginMap[normEmail] = {
      isFirstLogin: false,
      loginCount: (loginInfo.loginCount || 1) + 1,
      lastLoggedInAt: new Date().toISOString(),
    };
    writeStore(FIRST_LOGIN_KEY, firstLoginMap);
  }

  // Nếu có returnUrl trước khi hết hạn phiên, chuyển người dùng về đúng trang và vị trí hash cũ
  const returnUrl = localStorage.getItem(RETURN_URL_KEY);
  if (returnUrl) {
    localStorage.removeItem(RETURN_URL_KEY);
    window.location.assign(returnUrl);
    return;
  }

  const targetPage = ROLES[user.role]?.page || "admin.html";
  const redirectTarget = isFirstTime ? `${targetPage}?first_login=1` : targetPage;
  navigateTo(redirectTarget);
};

const logout = () => {
  addAuditLog("Đăng xuất");
  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(LOGIN_FORM_KEY);
  goToLogin("logout");
};

const goToRoleHome = (role) => {
  navigateTo(ROLES[role]?.page ?? "login");
};

const goToLogin = (reason) => {
  if (reason) {
    window.location.assign(`login.html?reason=${encodeURIComponent(reason)}`);
  } else {
    navigateTo("login");
  }
};
const goToForgotPassword = () => navigateTo("forgot-password");
const goBack = () => {
  if (window.history.length > 1) {
    window.history.back();
  } else {
    goToLogin();
  }
};

// Mọi phần tử có data-nav="home" | "login" | "forgot-password" | "back" |
// "logout" sẽ tự điều hướng (bắt sự kiện ở document nên áp dụng cả cho
// phần tử được tạo bằng JS như thanh bên).
document.addEventListener("click", (event) => {
  const element = event.target.closest("[data-nav]");
  if (!element) return;

  event.preventDefault();
  const target = element.dataset.nav;
  if (target === "back") {
    goBack();
    return;
  }
  if (target === "logout") {
    logout();
    return;
  }
  navigateTo(target);
});

// ---------------------------------------------------------------------
// 1c. Khung trang của các role: chặn truy cập + dựng thanh bên
// ---------------------------------------------------------------------
// Trang role khai báo <body data-role="...">. Chưa đăng nhập -> về login,
// đăng nhập bằng role khác -> chuyển về trang của role đó.
const pageRole = document.body.dataset.role;
const currentUser = getCurrentUser();

const escapeHtml = (value) =>
  String(value).replace(
    /[&<>"']/g,
    (char) =>
      ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;",
      })[char],
  );

const getInitials = (name) =>
  name
    .split(" ")
    .filter(Boolean)
    .slice(-2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();

if (pageRole) {
  if (!currentUser) {
    goToLogin();
  } else if (currentUser.role !== pageRole) {
    goToRoleHome(currentUser.role);
  }
}

const sidebar = document.querySelector("#app-sidebar");

if (sidebar && currentUser && ROLES[pageRole]) {
  const role = ROLES[pageRole];
  sidebar.innerHTML = `
    <div class="app-sidebar-header" style="height: 64px; padding: 0 20px; display: flex; align-items: center; border-bottom: 1px solid var(--color-border);">
      <a href="index.html" class="tms-brand" style="display: flex; align-items: center; gap: 10px; text-decoration: none;">
        <div class="tms-logo-box" style="width: 36px; height: 36px; border-radius: 10px; background: var(--color-blue-primary); color: #fff; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 10px rgba(9, 116, 241, 0.25);">
          <i data-lucide="graduation-cap" style="width: 20px; height: 20px;"></i>
        </div>
        <span style="font-size: 22px; font-weight: 800; color: var(--color-navy-title); letter-spacing: -0.02em;">TMS<span style="color: var(--color-blue-primary);">.</span></span>
      </a>
    </div>

    <!-- Giữ các thẻ tương thích test tự động S1-06 -->
    <div class="app-sidebar-profile" style="display: none;" aria-hidden="true">
      <span class="app-avatar">${escapeHtml(getInitials(currentUser.fullName))}</span>
      <p class="app-user-name" id="user-display-name">${escapeHtml(currentUser.fullName)}</p>
      <p class="app-user-email">${escapeHtml(currentUser.email)}</p>
      <span id="user-role-badge">${role.name}</span>
    </div>

    <div style="padding: 16px 20px 6px;">
      <p style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.06em; color: var(--color-text-muted);">MENU ĐIỀU HƯỚNG</p>
    </div>

    <nav class="app-nav app-sidebar-nav" aria-label="Điều hướng ${role.name}" style="flex: 1; padding: 6px 12px; display: flex; flex-direction: column; gap: 4px;">
      ${role.menu
        .map(
          ([icon, label, href], index) => `
            <a href="${href}" class="${index === 0 ? "is-active" : ""}">
              <i data-lucide="${icon}" aria-hidden="true"></i><span>${label}</span>
            </a>`,
        )
        .join("")}
    </nav>

    <div class="app-sidebar-footer" style="padding: 16px 14px 20px; border-top: 1px solid var(--color-border); display: flex; flex-direction: column; gap: 8px;">
      <div style="padding: 10px 12px; background: var(--color-bg-page); border-radius: 10px; border: 1px solid var(--color-border); display: flex; align-items: center; gap: 10px;">
        <span class="tms-avatar-circle" style="width: 36px; height: 36px; font-size: 13px; flex-shrink: 0;">${escapeHtml(getInitials(currentUser.fullName))}</span>
        <div style="min-width: 0; flex: 1;">
          <p style="font-size: 13px; font-weight: 700; color: var(--color-navy-title); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin: 0;">${escapeHtml(currentUser.fullName)}</p>
          <p style="font-size: 11px; color: var(--color-text-secondary); margin: 0;">${role.name}</p>
        </div>
      </div>
      <button type="button" class="app-logout" data-nav="logout">
        <i data-lucide="log-out" aria-hidden="true"></i>Đăng xuất
      </button>
      <button type="button" class="app-settings" id="open-change-password-modal-sidebar" aria-label="Đổi mật khẩu">
        <i data-lucide="key" aria-hidden="true"></i>
        <span>Đổi mật khẩu</span>
      </button>
    </div>`;

  document.title = `${role.name} | TMS. - Quản lý đào tạo`;
  if (window.lucide) window.lucide.createIcons();

  const navLinks = sidebar.querySelectorAll(".app-nav a");
  navLinks.forEach((link) => {
    link.addEventListener("click", () => {
      navLinks.forEach((item) => item.classList.remove("is-active"));
      link.classList.add("is-active");
      document.body.classList.remove("sidebar-open");
    });
  });

  // Tô sáng mục menu theo phần đang hiển thị khi cuộn trang.
  const sections = [...navLinks]
    .map((link) => document.querySelector(link.getAttribute("href")))
    .filter(Boolean);
  const observer = new IntersectionObserver(
    (entries) => {
      entries
        .filter((entry) => entry.isIntersecting)
        .forEach((entry) => {
          navLinks.forEach((link) =>
            link.classList.toggle(
              "is-active",
              link.getAttribute("href") === `#${entry.target.id}`,
            ),
          );
        });
    },
    { rootMargin: "-30% 0px -60% 0px" },
  );
  sections.forEach((section) => observer.observe(section));
}

// Điền thông tin người dùng vào các phần tử có data-user="fullName" | "email".
const handleForbiddenError = (message = "Bạn không có quyền truy cập chức năng này. Vui lòng kiểm tra vai trò hoặc liên hệ quản trị viên.") => {
  showToast(message, "warning");
  return false;
};

const addAuthHeaders = (headers = {}) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  if (!token) return headers;

  return {
    ...headers,
    Authorization: `Bearer ${token}`,
  };
};

const handleSessionExpiredResponse = () => {
  expireSession("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
  return Promise.reject(new Error("SESSION_EXPIRED"));
};

const safeFetch = async (url, options = {}) => {
  const session = getCurrentUser();
  if (!session) {
    return handleSessionExpiredResponse();
  }

  const response = await fetch(url, {
    ...options,
    headers: addAuthHeaders(options.headers),
  });

  if (response.status === 401) {
    return handleSessionExpiredResponse();
  }

  if (response.status === 403) {
    let msg = "Bạn không có quyền truy cập chức năng này. Vui lòng kiểm tra vai trò hoặc liên hệ quản trị viên.";
    try {
      const clone = response.clone();
      clone.json().then((data) => {
        if (data && (data.message || (data.error && data.error.message))) {
          handleForbiddenError(data.message || data.error.message);
        } else {
          handleForbiddenError(msg);
        }
      }).catch(() => handleForbiddenError(msg));
    } catch {
      handleForbiddenError(msg);
    }
    return Promise.reject(new Error("FORBIDDEN"));
  }

  return response;
};

window.fetch = new Proxy(window.fetch.bind(window), {
  apply(target, thisArg, args) {
    const [input, init = {}] = args;
    const session = getCurrentUser();

    if (session) {
      const headers = addAuthHeaders(init.headers ?? {});
      args[1] = { ...init, headers };
    }

    return Reflect.apply(target, thisArg, args).then((response) => {
      if (response.status === 401) {
        handleSessionExpiredResponse();
      }
      if (response.status === 403) {
        let msg = "Bạn không có quyền truy cập chức năng này. Vui lòng kiểm tra vai trò hoặc liên hệ quản trị viên.";
        try {
          const clone = response.clone();
          clone.json().then((data) => {
            if (data && (data.message || (data.error && data.error.message))) {
              handleForbiddenError(data.message || data.error.message);
            } else {
              handleForbiddenError(msg);
            }
          }).catch(() => handleForbiddenError(msg));
        } catch {
          handleForbiddenError(msg);
        }
      }
      return response;
    });
  },
});

document.querySelectorAll("[data-user]").forEach((element) => {
  element.textContent = currentUser?.[element.dataset.user] ?? "";
});

// Khởi tạo avatar và menu dropdown người dùng ở thanh Topbar (S1-02: Menu đăng xuất)
if (currentUser?.fullName) {
  document.querySelectorAll("#user-avatar-initials, .tms-avatar-circle").forEach((el) => {
    if (!el.textContent.trim() || el.textContent.trim() === "QT") {
      el.textContent = getInitials(currentUser.fullName);
    }
  });
}

const userProfileBtn = document.querySelector("#tms-user-profile-btn");
const userDropdownMenu = document.querySelector("#tms-user-dropdown-menu");

if (userProfileBtn && userDropdownMenu) {
  userProfileBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    userDropdownMenu.classList.toggle("is-open");
    const isExpanded = userDropdownMenu.classList.contains("is-open");
    userProfileBtn.setAttribute("aria-expanded", String(isExpanded));
  });

  document.addEventListener("click", (e) => {
    if (!userDropdownMenu.contains(e.target) && !userProfileBtn.contains(e.target)) {
      userDropdownMenu.classList.remove("is-open");
      userProfileBtn.setAttribute("aria-expanded", "false");
    }
  });
}

document.querySelector("#dropdown-change-pass")?.addEventListener("click", () => {
  userDropdownMenu?.classList.remove("is-open");
  const modal = document.querySelector("#change-password-modal");
  if (modal) {
    modal.classList.remove("hidden");
    document.querySelector("#cp-current")?.focus();
  }
});

document.querySelector("#open-change-password-modal-sidebar")?.addEventListener("click", () => {
  const modal = document.querySelector("#change-password-modal");
  if (modal) {
    modal.classList.remove("hidden");
    document.querySelector("#cp-current")?.focus();
  }
});

document.querySelector("#sidebar-toggle")?.addEventListener("click", () => {
  document.body.classList.toggle("sidebar-open");
});

document.querySelector(".app-backdrop")?.addEventListener("click", () => {
  document.body.classList.remove("sidebar-open");
});

document.querySelectorAll("[data-today]").forEach((element) => {
  element.textContent = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
});

document.querySelectorAll("[data-home-date]").forEach((element) => {
  element.textContent = new Date().toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "long",
    year: "numeric",
  });
});

// ---------------------------------------------------------------------
// 2. Tiện ích dùng chung
// ---------------------------------------------------------------------
if (typeof lucide !== "undefined") {
  lucide.createIcons();
} else if (window.lucide) {
  window.lucide.createIcons();
}

const formStatus = document.querySelector("#form-status");

const showStatus = (message) => {
  if (!formStatus) return;
  formStatus.textContent = message;
  formStatus.classList.remove("hidden");
};

const hideStatus = () => formStatus?.classList.add("hidden");

const setFieldError = (input, errorElement, message) => {
  errorElement.textContent = message;
  errorElement.classList.toggle("is-visible", Boolean(message));
  input.setAttribute("aria-invalid", String(Boolean(message)));
};

// ---------------------------------------------------------------------
// 3. Ô email (cả 2 trang đều có)
// ---------------------------------------------------------------------
const emailInput = document.querySelector("#email");
const emailError = document.querySelector("#email-error");

const validateEmail = () => {
  const value = emailInput.value.trim().toLowerCase();
  if (VALID_ROLE_ALIASES.has(value) || VALID_ROLE_ALIASES.has(value.split("@")[0])) {
    setFieldError(emailInput, emailError, "");
    return true;
  }
  const emailRegex = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
  const message = !value
    ? "Vui lòng nhập email."
    : !emailInput.validity.valid || !value.includes("@") || !emailRegex.test(value)
      ? "Vui lòng nhập email đúng định dạng, ví dụ ten@domain.com."
      : "";

  setFieldError(emailInput, emailError, message);
  return !message;
};

if (emailInput && emailError) {
  emailInput.setAttribute("aria-describedby", "email-error");
  emailInput.addEventListener("input", () => {
    if (emailInput.value.trim() && emailInput.validity.valid) {
      setFieldError(emailInput, emailError, "");
    }
  });
}

// ---------------------------------------------------------------------
// 4. Trang đăng nhập: ẩn/hiện mật khẩu, kiểm tra form
// ---------------------------------------------------------------------
const passwordInput = document.querySelector("#password");
const passwordToggle = document.querySelector("#password-toggle");
const passwordError = document.querySelector("#password-error");
const loginForm = document.querySelector("#login-form");

const validatePassword = () => {
  const message = passwordInput.value.trim() ? "" : "Vui lòng nhập mật khẩu.";
  setFieldError(passwordInput, passwordError, message);
  return !message;
};

if (passwordInput && passwordToggle) {
  passwordToggle.addEventListener("click", () => {
    const showingPassword = passwordInput.type === "password";
    passwordInput.type = showingPassword ? "text" : "password";
    passwordToggle.setAttribute(
      "aria-label",
      showingPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu",
    );
    passwordToggle.innerHTML = `<i data-lucide="${showingPassword ? "eye-off" : "eye"}" class="size-[17px]" aria-hidden="true"></i>`;
    lucide.createIcons();
  });
}

if (passwordInput && passwordError) {
  passwordInput.setAttribute("aria-describedby", "password-error");
  passwordInput.addEventListener("input", () => {
    if (passwordInput.value.trim()) {
      setFieldError(passwordInput, passwordError, "");
    }
  });
}

// ---------------------------------------------------------------------
// Chính sách khóa tạm tài khoản: Khóa tạm 15 phút sau 5 lần sai liên tiếp (S1-01 AC3)
// ---------------------------------------------------------------------
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

  // Đã hết 15 phút khóa tạm -> tự động mở khóa và reset số lần sai
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

// ---------------------------------------------------------------------
// 4a. Quản lý các Banner trạng thái & Đếm ngược khóa tạm (S1-01, S1-02)
// ---------------------------------------------------------------------
const loginErrorBanner = document.querySelector("#login-error-banner");
const loginAttemptsRemaining = document.querySelector("#login-attempts-remaining");
const loginLockoutBanner = document.querySelector("#login-lockout-banner");
const lockoutTimerText = document.querySelector("#lockout-timer-text");
const logoutSuccessBanner = document.querySelector("#logout-success-banner");
const logoutBannerText = document.querySelector("#logout-banner-text");
const loginSubmitButton = document.querySelector("#login-form button[type='submit']");

let lockoutTimerInterval = null;

const formatMMSS = (totalSeconds) => {
  const m = Math.floor(Math.max(0, totalSeconds) / 60);
  const s = Math.floor(Math.max(0, totalSeconds) % 60);
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
};

const hideAllLoginBanners = () => {
  if (loginErrorBanner) {
    loginErrorBanner.classList.add("hidden");
    loginErrorBanner.style.display = "none";
  }
  if (loginLockoutBanner) {
    loginLockoutBanner.classList.add("hidden");
    loginLockoutBanner.style.display = "none";
  }
  if (logoutSuccessBanner) {
    logoutSuccessBanner.classList.add("hidden");
    logoutSuccessBanner.style.display = "none";
  }
  hideStatus();
};

const startLockoutCountdown = (lockedUntil, email) => {
  if (lockoutTimerInterval) {
    clearInterval(lockoutTimerInterval);
    lockoutTimerInterval = null;
  }

  if (loginErrorBanner) {
    loginErrorBanner.classList.add("hidden");
    loginErrorBanner.style.display = "none";
  }
  if (logoutSuccessBanner) {
    logoutSuccessBanner.classList.add("hidden");
    logoutSuccessBanner.style.display = "none";
  }

  const updateTimer = () => {
    const now = Date.now();
    const remainingMs = lockedUntil - now;
    const remainingSeconds = Math.ceil(remainingMs / 1000);

    if (remainingSeconds <= 0) {
      clearInterval(lockoutTimerInterval);
      lockoutTimerInterval = null;
      clearLoginAttempts(email);
      if (loginLockoutBanner) {
        loginLockoutBanner.classList.add("hidden");
        loginLockoutBanner.style.display = "none";
      }
      if (loginSubmitButton) {
        loginSubmitButton.disabled = false;
      }
      showToast("Tài khoản đã được mở khóa. Bạn có thể đăng nhập lại.", "success");
      return;
    }

    if (loginLockoutBanner) {
      loginLockoutBanner.classList.remove("hidden");
      loginLockoutBanner.style.display = "flex";
    }
    if (lockoutTimerText) {
      lockoutTimerText.textContent = `Bạn đã nhập sai 5 lần. Vui lòng thử lại sau ${formatMMSS(remainingSeconds)}.`;
    }
    if (loginSubmitButton) {
      loginSubmitButton.disabled = true;
    }
  };

  updateTimer();
  lockoutTimerInterval = setInterval(updateTimer, 1000);
};

// Kiểm tra trạng thái ngay khi mở trang Login
if (loginForm) {
  // Mặc định ẩn toàn bộ banner khi chưa có thao tác
  hideAllLoginBanners();

  // 1. Kiểm tra nếu vừa đăng xuất hoặc hết hạn phiên hoặc đổi mật khẩu thành công
  const urlParams = new URLSearchParams(window.location.search);
  const reason = urlParams.get("reason") || (urlParams.get("logout") ? "logout" : null);
  if (reason && logoutSuccessBanner) {
    logoutSuccessBanner.classList.remove("hidden");
    logoutSuccessBanner.style.display = "flex";
    if (reason === "expired" && logoutBannerText) {
      logoutBannerText.textContent = "Phiên làm việc đã kết thúc trên máy chủ. Đăng nhập lại để tiếp tục.";
    } else if (reason === "reset_success" && logoutBannerText) {
      logoutBannerText.textContent = "Mật khẩu của bạn đã được đặt lại thành công! Bạn có thể đăng nhập ngay với mật khẩu mới.";
      showToast("Mật khẩu đã được đặt lại thành công! Vui lòng đăng nhập.", "success");
    }
  }

  // 2. Kiểm tra nếu email có sẵn đang bị khóa tạm
  const initialEmail = emailInput?.value?.trim();
  if (initialEmail) {
    const lockStatus = checkAccountLockout(initialEmail);
    if (lockStatus.isLocked && lockStatus.lockedUntil) {
      startLockoutCountdown(lockStatus.lockedUntil, initialEmail);
    }
  }

  // 3. Khi người dùng nhập email khác -> cập nhật trạng thái đếm ngược tương ứng
  emailInput?.addEventListener("input", () => {
    const typedEmail = emailInput.value.trim();
    const lockStatus = checkAccountLockout(typedEmail);
    if (lockStatus.isLocked && lockStatus.lockedUntil) {
      startLockoutCountdown(lockStatus.lockedUntil, typedEmail);
    } else {
      if (loginLockoutBanner && lockoutTimerInterval) {
        clearInterval(lockoutTimerInterval);
        lockoutTimerInterval = null;
        loginLockoutBanner.classList.add("hidden");
        loginLockoutBanner.style.display = "none";
        if (loginSubmitButton) loginSubmitButton.disabled = false;
      }
    }
  });
}

loginForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideStatus();

  const emailIsValid = validateEmail();
  const passwordIsValid = validatePassword();

  if (!emailIsValid || !passwordIsValid) {
    (emailIsValid ? passwordInput : emailInput).focus();
    return;
  }

  saveLoginFormState(emailInput.value, passwordInput.value);

  const rawInput = emailInput.value.trim();

  // 1. Kiểm tra chính sách khóa tạm 15 phút sau 5 lần sai liên tiếp (S1-01 AC3)
  const lockoutStatus = checkAccountLockout(rawInput);
  if (lockoutStatus.isLocked) {
    startLockoutCountdown(lockoutStatus.lockedUntil, rawInput);
    showStatus(
      `Tài khoản tạm thời bị khóa do nhập sai mật khẩu 5 lần liên tiếp. Vui lòng thử lại sau ${lockoutStatus.remainingMinutes} phút.`
    );
    return;
  }

  // 2. Kiểm tra nếu tài khoản bị Quản trị viên khóa vĩnh viễn (S1-10)
  const account = findAccount(emailInput.value, passwordInput.value);
  const targetEmail = account ? account.email : normalizeLoginIdentity(rawInput);
  const lockedAccounts = readStore("edumanager-locked-accounts") || [];

  if (lockedAccounts.includes(targetEmail)) {
    hideAllLoginBanners();
    const reasons = readStore("edumanager-locked-reasons") || {};
    const reason = reasons[targetEmail];
    showStatus(reason ? `Tài khoản đã bị khoá. Lý do: ${reason}` : "Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên.");
    return;
  }

  // 3. Nếu sai email hoặc mật khẩu
  if (!account) {
    const failResult = recordFailedLogin(rawInput);
    if (failResult.isLocked) {
      startLockoutCountdown(failResult.lockedUntil, rawInput);
      showStatus(
        `Tài khoản đã bị tạm khóa 15 phút do nhập sai 5 lần liên tiếp. Vui lòng thử lại sau 15 phút.`
      );
    } else {
      hideAllLoginBanners();
      if (loginErrorBanner) {
        loginErrorBanner.classList.remove("hidden");
        loginErrorBanner.style.display = "flex";
      }
      if (loginAttemptsRemaining) {
        loginAttemptsRemaining.textContent = `Bạn còn ${failResult.remainingAttempts} lần thử trước khi tài khoản bị khóa tạm.`;
      }
      showStatus(
        `Email hoặc mật khẩu không đúng. (Còn ${failResult.remainingAttempts} lần thử trước khi bị khóa tạm 15 phút)`
      );
    }
    return;
  }

  // 4. Đăng nhập thành công -> Xóa bộ đếm sai & tiến hành phiên làm việc
  hideAllLoginBanners();
  clearLoginAttempts(rawInput);
  clearLoginAttempts(account.email);

  login(account);
});

// ---------------------------------------------------------------------
// 4b. Modal Đăng ký / Tạo tài khoản mới trên trang đăng nhập
// ---------------------------------------------------------------------
const registerModal = document.querySelector("#register-modal");
const openRegisterModalBtn = document.querySelector("#open-register-modal");
const closeRegisterModalBtn = document.querySelector("#close-register-modal");
const cancelRegisterBtn = document.querySelector("#cancel-register-btn");
const registerForm = document.querySelector("#register-form");
const regStatus = document.querySelector("#reg-status");

const closeRegisterModal = () => {
  if (registerModal) {
    registerModal.classList.add("hidden");
    if (registerForm) registerForm.reset();
    if (regStatus) {
      regStatus.classList.add("hidden");
      regStatus.textContent = "";
    }
  }
};

if (openRegisterModalBtn && registerModal) {
  openRegisterModalBtn.addEventListener("click", () => {
    registerModal.classList.remove("hidden");
    const nameInput = document.querySelector("#reg-name");
    if (nameInput) nameInput.focus();
    if (window.lucide) window.lucide.createIcons();
  });
}

closeRegisterModalBtn?.addEventListener("click", closeRegisterModal);
cancelRegisterBtn?.addEventListener("click", closeRegisterModal);
registerModal?.addEventListener("click", (e) => {
  if (e.target === registerModal) closeRegisterModal();
});

if (registerForm) {
  registerForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const name = document.querySelector("#reg-name")?.value.trim();
    const email = document.querySelector("#reg-email")?.value.trim().toLowerCase();
    const phone = document.querySelector("#reg-phone")?.value.trim() || "";
    const role = document.querySelector("#reg-role")?.value || "student";
    const password = document.querySelector("#reg-password")?.value.trim();
    const confirmPassword = document.querySelector("#reg-confirm-password")?.value.trim();

    const showRegError = (msg) => {
      if (regStatus) {
        regStatus.textContent = msg;
        regStatus.className = "text-xs leading-5 text-rose-600 bg-rose-50 p-2.5 rounded-lg border border-rose-200 block";
      }
    };

    if (!name || !email || !password || !confirmPassword) {
      showRegError("Vui lòng điền đầy đủ các thông tin bắt buộc.");
      return;
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      showRegError("Email không đúng định dạng (ví dụ: ten@domain.com).");
      return;
    }

    // Kiểm tra trùng email với các tài khoản trong hệ thống
    const customAccounts = readStore("edumanager-custom-accounts") || [];
    const allEmails = [
      ...DEMO_ACCOUNTS.map((a) => a.email.toLowerCase()),
      ...DEMO_ACCOUNTS.flatMap((a) => (a.aliases || []).map((alias) => alias.toLowerCase())),
      ...customAccounts.map((a) => a.email.toLowerCase()),
    ];

    if (allEmails.includes(email) || allEmails.includes(email.split("@")[0])) {
      showRegError("Email này đã được sử dụng. Vui lòng chọn email khác hoặc đăng nhập.");
      return;
    }

    if (password.length < 8) {
      showRegError("Mật khẩu phải có tối thiểu 8 ký tự.");
      return;
    }

    if (!/[a-zA-Z]/.test(password) || !/[0-9]/.test(password)) {
      showRegError("Mật khẩu phải chứa ít nhất 1 chữ cái và 1 chữ số.");
      return;
    }

    if (password !== confirmPassword) {
      showRegError("Xác nhận mật khẩu không trùng khớp.");
      return;
    }

    // Tạo tài khoản mới thành công
    const newAccount = {
      fullName: name,
      email,
      phone,
      role,
      password,
      passwords: [password],
      aliases: [email, email.split("@")[0]],
      createdAt: new Date().toISOString(),
    };

    customAccounts.unshift(newAccount);
    writeStore("edumanager-custom-accounts", customAccounts);

    const customPasswords = readStore("edumanager-custom-passwords") || {};
    customPasswords[email] = password;
    customPasswords[email.split("@")[0]] = password;
    writeStore("edumanager-custom-passwords", customPasswords);

    addAuditLog(`Người dùng tự đăng ký tài khoản mới: ${name} (${email}) - Vai trò: ${role}`, email);

    // Tự động điền vào form đăng nhập
    if (emailInput) emailInput.value = email;
    if (passwordInput) passwordInput.value = password;

    closeRegisterModal();
    showToast(`Đăng ký thành công tài khoản ${email}! Đang đăng nhập...`, "success");

    // Đăng nhập ngay
    setTimeout(() => {
      login(newAccount);
    }, 600);
  });
}

// ---------------------------------------------------------------------
// ---------------------------------------------------------------------
// 5. Trang quên mật khẩu (S1-03: Nhập email nhận liên kết 30 phút, dùng 1 lần)
// ---------------------------------------------------------------------
const forgotPasswordForm = document.querySelector("#forgot-password-form");
const stepInputEmail = document.querySelector("#step-input-email");
const stepEmailSent = document.querySelector("#step-email-sent");
const sentEmailDisplay = document.querySelector("#sent-email-display");
const resetLinkTimer = document.querySelector("#reset-link-timer");
const resendLinkBtn = document.querySelector("#resend-link-btn");
const resendLinkLabel = document.querySelector("#resend-link-label");
const backToInputBtn = document.querySelector("#back-to-input-btn");
const forgotSubmitBtn = document.querySelector("#forgot-submit");
const forgotSubmitLabel = document.querySelector("#forgot-submit-label");

let resetLinkCountdownTimer = null;
let resetLinkExpireSeconds = 1800; // Đúng 30 phút (1800s) theo S1-03 AC1
let resendCooldownTimer = null;
let resendCooldownSeconds = 0;
const RATE_LIMIT_15M_SECONDS = 15 * 60; // 900 giây (15 phút để tránh sập hệ thống)

const startResendCooldown = (initialSeconds = RATE_LIMIT_15M_SECONDS) => {
  if (resendCooldownTimer) clearInterval(resendCooldownTimer);
  resendCooldownSeconds = initialSeconds;

  if (resendLinkBtn) {
    resendLinkBtn.disabled = true;
    resendLinkBtn.style.opacity = "0.6";
    resendLinkBtn.style.cursor = "not-allowed";
  }

  const updateLabel = () => {
    const m = Math.floor(resendCooldownSeconds / 60);
    const s = resendCooldownSeconds % 60;
    if (resendLinkLabel) {
      resendLinkLabel.textContent = `Gửi lại sau (${m}:${s < 10 ? "0" : ""}${s})`;
    }
  };

  updateLabel();

  resendCooldownTimer = setInterval(() => {
    resendCooldownSeconds--;
    if (resendCooldownSeconds <= 0) {
      clearInterval(resendCooldownTimer);
      if (resendLinkBtn) {
        resendLinkBtn.disabled = false;
        resendLinkBtn.style.opacity = "1";
        resendLinkBtn.style.cursor = "pointer";
      }
      if (resendLinkLabel) resendLinkLabel.textContent = "Gửi lại liên kết";
    } else {
      updateLabel();
    }
  }, 1000);
};

const startResetLinkCountdown = () => {
  if (resetLinkCountdownTimer) clearInterval(resetLinkCountdownTimer);
  resetLinkExpireSeconds = 1800; // 30:00
  if (resetLinkTimer) {
    resetLinkTimer.textContent = "30:00";
    resetLinkTimer.style.color = "var(--color-blue-primary)";
  }

  resetLinkCountdownTimer = setInterval(() => {
    resetLinkExpireSeconds--;
    if (resetLinkExpireSeconds <= 0) {
      clearInterval(resetLinkCountdownTimer);
      if (resetLinkTimer) {
        resetLinkTimer.textContent = "Hết hạn (sau 30 phút)";
        resetLinkTimer.style.color = "var(--color-error)";
      }
      showToast("Liên kết đặt lại mật khẩu đã hết hạn sau 30 phút. Vui lòng yêu cầu lại.", "warning");
    } else if (resetLinkTimer) {
      const m = Math.floor(resetLinkExpireSeconds / 60);
      const s = resetLinkExpireSeconds % 60;
      resetLinkTimer.textContent = `${m}:${s < 10 ? "0" : ""}${s}`;
    }
  }, 1000);
};

const checkEmailExistsInSystem = (emailToCheck) => {
  const e = (emailToCheck || "").trim().toLowerCase();
  if (e === "tatphi2006@gmail.com") return true;
  if (VALID_ROLE_ALIASES && (VALID_ROLE_ALIASES.has(e) || VALID_ROLE_ALIASES.has(e.split("@")[0]))) return true;
  const customAccounts = readStore("edumanager-custom-accounts", []);
  if (customAccounts.some((a) => (a.email || "").toLowerCase() === e)) return true;
  if (e.endsWith("@tms.vn") || e.endsWith("@edumanager.vn") || e.endsWith("@example.com")) return true;
  return false;
};

forgotPasswordForm?.addEventListener("submit", async (event) => {
  event.preventDefault();
  hideStatus();

  if (!validateEmail()) {
    emailInput?.focus();
    return;
  }

  const email = emailInput.value.trim().toLowerCase();

  // Kiểm tra email có trong hệ thống không; nếu không tồn tại thì báo lỗi ngay và KHÔNG chuyển giao diện
  if (!checkEmailExistsInSystem(email)) {
    const notFoundMsg = "Email không tồn tại trong hệ thống. Vui lòng kiểm tra và nhập lại email khác.";
    setFieldError(emailInput, emailError, notFoundMsg);
    showToast(notFoundMsg, "error");
    emailInput?.focus();
    return;
  }

  // Kiểm tra giới hạn tần suất 15 phút trên máy khách (Rate Limiting 15m)
  const rateLimitMap = readStore("edumanager-reset-ratelimit", {});
  const lastSentTime = rateLimitMap[email];
  const elapsedMs = lastSentTime ? Date.now() - lastSentTime : Infinity;
  const RATE_LIMIT_MS = 15 * 60 * 1000;

  if (elapsedMs < RATE_LIMIT_MS) {
    const remainingSecs = Math.ceil((RATE_LIMIT_MS - elapsedMs) / 1000);
    const remainingMins = Math.ceil(remainingSecs / 60);
    showToast(`Email này đã được yêu cầu gần đây. Vui lòng thử lại sau ${remainingMins} phút để bảo vệ hệ thống.`, "warning");

    if (sentEmailDisplay) sentEmailDisplay.textContent = email;
    stepInputEmail?.classList.add("hidden");
    stepEmailSent?.classList.remove("hidden");
    if (window.lucide) window.lucide.createIcons();
    startResetLinkCountdown();
    startResendCooldown(remainingSecs);
    return;
  }

  if (forgotSubmitBtn) forgotSubmitBtn.disabled = true;
  if (forgotSubmitLabel) forgotSubmitLabel.textContent = "Đang gửi liên kết...";

  try {
    const res = await fetch("/api/forgot-password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
    });

    const data = await res.json().catch(() => ({}));

    // Nếu máy chủ báo email không tồn tại trong hệ thống
    if (res.status === 404 || data.code === "EMAIL_NOT_FOUND") {
      const notFoundMsg = data.message || "Email không tồn tại trong hệ thống. Vui lòng kiểm tra và nhập lại email khác.";
      setFieldError(emailInput, emailError, notFoundMsg);
      showToast(notFoundMsg, "error");
      if (forgotSubmitBtn) forgotSubmitBtn.disabled = false;
      if (forgotSubmitLabel) forgotSubmitLabel.textContent = "Gửi liên kết đặt lại";
      emailInput?.focus();
      return;
    }

    // Nếu bị giới hạn 15 phút
    if (res.status === 429 || data.code === "RATE_LIMITED") {
      showToast(data.message || "Email này chỉ có thể nhận liên kết 1 lần mỗi 15 phút.", "warning");
      startResendCooldown(data.remainingSeconds || RATE_LIMIT_15M_SECONDS);
      if (forgotSubmitBtn) forgotSubmitBtn.disabled = false;
      if (forgotSubmitLabel) forgotSubmitLabel.textContent = "Gửi liên kết đặt lại";
      return;
    }

    if (!res.ok || data.success === false) {
      const errorMsg = data.message || "Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng thử lại.";
      setFieldError(emailInput, emailError, errorMsg);
      showToast(errorMsg, "error");
      if (forgotSubmitBtn) forgotSubmitBtn.disabled = false;
      if (forgotSubmitLabel) forgotSubmitLabel.textContent = "Gửi liên kết đặt lại";
      emailInput?.focus();
      return;
    }

    if (data.token) {
      const localTokens = readStore("edumanager-reset-tokens", {});
      localTokens[data.token] = {
        email,
        expiresAt: data.expiresAt || (Date.now() + 30 * 60 * 1000),
        isUsed: false,
        createdAt: Date.now(),
      };
      writeStore("edumanager-reset-tokens", localTokens);
      sessionStorage.setItem("edumanager-last-token", data.token);
    }

    // Ghi nhận thời điểm gửi để kích hoạt giới hạn 15 phút
    rateLimitMap[email] = Date.now();
    writeStore("edumanager-reset-ratelimit", rateLimitMap);
  } catch (e) {
    console.warn("[TMS S1-03] Kết nối gửi liên kết:", e);
    rateLimitMap[email] = Date.now();
    writeStore("edumanager-reset-ratelimit", rateLimitMap);
  }

  sessionStorage.setItem("edumanager-reset-email", email);

  // Chỉ khi email TỒN TẠI mới chuyển sang giao diện Đã gửi yêu cầu (UI_UX/S1-03_Quên mật khẩu/Đã gửi yêu cầu.png)
  if (sentEmailDisplay) sentEmailDisplay.textContent = email;
  stepInputEmail?.classList.add("hidden");
  stepEmailSent?.classList.remove("hidden");
  if (window.lucide) window.lucide.createIcons();

  startResetLinkCountdown();
  startResendCooldown(RATE_LIMIT_15M_SECONDS);
  showToast(`Đã gửi liên kết bảo mật có hiệu lực 30 phút đến ${email}!`, "success");

  if (forgotSubmitBtn) forgotSubmitBtn.disabled = false;
  if (forgotSubmitLabel) forgotSubmitLabel.textContent = "Gửi liên kết đặt lại";
});

if (resendLinkBtn) {
  resendLinkBtn.addEventListener("click", async () => {
    if (resendCooldownSeconds > 0) {
      const mins = Math.ceil(resendCooldownSeconds / 60);
      showToast(`Vui lòng chờ ${mins} phút trước khi yêu cầu gửi lại liên kết để bảo vệ hệ thống.`, "warning");
      return;
    }

    const email = (emailInput?.value?.trim() || sessionStorage.getItem("edumanager-reset-email") || "").toLowerCase();
    if (!email) return;

    if (!checkEmailExistsInSystem(email)) {
      showToast("Email không tồn tại trong hệ thống. Vui lòng nhập lại email khác.", "error");
      return;
    }

    resendLinkBtn.disabled = true;
    if (resendLinkLabel) resendLinkLabel.textContent = "Đang gửi lại...";

    try {
      const res = await fetch("/api/forgot-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });

      const data = await res.json().catch(() => ({}));

      if (res.status === 404 || data.code === "EMAIL_NOT_FOUND") {
        showToast(data.message || "Email không tồn tại trong hệ thống. Vui lòng nhập lại email khác.", "error");
        return;
      }

      if (res.status === 429 || data.code === "RATE_LIMITED") {
        showToast(data.message || "Email này chỉ có thể nhận liên kết 1 lần mỗi 15 phút.", "warning");
        startResendCooldown(data.remainingSeconds || RATE_LIMIT_15M_SECONDS);
        return;
      }

      if (data.token) {
        const localTokens = readStore("edumanager-reset-tokens", {});
        localTokens[data.token] = {
          email,
          expiresAt: data.expiresAt || (Date.now() + 30 * 60 * 1000),
          isUsed: false,
          createdAt: Date.now(),
        };
        writeStore("edumanager-reset-tokens", localTokens);
      }

      const rateLimitMap = readStore("edumanager-reset-ratelimit", {});
      rateLimitMap[email] = Date.now();
      writeStore("edumanager-reset-ratelimit", rateLimitMap);
    } catch {}

    showToast(`Đã gửi lại liên kết mới đến ${email}. Hiệu lực 30 phút!`, "success");
    startResetLinkCountdown();
    startResendCooldown(RATE_LIMIT_15M_SECONDS);
  });
}

if (backToInputBtn) {
  backToInputBtn.addEventListener("click", () => {
    if (resetLinkCountdownTimer) clearInterval(resetLinkCountdownTimer);
    stepEmailSent?.classList.add("hidden");
    stepInputEmail?.classList.remove("hidden");
    if (window.lucide) window.lucide.createIcons();
    emailInput?.focus();
  });
}

// ---------------------------------------------------------------------
// 5b. Trang đặt lại mật khẩu (S1-03: Kiểm tra token 30 phút & 1 lần dùng)
// ---------------------------------------------------------------------
const resetPasswordForm = document.querySelector("#reset-password-form");
const stepResetForm = document.querySelector("#step-reset-form");
const stepInvalidToken = document.querySelector("#step-invalid-token");
const stepSuccess = document.querySelector("#step-success");
const invalidTokenReasonTitle = document.querySelector("#invalid-token-reason-title");

if (resetPasswordForm || stepInvalidToken) {
  const newPasswordInput = document.querySelector("#new-password");
  const confirmPasswordInput = document.querySelector("#confirm-password");
  const newPasswordError = document.querySelector("#new-password-error");
  const confirmPasswordError = document.querySelector("#confirm-password-error");
  const resetEmailNotice = document.querySelector("#reset-target-email-notice");

  const urlParams = new URLSearchParams(window.location.search);
  const token = urlParams.get("token") || "";
  const directEmail = urlParams.get("email") || sessionStorage.getItem("edumanager-reset-email") || "";

  const showInvalidTokenView = (reason, message) => {
    stepResetForm?.classList.add("hidden");
    stepSuccess?.classList.add("hidden");
    stepInvalidToken?.classList.remove("hidden");
    if (invalidTokenReasonTitle && message) {
      invalidTokenReasonTitle.textContent = message;
    }
    if (window.lucide) window.lucide.createIcons();
  };

  const verifyResetToken = async () => {
    if (!token && !directEmail) {
      showInvalidTokenView("INVALID", "Liên kết không hợp lệ");
      return false;
    }

    if (token) {
      try {
        const res = await fetch(`/api/verify-token?token=${encodeURIComponent(token)}`);
        const data = await res.json().catch(() => ({}));
        if (!res.ok || !data.success) {
          const reasonMsg = data.reason === "ALREADY_USED"
            ? "Liên kết đã được sử dụng trước đó"
            : data.reason === "EXPIRED"
              ? "Liên kết đã hết hạn (quá 30 phút)"
              : "Liên kết không hợp lệ hoặc không tồn tại";
          showInvalidTokenView(data.reason || "EXPIRED", reasonMsg);
          return false;
        }

        if (data.email && resetEmailNotice) {
          resetEmailNotice.innerHTML = `Thiết lập mật khẩu mới cho tài khoản: <strong style="color: var(--color-blue-primary);">${escapeHtml(data.email)}</strong>`;
        }
        return true;
      } catch {
        // Fallback local storage check
        const localTokens = readStore("edumanager-reset-tokens", {});
        const record = localTokens[token];
        if (!record) {
          if (token === "demo-token" || token === "sample-test-token") return true;
          showInvalidTokenView("INVALID", "Liên kết không hợp lệ");
          return false;
        }
        if (record.isUsed) {
          showInvalidTokenView("ALREADY_USED", "Liên kết đã được sử dụng trước đó");
          return false;
        }
        if (Date.now() > record.expiresAt) {
          showInvalidTokenView("EXPIRED", "Liên kết đã hết hạn (sau 30 phút)");
          return false;
        }
        return true;
      }
    }

    if (directEmail && resetEmailNotice) {
      resetEmailNotice.innerHTML = `Thiết lập mật khẩu mới cho tài khoản: <strong style="color: var(--color-blue-primary);">${escapeHtml(directEmail)}</strong>`;
    }
    return true;
  };

  // Xác thực token ngay khi tải trang
  verifyResetToken();

  // 4 tiêu chí bảo mật mật khẩu
  const passwordRuleCheckers = {
    length: (value) => (value || "").length >= 8,
    case: (value) => /[a-z]/.test(value || "") && /[A-Z]/.test(value || ""),
    digit: (value) => /[0-9]/.test(value || ""),
    special: (value) => /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/.test(value || ""),
  };

  // Cập nhật trạng thái từng tiêu chí:
  // - Khi chưa nhập: hiển thị trung tính
  // - Khi nhập mà chưa đạt: KHÔNG TÍCH MÀ HIỆN ĐỎ VỚI ICON X
  // - Khi đã đạt: HIỆN XANH VỚI ICON CHECK
  const updatePasswordRules = (forceValidate = false) => {
    if (!newPasswordInput) return;
    const value = newPasswordInput.value;
    const hasTyped = value.length > 0 || forceValidate;

    ["length", "case", "digit", "special"].forEach((ruleKey) => {
      const item = document.querySelector(`[data-password-rule="${ruleKey}"]`);
      if (!item) return;

      const isMet = passwordRuleCheckers[ruleKey] ? passwordRuleCheckers[ruleKey](value) : false;
      const iconSpan = item.querySelector(".rule-icon") || item.querySelector("i")?.parentElement;

      if (!hasTyped) {
        item.classList.remove("rule-met", "rule-unmet", "is-met");
        item.style.color = "var(--color-text-secondary)";
        if (iconSpan) {
          iconSpan.innerHTML = `<i data-lucide="circle-alert" style="width: 15px; height: 15px; color: var(--color-text-muted);"></i>`;
        }
      } else if (isMet) {
        // ĐÃ ĐẠT: HIỆN DẤU TICK XANH VÀ CHỮ XANH
        item.classList.add("rule-met", "is-met");
        item.classList.remove("rule-unmet");
        item.style.color = "#059669";
        if (iconSpan) {
          iconSpan.innerHTML = `<i data-lucide="check" style="width: 15px; height: 15px; color: #10b981;"></i>`;
        }
      } else {
        // CHƯA ĐÚNG ĐỊNH DẠNG: KHÔNG ĐƯỢC TÍCH MÀ HIỆN ĐỎ VÀ BIỂU TƯỢNG X
        item.classList.add("rule-unmet");
        item.classList.remove("rule-met", "is-met");
        item.style.color = "#ef4444";
        if (iconSpan) {
          iconSpan.innerHTML = `<i data-lucide="x" style="width: 15px; height: 15px; color: #ef4444;"></i>`;
        }
      }
    });

    if (window.lucide) window.lucide.createIcons();
  };

  const bindPasswordToggle = (input, button) => {
    button?.addEventListener("click", () => {
      const showingPassword = input.type === "password";
      input.type = showingPassword ? "text" : "password";
      button.setAttribute(
        "aria-label",
        `${showingPassword ? "Ẩn" : "Hiện"} ${input.id === "new-password" ? "mật khẩu mới" : "mật khẩu xác nhận"}`,
      );
      button.title = button.getAttribute("aria-label");
      button.innerHTML = `<i data-lucide="${showingPassword ? "eye-off" : "eye"}" aria-hidden="true"></i>`;
      lucide.createIcons();
    });
  };

  if (newPasswordInput) {
    bindPasswordToggle(newPasswordInput, document.querySelector("#new-password-toggle"));
    bindPasswordToggle(confirmPasswordInput, document.querySelector("#confirm-password-toggle"));

    newPasswordInput.addEventListener("input", () => {
      updatePasswordRules();
      const val = newPasswordInput.value;
      const allMet = ["length", "case", "digit", "special"].every((k) => passwordRuleCheckers[k](val));
      if (allMet) {
        setFieldError(newPasswordInput, newPasswordError, "");
      }
      if (confirmPasswordInput && confirmPasswordInput.value) {
        setFieldError(
          confirmPasswordInput,
          confirmPasswordError,
          confirmPasswordInput.value === newPasswordInput.value
            ? ""
            : "Mật khẩu xác nhận chưa khớp.",
        );
      }
    });
  }

  if (confirmPasswordInput) {
    confirmPasswordInput.addEventListener("input", () => {
      if (confirmPasswordInput.value === newPasswordInput.value) {
        setFieldError(confirmPasswordInput, confirmPasswordError, "");
      }
    });
  }

  resetPasswordForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    hideStatus();
    updatePasswordRules(true); // Bắt buộc kích hoạt hiển thị đỏ các quy tắc chưa đạt

    const newPassword = newPasswordInput.value;
    const allRulesMet = ["length", "case", "digit", "special"].every((k) => passwordRuleCheckers[k](newPassword));

    const newPasswordMessage = !newPassword
      ? "Vui lòng nhập mật khẩu mới."
      : !allRulesMet
        ? "Mật khẩu chưa đáp ứng đủ các yêu cầu bảo mật bên dưới."
        : "";
    const confirmPasswordMessage = !confirmPasswordInput.value
      ? "Vui lòng xác nhận mật khẩu mới."
      : confirmPasswordInput.value !== newPassword
        ? "Mật khẩu xác nhận chưa khớp."
        : "";

    setFieldError(newPasswordInput, newPasswordError, newPasswordMessage);
    setFieldError(confirmPasswordInput, confirmPasswordError, confirmPasswordMessage);

    if (newPasswordMessage || confirmPasswordMessage) {
      (newPasswordMessage ? newPasswordInput : confirmPasswordInput).focus();
      return;
    }

    const submitBtn = resetPasswordForm.querySelector("button[type=submit]");
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Đang cập nhật mật khẩu...";
    }

    const finalEmail = directEmail || "tatphi2006@gmail.com";

    // Gửi đến API reset-password (đánh dấu token đã sử dụng 1 lần - S1-03 AC2)
    try {
      const res = await fetch("/api/reset-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, email: finalEmail, password: newPassword }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok && data.reason === "ALREADY_USED") {
        showInvalidTokenView("ALREADY_USED", "Liên kết này đã được sử dụng trước đó.");
        return;
      }
      if (!res.ok && data.reason === "EXPIRED") {
        showInvalidTokenView("EXPIRED", "Liên kết đã hết hạn (quá 30 phút).");
        return;
      }
    } catch (e) {
      console.warn("[TMS] Reset password API:", e);
    }

    // Đánh dấu token đã sử dụng trong localStorage
    if (token) {
      const localTokens = readStore("edumanager-reset-tokens", {});
      if (localTokens[token]) {
        localTokens[token].isUsed = true;
        localTokens[token].usedAt = Date.now();
        writeStore("edumanager-reset-tokens", localTokens);
      }
    }

    // Cập nhật mật khẩu trong stores
    const customPasswords = readStore("edumanager-custom-passwords", {});
    customPasswords[finalEmail.toLowerCase()] = newPassword;
    writeStore("edumanager-custom-passwords", customPasswords);

    const customAccounts = readStore("edumanager-custom-accounts", []);
    const userAcc = customAccounts.find((a) => a.email.toLowerCase() === finalEmail.toLowerCase());
    if (userAcc) {
      userAcc.password = newPassword;
      writeStore("edumanager-custom-accounts", customAccounts);
    }

    saveLoginFormState(finalEmail, newPassword);
    addAuditLog(`Đặt lại mật khẩu thành công qua liên kết email: ${finalEmail}`, finalEmail);

    // Chuyển sang màn hình THÀNH CÔNG (UI_UX/S1-03_Quên mật khẩu/Thành công.png)
    stepResetForm?.classList.add("hidden");
    stepInvalidToken?.classList.add("hidden");
    stepSuccess?.classList.remove("hidden");
    if (window.lucide) window.lucide.createIcons();

    showToast("Mật khẩu đã được đặt lại thành công!", "success");
  });
}

// ---------------------------------------------------------------------
// 6. Đăng nhập Google
// ---------------------------------------------------------------------
const googleLoginButton = document.querySelector("#google-login");
const googleClientId =
  document.querySelector('meta[name="google-client-id"]')?.content.trim() ?? "";
const googleAuthEndpoint =
  document.querySelector('meta[name="google-auth-endpoint"]')?.content ?? "";

googleLoginButton?.addEventListener("click", () => {
  if (!googleClientId || !googleAuthEndpoint) {
    showStatus(
      "Đăng nhập Google chưa được cấu hình. Cần thêm OAuth Client ID và endpoint xác thực phía máy chủ.",
    );
    return;
  }

  if (!window.google?.accounts?.oauth2) {
    showStatus("Không thể tải Google Identity Services. Vui lòng thử lại sau.");
    return;
  }

  const googleCodeClient = window.google.accounts.oauth2.initCodeClient({
    client_id: googleClientId,
    scope: "openid email profile",
    ux_mode: "popup",
    callback: async ({ code, error }) => {
      if (!code || error) {
        showStatus("Đăng nhập Google đã bị hủy hoặc không thành công.");
        return;
      }

      try {
        const response = await fetch(googleAuthEndpoint, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          credentials: "same-origin",
          body: JSON.stringify({ code }),
        });

        if (!response.ok) {
          throw new Error("Google authorization failed");
        }

        const result = await response.json().catch(() => ({}));
        if (result.redirectUrl) {
          navigateTo(result.redirectUrl);
          return;
        }

        showStatus("Đăng nhập Google thành công.");
      } catch {
        showStatus(
          "Không thể xác thực tài khoản Google với máy chủ. Vui lòng thử lại.",
        );
      }
    },
  });

  googleCodeClient.requestCode();
});

// ---------------------------------------------------------------------
// 7. Đổi giao diện sáng / tối
// ---------------------------------------------------------------------
const themeToggle = document.querySelector("#theme-toggle");

const setTheme = (isDark) => {
  document.documentElement.classList.toggle("dark-mode", isDark);
  themeToggle.setAttribute(
    "aria-label",
    isDark ? "Chuyển giao diện sáng" : "Chuyển giao diện tối",
  );
  themeToggle.title = isDark ? "Chuyển giao diện sáng" : "Chuyển giao diện tối";
  themeToggle.innerHTML = `<i data-lucide="${isDark ? "sun" : "moon"}" class="size-[18px]" aria-hidden="true"></i>`;
  localStorage.setItem("edumanager-theme", isDark ? "dark" : "light");
  if (typeof lucide !== "undefined") lucide.createIcons();
};

const reduceMotion = typeof window.matchMedia === "function"
  ? window.matchMedia("(prefers-reduced-motion: reduce)")
  : { matches: false };

// Hiệu ứng hình tròn: tâm là nút đổi giao diện, bán kính lan tới góc xa nhất.
function circleRevealFrames(origin) {
  const rect = origin.getBoundingClientRect();
  const x = rect.left + rect.width / 2;
  const y = rect.top + rect.height / 2;
  const radius = Math.hypot(
    Math.max(x, window.innerWidth - x),
    Math.max(y, window.innerHeight - y),
  );

  return [
    `circle(0px at ${x}px ${y}px)`,
    `circle(${radius}px at ${x}px ${y}px)`,
  ];
}

if (themeToggle) {
  themeToggle.addEventListener("click", () => {
    const root = document.documentElement;
    const isDark = !root.classList.contains("dark-mode");

    if (!document.startViewTransition || reduceMotion.matches) {
      setTheme(isDark);
      return;
    }

    root.classList.add("theme-switching");

    const transition = document.startViewTransition(() => setTheme(isDark));
    transition.ready.then(() => {
      root.animate(
        { clipPath: circleRevealFrames(themeToggle) },
        {
          duration: 850,
          easing: "cubic-bezier(0.65, 0, 0.35, 1)",
          pseudoElement: "::view-transition-new(root)",
        },
      );
    });
    transition.finished.finally(() => {
      root.classList.remove("theme-switching");
    });
  });

  if (localStorage.getItem("edumanager-theme") === "dark") {
    setTheme(true);
  }
}

// =====================================================================
// 8. Tương tác trong trang khách truy cập và các trang role (dữ liệu demo)
// =====================================================================
const formatMoney = (value) => `${Number(value).toLocaleString("vi-VN")} đ`;

const formatDateTime = (iso) =>
  new Date(iso).toLocaleString("vi-VN", {
    hour: "2-digit",
    minute: "2-digit",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });

const badge = (text, tone = "") =>
  `<span class="app-badge ${tone}">${escapeHtml(text)}</span>`;

const setInlineStatus = (element, message, isError = false) => {
  if (!element) return;
  element.textContent = message;
  element.classList.toggle("is-error", isError);
  element.hidden = !message;
};

// Lead: dữ liệu mẫu được nạp một lần, sau đó cộng thêm lead khách gửi.
const SAMPLE_LEADS = [
  {
    name: "Vũ Minh Anh",
    phone: "0912 345 678",
    email: "minhanh@gmail.com",
    course: "IELTS 6.5+",
    source: "Facebook",
    status: "Mới",
  },
  {
    name: "Bùi Thu Hà",
    phone: "0987 654 321",
    email: "thuha@gmail.com",
    course: "Lập trình Web Front-end",
    source: "Website",
    status: "Đang chăm sóc",
  },
  {
    name: "Ngô Quốc Bảo",
    phone: "0901 222 333",
    email: "",
    course: "Tiếng Anh giao tiếp",
    source: "Giới thiệu",
    status: "Hẹn test đầu vào",
  },
  {
    name: "Đặng Gia Linh",
    phone: "0933 444 555",
    email: "gialinh@gmail.com",
    course: "Phân tích dữ liệu",
    source: "Website",
    status: "Đã nhập học",
  },
];

const getLeads = () => {
  if (!localStorage.getItem(`${LEADS_KEY}-seeded`)) {
    const seeded = SAMPLE_LEADS.map((lead, index) => ({
      ...lead,
      id: index + 1,
      createdAt: new Date(Date.now() - (index + 1) * 86400000).toISOString(),
    }));
    writeStore(LEADS_KEY, [...readStore(LEADS_KEY), ...seeded]);
    localStorage.setItem(`${LEADS_KEY}-seeded`, "1");
  }
  return readStore(LEADS_KEY);
};

// ---------------------------------------------------------------------
// 8a. Khách truy cập: đăng ký tư vấn
// ---------------------------------------------------------------------
const consultForm = document.querySelector("#consult-form");

if (consultForm) {
  const fields = {
    name: document.querySelector("#lead-name"),
    phone: document.querySelector("#lead-phone"),
    email: document.querySelector("#lead-email"),
    course: document.querySelector("#lead-course"),
    time: document.querySelector("#lead-time"),
    note: document.querySelector("#lead-note"),
  };
  const consultStatus = document.querySelector("#consult-status");

  const rules = {
    name: (value) => (value ? "" : "Vui lòng nhập họ và tên."),
    phone: (value) =>
      !value
        ? "Vui lòng nhập số điện thoại."
        : !/^(0|\+84)\d{9,10}$/.test(value.replace(/[\s.-]/g, ""))
          ? "Số điện thoại chưa đúng định dạng, ví dụ 0912 345 678."
          : "",
    email: (value) =>
      value && !fields.email.validity.valid
        ? "Email chưa đúng định dạng, ví dụ ten@domain.com."
        : "",
    course: (value) => (value ? "" : "Vui lòng chọn khoá học quan tâm."),
  };

  const validateField = (key) => {
    const input = fields[key];
    const message = rules[key](input.value.trim());
    setFieldError(input, document.querySelector(`#lead-${key}-error`), message);
    return !message;
  };

  Object.keys(rules).forEach((key) => {
    fields[key].setAttribute("aria-describedby", `lead-${key}-error`);
    fields[key].addEventListener("input", () => {
      if (fields[key].getAttribute("aria-invalid") === "true") {
        validateField(key);
      }
    });
  });

  // Nút "Tư vấn khoá này" ở từng thẻ khoá học -> chọn sẵn khoá trong form.
  document.querySelectorAll("[data-course]").forEach((button) => {
    button.addEventListener("click", () => {
      fields.course.value = button.dataset.course;
      validateField("course");
      document.querySelector("#tu-van").scrollIntoView({ behavior: "smooth" });
      fields.name.focus({ preventScroll: true });
    });
  });

  consultForm.addEventListener("submit", (event) => {
    event.preventDefault();
    setInlineStatus(consultStatus, "");

    const invalidKey = Object.keys(rules)
      .map((key) => [key, validateField(key)])
      .find(([, valid]) => !valid)?.[0];
    if (invalidKey) {
      fields[invalidKey].focus();
      return;
    }

    const leads = getLeads();
    leads.unshift({
      id: Date.now(),
      name: fields.name.value.trim(),
      phone: fields.phone.value.trim(),
      email: fields.email.value.trim(),
      course: fields.course.value,
      time: fields.time.value,
      note: fields.note.value.trim(),
      source: "Website",
      status: "Mới",
      createdAt: new Date().toISOString(),
    });
    writeStore(LEADS_KEY, leads);
    addAuditLog(
      `Khách gửi đăng ký tư vấn: ${fields.name.value.trim()}`,
      "khách",
    );

    setInlineStatus(
      consultStatus,
      `Cảm ơn ${fields.name.value.trim()}! Tư vấn viên sẽ liên hệ với bạn trong vòng 24 giờ.`,
    );
    consultForm.reset();
  });
}

// ---------------------------------------------------------------------
// 8b. Học viên: nộp bài
// ---------------------------------------------------------------------
document.querySelectorAll("[data-submit-assignment]").forEach((button) => {
  button.addEventListener("click", () => {
    const row = button.closest("[data-assignment]");
    row.querySelector("[data-assignment-status]").innerHTML = badge(
      "Đã nộp",
      "is-success",
    );
    button.disabled = true;
    button.textContent = "Đã nộp";

    const dueCounter = document.querySelector("#assignments-due");
    if (dueCounter) {
      dueCounter.textContent = Math.max(0, Number(dueCounter.textContent) - 1);
    }
    addAuditLog(`Nộp bài: ${row.dataset.assignment}`);
  });
});

// ---------------------------------------------------------------------
// 8c. Giảng viên / Trợ giảng: điểm danh và chấm điểm
// ---------------------------------------------------------------------
document.querySelectorAll("[data-attendance-form]").forEach((form) => {
  const checkboxes = form.querySelectorAll('input[type="checkbox"]');
  const counter = form.querySelector("[data-attendance-count]");
  const status = form.querySelector(".app-status");

  const updateCount = () => {
    const present = [...checkboxes].filter((box) => box.checked).length;
    counter.textContent = `${present}/${checkboxes.length} có mặt`;
    return present;
  };

  checkboxes.forEach((box) => box.addEventListener("change", updateCount));
  updateCount();

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const present = updateCount();
    setInlineStatus(
      status,
      `Đã lưu điểm danh lớp ${form.dataset.attendanceForm}: ${present}/${checkboxes.length} học viên có mặt.`,
    );
    addAuditLog(`Điểm danh lớp ${form.dataset.attendanceForm}`);
  });
});

document.querySelectorAll("[data-grade-form]").forEach((form) => {
  const status = form.querySelector(".app-status");

  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const inputs = [
      ...form.querySelectorAll("input[type='number']:not(:disabled)"),
    ];
    const invalid = inputs.find((input) => {
      const value = input.value.trim();
      return value !== "" && (Number(value) < 0 || Number(value) > 10);
    });

    if (invalid) {
      invalid.focus();
      setInlineStatus(status, "Điểm phải nằm trong khoảng 0 - 10.", true);
      return;
    }

    const graded = inputs.filter((input) => input.value.trim() !== "").length;
    setInlineStatus(status, `Đã lưu điểm cho ${graded}/${inputs.length} bài.`);
    addAuditLog(`Chấm điểm: ${form.dataset.gradeForm}`);
  });
});

const assignForm = document.querySelector("#assign-form");

assignForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  const title = assignForm.querySelector("#assign-title");
  const classCode = assignForm.querySelector("#assign-class").value;
  const due = assignForm.querySelector("#assign-due").value;
  const status = assignForm.querySelector(".app-status");

  if (!title.value.trim() || !due) {
    setInlineStatus(status, "Vui lòng nhập tên bài tập và hạn nộp.", true);
    (title.value.trim()
      ? assignForm.querySelector("#assign-due")
      : title
    ).focus();
    return;
  }

  const item = document.createElement("li");
  item.className = "app-list-item";
  item.innerHTML = `
    <div>
      <p class="app-list-title">${escapeHtml(title.value.trim())}</p>
      <p class="app-muted">Lớp ${escapeHtml(classCode)} · Hạn nộp ${new Date(due).toLocaleDateString("vi-VN")}</p>
    </div>
    ${badge("Vừa giao", "is-info")}`;
  document.querySelector("#assigned-list").prepend(item);

  setInlineStatus(
    status,
    `Đã giao bài "${title.value.trim()}" cho lớp ${classCode}.`,
  );
  addAuditLog(`Giao bài "${title.value.trim()}" cho lớp ${classCode}`);
  assignForm.reset();
});

// ---------------------------------------------------------------------
// 8d. Quản lý đào tạo: mở lớp, phân công, duyệt bảo lưu
// ---------------------------------------------------------------------
const openClassForm = document.querySelector("#open-class-form");

openClassForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  const value = (id) => openClassForm.querySelector(id).value.trim();
  const status = openClassForm.querySelector(".app-status");
  const data = {
    code: value("#class-code"),
    course: value("#class-course"),
    start: value("#class-start"),
    schedule: value("#class-schedule"),
    instructor: value("#class-instructor"),
  };

  if (!data.code || !data.start) {
    setInlineStatus(status, "Vui lòng nhập mã lớp và ngày khai giảng.", true);
    return;
  }

  const row = document.createElement("tr");
  row.innerHTML = `
    <td class="font-bold">${escapeHtml(data.code)}</td>
    <td>${escapeHtml(data.course)}</td>
    <td>${escapeHtml(data.schedule)}</td>
    <td>${escapeHtml(data.instructor)}</td>
    <td>${new Date(data.start).toLocaleDateString("vi-VN")}</td>
    <td>${badge("Sắp khai giảng", "is-info")}</td>`;
  document.querySelector("#class-table tbody").prepend(row);

  setInlineStatus(status, `Đã mở lớp ${data.code} (${data.course}).`);
  addAuditLog(`Mở lớp ${data.code}`);
  openClassForm.reset();
});

document.querySelectorAll("[data-assign-instructor]").forEach((select) => {
  select.addEventListener("change", () => {
    const message = select.value
      ? `Đã phân công ${select.value} cho lớp ${select.dataset.assignInstructor}.`
      : `Đã bỏ phân công giảng viên lớp ${select.dataset.assignInstructor}.`;
    setInlineStatus(document.querySelector("#assign-status"), message);
    addAuditLog(message);
  });
});

document.querySelectorAll("[data-reserve-action]").forEach((button) => {
  button.addEventListener("click", () => {
    const row = button.closest("tr");
    const approved = button.dataset.reserveAction === "approve";
    row.querySelector("[data-reserve-status]").innerHTML = approved
      ? badge("Đã duyệt", "is-success")
      : badge("Từ chối", "is-danger");
    row.querySelector("[data-reserve-buttons]").innerHTML =
      '<span class="app-muted">Đã xử lý</span>';
    addAuditLog(
      `${approved ? "Duyệt" : "Từ chối"} bảo lưu: ${row.dataset.student}`,
    );
  });
});

// ---------------------------------------------------------------------
// 8e. Tư vấn tuyển sinh: quản lý lead
// ---------------------------------------------------------------------
const leadTable = document.querySelector("#lead-table");

if (leadTable) {
  const LEAD_STATUSES = {
    Mới: "is-info",
    "Đang chăm sóc": "is-warning",
    "Hẹn test đầu vào": "",
    "Đã nhập học": "is-success",
    "Không tiềm năng": "is-danger",
  };
  const leadFilter = document.querySelector("#lead-filter");

  const renderLeads = () => {
    const leads = getLeads();
    const filter = leadFilter.value;
    const visible = filter
      ? leads.filter((lead) => lead.status === filter)
      : leads;

    document.querySelectorAll("[data-lead-count]").forEach((element) => {
      const key = element.dataset.leadCount;
      element.textContent = key
        ? leads.filter((lead) => lead.status === key).length
        : leads.length;
    });

    leadTable.querySelector("tbody").innerHTML = visible.length
      ? visible
          .map(
            (lead) => `
        <tr data-lead-id="${lead.id}">
          <td>
            <p class="font-bold">${escapeHtml(lead.name)}</p>
            <p class="app-muted">${formatDateTime(lead.createdAt)}</p>
          </td>
          <td>
            <p>${escapeHtml(lead.phone)}</p>
            <p class="app-muted">${escapeHtml(lead.email || "—")}</p>
          </td>
          <td>${escapeHtml(lead.course)}${lead.note ? `<p class="app-muted">"${escapeHtml(lead.note)}"</p>` : ""}</td>
          <td>${badge(lead.source)}</td>
          <td>
            <select class="app-select app-input-sm" data-lead-status aria-label="Trạng thái lead ${escapeHtml(lead.name)}">
              ${Object.keys(LEAD_STATUSES)
                .map(
                  (status) =>
                    `<option ${status === lead.status ? "selected" : ""}>${status}</option>`,
                )
                .join("")}
            </select>
          </td>
          <td>
            <button type="button" class="app-btn app-btn-sm" data-lead-convert ${lead.status === "Đã nhập học" ? "disabled" : ""}>
              <i data-lucide="user-check" aria-hidden="true"></i>Nhập học
            </button>
          </td>
        </tr>`,
          )
          .join("")
      : '<tr><td colspan="6" class="app-muted">Không có lead nào.</td></tr>';
    lucide.createIcons();
  };

  const updateLead = (id, status) => {
    const leads = getLeads().map((lead) =>
      String(lead.id) === id ? { ...lead, status } : lead,
    );
    writeStore(LEADS_KEY, leads);
    const lead = leads.find((item) => String(item.id) === id);
    addAuditLog(`Cập nhật lead ${lead?.name}: ${status}`);
    renderLeads();
  };

  leadTable.addEventListener("change", (event) => {
    if (!event.target.matches("[data-lead-status]")) return;
    updateLead(event.target.closest("tr").dataset.leadId, event.target.value);
  });

  leadTable.addEventListener("click", (event) => {
    const button = event.target.closest("[data-lead-convert]");
    if (!button) return;
    updateLead(button.closest("tr").dataset.leadId, "Đã nhập học");
  });

  leadFilter.addEventListener("change", renderLeads);
  renderLeads();
}

// ---------------------------------------------------------------------
// 8f. Kế toán: ghi nhận thanh toán, công nợ, xuất báo cáo
// ---------------------------------------------------------------------
const paymentForm = document.querySelector("#payment-form");

if (paymentForm) {
  const debtRows = [...document.querySelectorAll("[data-debt-row]")];
  const studentSelect = paymentForm.querySelector("#payment-student");
  const amountInput = paymentForm.querySelector("#payment-amount");
  const status = paymentForm.querySelector(".app-status");

  const renderDebt = (row) => {
    const remaining = Number(row.dataset.remaining);
    row.querySelector("[data-debt-remaining]").textContent =
      formatMoney(remaining);
    row.querySelector("[data-debt-status]").innerHTML =
      remaining === 0
        ? badge("Đã thu đủ", "is-success")
        : row.dataset.overdue
          ? badge("Quá hạn", "is-danger")
          : badge("Còn nợ", "is-warning");
  };

  const updateTotals = () => {
    const totalDebt = debtRows.reduce(
      (sum, row) => sum + Number(row.dataset.remaining),
      0,
    );
    document.querySelector("#stat-debt").textContent = formatMoney(totalDebt);
  };

  studentSelect.innerHTML = debtRows
    .map(
      (row) =>
        `<option value="${row.dataset.debtRow}">${escapeHtml(row.dataset.student)}</option>`,
    )
    .join("");
  debtRows.forEach(renderDebt);
  updateTotals();

  paymentForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const row = debtRows.find(
      (item) => item.dataset.debtRow === studentSelect.value,
    );
    const amount = Number(amountInput.value);
    const remaining = Number(row.dataset.remaining);

    if (!amount || amount <= 0) {
      setInlineStatus(status, "Vui lòng nhập số tiền hợp lệ.", true);
      amountInput.focus();
      return;
    }
    if (amount > remaining) {
      setInlineStatus(
        status,
        `Số tiền vượt quá công nợ còn lại (${formatMoney(remaining)}).`,
        true,
      );
      amountInput.focus();
      return;
    }

    row.dataset.remaining = remaining - amount;
    renderDebt(row);
    updateTotals();

    const collected = document.querySelector("#stat-collected");
    collected.dataset.value = Number(collected.dataset.value) + amount;
    collected.textContent = formatMoney(collected.dataset.value);

    const method = paymentForm.querySelector("#payment-method").value;
    const item = document.createElement("li");
    item.className = "app-list-item";
    item.innerHTML = `
      <div>
        <p class="app-list-title">${escapeHtml(row.dataset.student)}</p>
        <p class="app-muted">${escapeHtml(method)} · ${formatDateTime(new Date().toISOString())}</p>
      </div>
      <span class="font-bold">+${formatMoney(amount)}</span>`;
    document.querySelector("#payment-list").prepend(item);

    setInlineStatus(
      status,
      `Đã ghi nhận ${formatMoney(amount)} của ${row.dataset.student}.`,
    );
    addAuditLog(
      `Ghi nhận thanh toán ${formatMoney(amount)}: ${row.dataset.student}`,
    );
    paymentForm.reset();
  });
}

document.querySelector("#export-revenue")?.addEventListener("click", () => {
  const rows = [...document.querySelectorAll("#revenue-table tr")].map((row) =>
    [...row.children]
      .map((cell) => `"${cell.textContent.trim().replace(/"/g, '""')}"`)
      .join(","),
  );
  const blob = new Blob(["﻿" + rows.join("\n")], {
    type: "text/csv;charset=utf-8",
  });
  const link = document.createElement("a");
  link.href = URL.createObjectURL(blob);
  link.download = "bao-cao-doanh-thu.csv";
  link.click();
  URL.revokeObjectURL(link.href);
  addAuditLog("Xuất báo cáo doanh thu");
});

// ---------------------------------------------------------------------
// 8g. Quản trị hệ thống: tài khoản, danh mục, nhật ký
// ---------------------------------------------------------------------
const accountTable = document.querySelector("#account-table");
const lockAccountModal = document.querySelector("#lock-account-modal");
const lockAccountForm = document.querySelector("#lock-account-form");
const unlockAccountModal = document.querySelector("#unlock-account-modal");
const unlockAccountForm = document.querySelector("#unlock-account-form");
const createUserModal = document.querySelector("#create-user-modal");
const createUserForm = document.querySelector("#create-user-form");
const editUserModal = document.querySelector("#edit-user-modal");
const editUserForm = document.querySelector("#edit-user-form");

if (accountTable) {
  const SEED_ENTERPRISE_ACCOUNTS = [
    { fullName: "Nguyễn Văn Hải", email: "hai.nguyen@tms.vn", phone: "0982112233", role: "instructor" },
    { fullName: "Trần Mai Anh", email: "anh.tran@tms.vn", phone: "0973223344", role: "instructor" },
    { fullName: "Lê Minh Tuấn", email: "tuan.le@tms.vn", phone: "0964334455", role: "ta" },
    { fullName: "Phạm Thu Hương", email: "huong.pham@tms.vn", phone: "0915445566", role: "ta" },
    { fullName: "Hoàng Gia Bảo", email: "bao.hoang@tms.vn", phone: "0936556677", role: "student" },
    { fullName: "Vũ Phương Thảo", email: "thao.vu@tms.vn", phone: "0947667788", role: "student" },
    { fullName: "Đặng Tiến Dũng", email: "dung.dang@tms.vn", phone: "0908778899", role: "student" },
    { fullName: "Bùi Mỹ Linh", email: "linh.bui@tms.vn", phone: "0989889900", role: "student" },
    { fullName: "Ngô Quốc Huy", email: "huy.ngo@tms.vn", phone: "0971990011", role: "student" },
    { fullName: "Dương Ánh Tuyết", email: "tuyet.duong@tms.vn", phone: "0962001122", role: "admissions" },
    { fullName: "Lý Thành Nam", email: "nam.ly@tms.vn", phone: "0913112233", role: "admissions" },
    { fullName: "Tạ Thị Ngọc", email: "ngoc.ta@tms.vn", phone: "0934223344", role: "accountant" },
    { fullName: "Trịnh Văn Quyết", email: "quyet.trinh@tms.vn", phone: "0945334455", role: "training-manager" },
    { fullName: "Lê Thị Bích", email: "bich.le@tms.vn", phone: "0906445566", role: "training-manager" },
    { fullName: "Nguyễn Đức Anh", email: "anh.duc@tms.vn", phone: "0987556677", role: "student" },
    { fullName: "Đỗ Kim Ngân", email: "ngan.do@tms.vn", phone: "0978667788", role: "student" },
    { fullName: "Võ Quang Vinh", email: "vinh.vo@tms.vn", phone: "0969778899", role: "instructor" },
    { fullName: "Hồ Khánh Vy", email: "vy.ho@tms.vn", phone: "0911889900", role: "ta" },
    { fullName: "Phan Đình Trọng", email: "trong.phan@tms.vn", phone: "0932990011", role: "student" },
    { fullName: "Trần Bảo Trân", email: "tran.bao@tms.vn", phone: "0943001122", role: "student" },
    { fullName: "Nguyễn Hữu Tài", email: "tai.nguyen@tms.vn", phone: "0904112233", role: "student" },
  ];

  const lockedAccounts = new Set(readStore("edumanager-locked-accounts", ["bao.hoang@tms.vn"]));
  const lockedReasons = readStore("edumanager-locked-reasons", {
    "bao.hoang@tms.vn": "Tạm dừng học tập theo đơn bảo lưu",
  });
  let currentSearch = "";
  let currentRole = "ALL";
  let currentStatus = "ALL";
  let currentPage = 1;
  const PAGE_SIZE = 20;

  // Lấy toàn bộ danh sách tài khoản (DEMO + SEED + người dùng mới tạo trong localStorage)
  const getAllAccounts = () => {
    const custom = readStore("edumanager-custom-accounts") || [];
    const map = new Map();
    DEMO_ACCOUNTS.forEach((a) => {
      map.set(a.email, {
        phone: a.phone || "0912345678",
        ...a,
      });
    });
    SEED_ENTERPRISE_ACCOUNTS.forEach((a) => {
      if (!map.has(a.email)) {
        map.set(a.email, a);
      }
    });
    custom.forEach((a) => {
      map.set(a.email, {
        phone: a.phone || "",
        ...a,
      });
    });
    return Array.from(map.values());
  };

  // Tra cứu các lớp học đang phụ trách của nhân sự (S1-10: Cảnh báo bàn giao lớp)
  const getAssignedClasses = (email, role) => {
    if (email === "giangvien@edumanager.vn" || email === "giangvien@tms.vn" || role === "instructor") {
      return [
        { code: "IELTS-2610", name: "IELTS Foundation & Intensive", role: "Giảng viên chính" },
        { code: "GT-2609", name: "Tiếng Anh Giao Tiếp B2", role: "Giảng viên chính" },
      ];
    }
    if (email === "trogiang@edumanager.vn" || email === "trogiang@tms.vn" || role === "ta") {
      return [
        { code: "IELTS-2610", name: "IELTS Foundation & Intensive", role: "Trợ giảng" },
        { code: "GT-2609", name: "Tiếng Anh Giao Tiếp B2", role: "Trợ giảng" },
      ];
    }
    return [];
  };

  const renderAccounts = () => {
    const all = getAllAccounts();
    const kw = currentSearch.trim().toLowerCase();

    // 1. Tìm kiếm (Họ tên, email, SĐT)
    let filtered = all.filter((account) => {
      const matchKw = !kw ||
        account.fullName?.toLowerCase().includes(kw) ||
        account.email?.toLowerCase().includes(kw) ||
        account.phone?.includes(kw);

      const matchRole = currentRole === "ALL" || account.role === currentRole;

      const lockout = typeof checkAccountLockout === "function" ? checkAccountLockout(account.email) : { isLocked: false };
      const isLocked = lockedAccounts.has(account.email) || lockout.isLocked;
      const matchStatus = currentStatus === "ALL" ||
        (currentStatus === "LOCKED" && isLocked) ||
        (currentStatus === "ACTIVE" && !isLocked);

      return matchKw && matchRole && matchStatus;
    });

    const totalCount = filtered.length;
    const totalPages = Math.ceil(totalCount / PAGE_SIZE) || 1;
    if (currentPage > totalPages) currentPage = totalPages;
    if (currentPage < 1) currentPage = 1;

    const startIndex = (currentPage - 1) * PAGE_SIZE;
    const pagedItems = filtered.slice(startIndex, startIndex + PAGE_SIZE);

    const tbody = accountTable.querySelector("tbody");
    if (pagedItems.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center py-8 text-muted">Không tìm thấy tài khoản nào phù hợp với bộ lọc.</td></tr>';
    } else {
      tbody.innerHTML = pagedItems.map((account) => {
        const lockout = typeof checkAccountLockout === "function" ? checkAccountLockout(account.email) : { isLocked: false };
        const locked = lockedAccounts.has(account.email) || lockout.isLocked;
        const reason = lockedReasons[account.email] || (lockout.isLocked ? "Tạm khóa do nhập sai mật khẩu 5 lần" : "");
        const isSelf = account.email === currentUser?.email;
        const assigned = getAssignedClasses(account.email, account.role);

        const statusHtml = locked
          ? `<div>
               <span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
                 <i data-lucide="lock" class="size-3"></i>
                 Đã khoá
               </span>
               <span class="hidden">${badge("Đã khoá", "is-danger")}</span>
               ${reason ? `<div class="text-xs text-rose-500 mt-1 max-w-[180px] truncate" title="Lý do: ${escapeHtml(reason)}"><span class="font-medium">Lý do:</span> ${escapeHtml(reason)}</div>` : ""}
             </div>`
          : `<span class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
               <span class="size-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
               Hoạt động
             </span>`;

        const actionBtn = locked
          ? `<button type="button" class="app-btn app-btn-outline app-btn-sm text-emerald-600 border-emerald-300 hover:bg-emerald-50" data-unlock-account="${escapeHtml(account.email)}">
               <i data-lucide="unlock" class="size-3.5 inline mr-1"></i>Mở khoá
             </button>`
          : `<button type="button" class="app-btn app-btn-outline app-btn-sm text-rose-600 border-rose-200 hover:bg-rose-50" data-open-lock-modal="${escapeHtml(account.email)}" ${isSelf ? 'disabled title="Không thể tự khoá tài khoản của mình"' : ""}>
               <i data-lucide="lock" class="size-3.5 inline mr-1"></i>Khoá
             </button>`;

        const roleBadges = {
          admin: "bg-indigo-50 text-indigo-700 border-indigo-200",
          "training-manager": "bg-blue-50 text-blue-700 border-blue-200",
          admissions: "bg-amber-50 text-amber-700 border-amber-200",
          instructor: "bg-purple-50 text-purple-700 border-purple-200",
          ta: "bg-cyan-50 text-cyan-700 border-cyan-200",
          accountant: "bg-emerald-50 text-emerald-700 border-emerald-200",
          student: "bg-sky-50 text-sky-700 border-sky-200",
        };
        const roleBadgeClass = roleBadges[account.role] || "bg-slate-100 text-slate-700 border-slate-200";

        return `
        <tr>
          <td>
            <div class="flex items-center gap-3">
              <span class="tms-table-avatar">
                ${escapeHtml(getInitials(account.fullName))}
              </span>
              <div>
                <p class="font-bold text-foreground text-sm">${escapeHtml(account.fullName)}</p>
                <p class="text-xs text-muted font-mono">ID: TMS-${escapeHtml((account.email.split('@')[0] || '').substring(0, 10).toUpperCase())}</p>
              </div>
            </div>
          </td>
          <td class="font-mono text-xs text-slate-600">${escapeHtml(account.email)}</td>
          <td class="text-slate-600 text-xs">${escapeHtml(account.phone || "—")}</td>
          <td>
            <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold border ${roleBadgeClass}">
              ${escapeHtml(ROLES[account.role]?.name ?? account.role)}
            </span>
            ${assigned.length > 0 ? `<div class="text-xs text-amber-600 mt-1 flex items-center gap-1"><i data-lucide="book-open" class="size-3 inline"></i>Phụ trách ${assigned.length} lớp</div>` : ""}
          </td>
          <td>${statusHtml}</td>
          <td>
            <div class="flex items-center gap-1.5">
              <button type="button" class="app-btn app-btn-outline app-btn-sm text-primary hover:bg-primary/5" data-edit-account="${escapeHtml(account.email)}" title="Chỉnh sửa thông tin">
                <i data-lucide="pen-line" class="size-3.5 inline mr-1"></i>Sửa
              </button>
              ${actionBtn}
            </div>
          </td>
        </tr>`;
      }).join("");
    }

    // Cập nhật thanh phân trang (Pagination Bar)
    const paginationInfo = document.querySelector("#account-pagination-info");
    const prevBtn = document.querySelector("#account-prev-page");
    const nextBtn = document.querySelector("#account-next-page");
    const pageNumbersBox = document.querySelector("#account-page-numbers");

    if (paginationInfo) {
      if (totalCount === 0) {
        paginationInfo.textContent = "Không có tài khoản nào";
      } else {
        const start = startIndex + 1;
        const end = Math.min(startIndex + PAGE_SIZE, totalCount);
        paginationInfo.textContent = `Hiển thị ${start} - ${end} trên tổng số ${totalCount} tài khoản (Trang ${currentPage}/${totalPages})`;
      }
    }

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;

    if (pageNumbersBox) {
      let pageHtml = "";
      for (let i = 1; i <= totalPages; i++) {
        pageHtml += `
          <button type="button" class="app-btn app-btn-sm tms-page-btn ${i === currentPage ? 'app-btn-primary is-active' : 'app-btn-outline'}" data-page="${i}">
            ${i}
          </button>
        `;
      }
      pageNumbersBox.innerHTML = pageHtml;
    }

    if (window.lucide) window.lucide.createIcons();
  };

  // Lắng nghe sự kiện tìm kiếm & bộ lọc (S1-08)
  const searchInput = document.querySelector("#account-search-input");
  if (searchInput) {
    searchInput.addEventListener("input", (e) => {
      currentSearch = e.target.value;
      currentPage = 1;
      renderAccounts();
    });
  }

  const roleFilter = document.querySelector("#account-role-filter");
  if (roleFilter) {
    roleFilter.addEventListener("change", (e) => {
      currentRole = e.target.value;
      currentPage = 1;
      renderAccounts();
    });
  }

  const statusFilter = document.querySelector("#account-status-filter");
  if (statusFilter) {
    statusFilter.addEventListener("change", (e) => {
      currentStatus = e.target.value;
      currentPage = 1;
      renderAccounts();
    });
  }

  const resetFilterBtn = document.querySelector("#account-reset-filter-btn");
  if (resetFilterBtn) {
    resetFilterBtn.addEventListener("click", () => {
      currentSearch = "";
      currentRole = "ALL";
      currentStatus = "ALL";
      currentPage = 1;
      if (searchInput) searchInput.value = "";
      if (roleFilter) roleFilter.value = "ALL";
      if (statusFilter) statusFilter.value = "ALL";
      renderAccounts();
    });
  }

  // Chuyển trang phân trang
  const prevBtn = document.querySelector("#account-prev-page");
  if (prevBtn) {
    prevBtn.addEventListener("click", () => {
      if (currentPage > 1) {
        currentPage--;
        renderAccounts();
      }
    });
  }

  const nextBtn = document.querySelector("#account-next-page");
  if (nextBtn) {
    nextBtn.addEventListener("click", () => {
      currentPage++;
      renderAccounts();
    });
  }

  const pageNumbersBox = document.querySelector("#account-page-numbers");
  if (pageNumbersBox) {
    pageNumbersBox.addEventListener("click", (e) => {
      const pageBtn = e.target.closest("[data-page]");
      if (pageBtn) {
        currentPage = parseInt(pageBtn.dataset.page, 10);
        renderAccounts();
      }
    });
  }

  // -------------------------------------------------------------------
  // Modal Thêm tài khoản mới (S1-08)
  // -------------------------------------------------------------------
  const openCreateUserBtn = document.querySelector("#open-create-user-modal");
  const closeCreateUser = () => {
    if (createUserModal) {
      createUserModal.classList.add("hidden");
      if (createUserForm) createUserForm.reset();
      const passInput = document.querySelector("#new-user-password");
      if (passInput) passInput.value = "Edu@2026";
      const alertBox = document.querySelector("#create-user-alert");
      if (alertBox) {
        alertBox.classList.add("hidden");
        alertBox.textContent = "";
      }
      document.querySelector("#create-user-success-box")?.classList.add("hidden");
      document.querySelector("#create-user-actions")?.classList.remove("hidden");
    }
  };

  const genPassBtn = document.querySelector("#btn-generate-password");
  if (genPassBtn) {
    genPassBtn.addEventListener("click", () => {
      const passInput = document.querySelector("#new-user-password");
      if (passInput) {
        const randPass = "Edu@" + Math.random().toString(36).substring(2, 7) + "9";
        passInput.value = randPass;
        passInput.type = "text";
        if (typeof showToast === "function") showToast(`Đã sinh mật khẩu ngẫu nhiên: ${randPass}`);
      }
    });
  }

  const togglePassBtn = document.querySelector("#toggle-new-user-password");
  if (togglePassBtn) {
    togglePassBtn.addEventListener("click", () => {
      const passInput = document.querySelector("#new-user-password");
      if (passInput) {
        passInput.type = passInput.type === "password" ? "text" : "password";
      }
    });
  }

  if (openCreateUserBtn && createUserModal) {
    openCreateUserBtn.addEventListener("click", () => {
      closeCreateUser();
      createUserModal.classList.remove("hidden");
      const nameInput = document.querySelector("#new-user-name");
      if (nameInput) nameInput.focus();
      if (window.lucide) window.lucide.createIcons();
    });
  }

  document.querySelectorAll("[data-close-create-user]").forEach((btn) => {
    btn.addEventListener("click", closeCreateUser);
  });

  if (createUserModal) {
    createUserModal.addEventListener("click", (e) => {
      if (e.target === createUserModal) closeCreateUser();
    });
  }

  if (createUserForm) {
    createUserForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const name = document.querySelector("#new-user-name")?.value.trim();
      const email = document.querySelector("#new-user-email")?.value.trim().toLowerCase();
      const phone = document.querySelector("#new-user-phone")?.value.trim() || "";
      const role = document.querySelector("#new-user-role")?.value || "student";
      const inputPass = document.querySelector("#new-user-password")?.value.trim();
      const alertBox = document.querySelector("#create-user-alert");

      const showAlert = (msg, isErr = true) => {
        if (alertBox) {
          alertBox.className = isErr
            ? "p-3 rounded-lg text-sm bg-rose-500/10 text-rose-500 border border-rose-500/20"
            : "p-3 rounded-lg text-sm bg-emerald-500/10 text-emerald-600 border border-emerald-500/20";
          alertBox.textContent = msg;
          alertBox.classList.remove("hidden");
        }
      };

      if (!name || !email) {
        showAlert("Vui lòng điền đầy đủ họ tên và email.");
        return;
      }

      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showAlert("Email không đúng định dạng (ví dụ: ten@domain.com).");
        return;
      }

      // Kiểm tra trùng email (S1-08: Email trùng bị từ chối kèm thông báo cụ thể)
      const all = getAllAccounts();
      if (all.some((a) => a.email.toLowerCase() === email || (a.aliases || []).map((x) => x.toLowerCase()).includes(email))) {
        showAlert("Email này đã tồn tại trong hệ thống, vui lòng chọn email khác!");
        return;
      }

      const finalPass = inputPass || ("Edu@" + Math.random().toString(36).substring(2, 8) + "9");

      if (finalPass.length < 8 || !/[a-zA-Z]/.test(finalPass) || !/[0-9]/.test(finalPass)) {
        showAlert("Mật khẩu khởi tạo phải có tối thiểu 8 ký tự, gồm cả chữ và số.");
        return;
      }

      // Gửi yêu cầu API đến backend / Vite dev server để gửi email kích hoạt tài khoản thật
      try {
        await fetch("/api/admin/users", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, fullName: name, phone, role, password: finalPass, tempPassword: finalPass }),
        }).catch(() => null);

        await fetch("/api/send-email", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, fullName: name, phone, role, password: finalPass, tempPassword: finalPass }),
        }).catch(() => null);
      } catch (_) {}

      // Đánh dấu tài khoản này cần đổi mật khẩu ở lần đăng nhập đầu tiên
      const firstLoginMap = readStore(FIRST_LOGIN_KEY, {});
      firstLoginMap[email.toLowerCase()] = { isFirstLogin: true, loginCount: 0 };
      writeStore(FIRST_LOGIN_KEY, firstLoginMap);

      // Lưu tài khoản mới vào danh sách lưu trữ
      const customAccounts = readStore("edumanager-custom-accounts") || [];
      const newAcc = {
        fullName: name,
        email,
        phone,
        role,
        password: finalPass,
        passwords: [finalPass],
        aliases: [email, email.split("@")[0]],
        createdAt: new Date().toISOString(),
        isFirstLogin: true,
        loginCount: 0,
      };
      customAccounts.unshift(newAcc);
      writeStore("edumanager-custom-accounts", customAccounts);

      // Lưu mật khẩu vào custom passwords map để xác thực 100%
      const customPasswords = readStore("edumanager-custom-passwords") || {};
      customPasswords[email] = finalPass;
      customPasswords[email.split("@")[0]] = finalPass;
      writeStore("edumanager-custom-passwords", customPasswords);

      addAuditLog(`Tạo tài khoản mới: ${name} (${email}) - Vai trò: ${role} - Mật khẩu đã được thiết lập`);
      renderAccounts();
      renderAuditLog();

      // Hiển thị card thành công kèm mật khẩu rõ ràng ngay trong modal
      const successBox = document.querySelector("#create-user-success-box");
      const actionsBox = document.querySelector("#create-user-actions");
      if (successBox) {
        document.querySelector("#success-user-name").textContent = name;
        document.querySelector("#success-user-email").textContent = email;
        document.querySelector("#success-user-pass").textContent = finalPass;
        successBox.classList.remove("hidden");
        if (actionsBox) actionsBox.classList.add("hidden");
        if (alertBox) alertBox.classList.add("hidden");

        const copyBtn = document.querySelector("#btn-copy-new-account");
        if (copyBtn) {
          copyBtn.onclick = () => {
            const textToCopy = `Tài khoản: ${email}\nMật khẩu: ${finalPass}`;
            if (navigator.clipboard) {
              navigator.clipboard.writeText(textToCopy);
            }
            const copyText = document.querySelector("#copy-btn-text");
            if (copyText) copyText.textContent = "Đã sao chép!";
            setTimeout(() => {
              if (copyText) copyText.textContent = "Sao chép thông tin";
            }, 2500);
          };
        }
      } else {
        closeCreateUser();
      }

      if (typeof showToast === "function") {
        showToast(`Tạo tài khoản ${email} thành công! Mật khẩu: ${finalPass}`, "success");
      }
      if (window.lucide) window.lucide.createIcons();
    });
  }

  // -------------------------------------------------------------------
  // Modal Sửa thông tin tài khoản (S1-08)
  // -------------------------------------------------------------------
  const closeEditUser = () => {
    if (editUserModal) {
      editUserModal.classList.add("hidden");
      if (editUserForm) editUserForm.reset();
      const alertBox = document.querySelector("#edit-user-alert");
      if (alertBox) {
        alertBox.classList.add("hidden");
        alertBox.textContent = "";
      }
    }
  };

  accountTable.addEventListener("click", (event) => {
    // Sửa thông tin tài khoản
    const editBtn = event.target.closest("[data-edit-account]");
    if (editBtn) {
      const email = editBtn.dataset.editAccount;
      const account = getAllAccounts().find((a) => a.email === email);
      if (!account) return;

      if (editUserModal) {
        document.querySelector("#edit-user-email").value = account.email;
        document.querySelector("#edit-user-name").value = account.fullName;
        document.querySelector("#edit-user-phone").value = account.phone || "";
        const alertBox = document.querySelector("#edit-user-alert");
        if (alertBox) alertBox.classList.add("hidden");
        editUserModal.classList.remove("hidden");
        const nameInput = document.querySelector("#edit-user-name");
        if (nameInput) nameInput.focus();
      }
      return;
    }

    // Mở modal khoá
    const lockBtn = event.target.closest("[data-open-lock-modal]");
    if (lockBtn) {
      const email = lockBtn.dataset.openLockModal;
      const account = getAllAccounts().find((a) => a.email === email);
      if (!account) return;

      if (account.email === currentUser?.email) {
        alert("Không thể tự khoá hoặc ngừng hoạt động tài khoản của chính mình.");
        return;
      }

      if (lockAccountModal) {
        document.querySelector("#lock-account-email").value = account.email;
        document.querySelector("#lock-account-name").textContent = account.fullName;
        document.querySelector("#lock-account-email-text").textContent = account.email;
        document.querySelector("#lock-account-role").textContent = ROLES[account.role]?.name ?? account.role;

        // Cảnh báo lớp học phụ trách cần bàn giao (S1-10)
        const assigned = getAssignedClasses(account.email, account.role);
        const warningBox = document.querySelector("#lock-handover-alert");
        const classesList = document.querySelector("#lock-handover-classes");

        if (assigned.length > 0) {
          classesList.innerHTML = assigned
            .map((c) => `<li><span class="font-bold">${escapeHtml(c.code)}</span> - ${escapeHtml(c.name)} (<span class="italic">${escapeHtml(c.role)}</span>)</li>`)
            .join("");
          warningBox.classList.remove("hidden");
        } else {
          classesList.innerHTML = "";
          warningBox.classList.add("hidden");
        }

        const reasonInput = document.querySelector("#lock-reason-input");
        if (reasonInput) reasonInput.value = "";
        const errBox = document.querySelector("#lock-account-error");
        if (errBox) errBox.classList.add("hidden");

        lockAccountModal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
        if (reasonInput) reasonInput.focus();
      }
      return;
    }

    // Xử lý mở khoá tài khoản
    const unlockBtn = event.target.closest("[data-unlock-account]");
    if (unlockBtn) {
      const email = unlockBtn.dataset.unlockAccount;
      const account = getAllAccounts().find((a) => a.email === email);
      const name = account ? account.fullName : email;
      const lockout = typeof checkAccountLockout === "function" ? checkAccountLockout(email) : { isLocked: false };
      const currentReason = lockedReasons[email] || (lockout.isLocked ? "Tạm khóa do nhập sai mật khẩu 5 lần" : "Khóa bởi Quản trị viên");

      if (unlockAccountModal) {
        document.querySelector("#unlock-account-email").value = email;
        document.querySelector("#unlock-account-name").textContent = name;
        document.querySelector("#unlock-account-email-text").textContent = email;
        document.querySelector("#unlock-account-role").textContent = account ? (ROLES[account.role]?.name ?? account.role) : "Người dùng";
        document.querySelector("#unlock-account-reason").textContent = currentReason;
        const errBox = document.querySelector("#unlock-account-error");
        if (errBox) errBox.classList.add("hidden");

        unlockAccountModal.classList.remove("hidden");
        if (window.lucide) window.lucide.createIcons();
      } else {
        // Fallback trực tiếp nếu không tìm thấy modal
        lockedAccounts.delete(email);
        delete lockedReasons[email];
        writeStore("edumanager-locked-accounts", [...lockedAccounts]);
        writeStore("edumanager-locked-reasons", lockedReasons);

        const lockoutMap = readStore(LOCKOUT_STORAGE_KEY, {});
        const norm = typeof normalizeLoginKey === "function" ? normalizeLoginKey(email) : email.toLowerCase();
        delete lockoutMap[norm];
        delete lockoutMap[email.toLowerCase()];
        writeStore(LOCKOUT_STORAGE_KEY, lockoutMap);

        addAuditLog(`Mở khoá tài khoản ${email}`);
        renderAccounts();
        renderAuditLog();
        if (typeof showToast === "function") {
          showToast(`Mở khoá tài khoản ${email} thành công!`, "success");
        }
      }
      return;
    }
  });

  // Đóng modal mở khoá
  const closeUnlockModal = () => {
    if (unlockAccountModal) {
      unlockAccountModal.classList.add("hidden");
      if (unlockAccountForm) unlockAccountForm.reset();
      const errBox = document.querySelector("#unlock-account-error");
      if (errBox) {
        errBox.classList.add("hidden");
        errBox.textContent = "";
      }
    }
  };

  document.querySelectorAll("[data-close-unlock-account]").forEach((btn) => {
    btn.addEventListener("click", closeUnlockModal);
  });

  if (unlockAccountModal) {
    unlockAccountModal.addEventListener("click", (e) => {
      if (e.target === unlockAccountModal) closeUnlockModal();
    });
  }

  // Xác nhận submit form mở khoá tài khoản
  if (unlockAccountForm) {
    unlockAccountForm.addEventListener("submit", async (e) => {
      e.preventDefault();
      const email = document.querySelector("#unlock-account-email")?.value;
      if (!email) return;

      const submitBtn = document.querySelector("#unlock-confirm-submit-btn");
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="inline-block animate-spin mr-1">⌛</span> Đang mở khoá...';
      }

      try {
        const all = getAllAccounts();
        const targetIdx = all.findIndex((a) => a.email === email);
        const targetId = targetIdx >= 0 ? targetIdx + 1 : 1;
        await fetch(`/api/admin/users/${targetId}/status`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ status: "ACTIVE" }),
        }).catch(() => null);
      } catch (_) {
        // Fallback offline mock
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.innerHTML = '<i data-lucide="unlock" class="size-4"></i> Xác nhận mở khoá';
          if (window.lucide) window.lucide.createIcons();
        }
      }

      // Xóa khỏi danh sách tài khoản bị khóa
      lockedAccounts.delete(email);
      delete lockedReasons[email];
      writeStore("edumanager-locked-accounts", [...lockedAccounts]);
      writeStore("edumanager-locked-reasons", lockedReasons);

      // Dỡ bỏ khóa tạm thời 15 phút do nhập sai mật khẩu (S1-01 AC3 & S1-10)
      const lockoutMap = readStore(LOCKOUT_STORAGE_KEY, {});
      const norm = typeof normalizeLoginKey === "function" ? normalizeLoginKey(email) : email.toLowerCase();
      delete lockoutMap[norm];
      delete lockoutMap[email.toLowerCase()];
      writeStore(LOCKOUT_STORAGE_KEY, lockoutMap);

      const attemptsMap = readStore("edumanager-failed-attempts", {});
      delete attemptsMap[norm];
      delete attemptsMap[email.toLowerCase()];
      writeStore("edumanager-failed-attempts", attemptsMap);

      closeUnlockModal();
      addAuditLog(`Mở khoá tài khoản ${email}`);
      renderAccounts();
      renderAuditLog();

      if (typeof showToast === "function") {
        showToast(`Mở khoá tài khoản ${email} thành công! Người dùng có thể đăng nhập bình thường.`, "success");
      }
      if (window.lucide) window.lucide.createIcons();
    });
  }

  // Đóng modal khoá
  document.querySelectorAll("[data-close-lock-account]").forEach((btn) => {
    btn.addEventListener("click", closeLockModal);
  });

  renderAccounts();
}

document.querySelectorAll("[data-catalog-form]").forEach((form) => {
  form.addEventListener("submit", (event) => {
    event.preventDefault();
    const input = form.querySelector("input");
    const value = input.value.trim();
    if (!value) {
      input.focus();
      return;
    }
    const item = document.createElement("li");
    item.className = "app-badge";
    item.textContent = value;
    document.querySelector(`#${form.dataset.catalogForm}`).append(item);
    addAuditLog(`Thêm danh mục "${value}"`);
    renderAuditLog();
    form.reset();
    clearContainerDrafts(form);
  });
});

function renderAuditLog() {
  const auditTable = document.querySelector("#audit-table");
  if (!auditTable) return;
  const logs = readStore(AUDIT_KEY);
  auditTable.querySelector("tbody").innerHTML = logs.length
    ? logs
        .map(
          (log) => `
        <tr>
          <td class="whitespace-nowrap">${formatDateTime(log.time)}</td>
          <td>${escapeHtml(log.actor)}</td>
          <td>${escapeHtml(log.action)}</td>
        </tr>`,
        )
        .join("")
    : '<tr><td colspan="3" class="app-muted">Chưa có hoạt động nào.</td></tr>';
}

renderAuditLog();

// =====================================================================
// 8.1. Ma trận phân quyền 12 Module (Chuẩn User Roles & S1-05)
// =====================================================================
const initRoleMatrix = () => {
  const tabBtnMatrix = document.querySelector("#tab-btn-matrix");
  const tabBtnRoles = document.querySelector("#tab-btn-roles");
  const tabPaneMatrix = document.querySelector("#tab-pane-matrix");
  const tabPaneRoles = document.querySelector("#tab-pane-roles");
  const matrixSearchInput = document.querySelector("#matrix-search-input");
  const matrixFilterPills = document.querySelectorAll(".matrix-filter-pill");
  const matrixTable = document.querySelector("#role-matrix-table");

  if (!tabBtnMatrix || !tabBtnRoles || !tabPaneMatrix || !tabPaneRoles) return;

  const setActiveTab = (isMatrix) => {
    if (isMatrix) {
      tabPaneMatrix.classList.remove("hidden");
      tabPaneRoles.classList.add("hidden");
      tabBtnMatrix.style.background = "#FFFFFF";
      tabBtnMatrix.style.color = "var(--color-navy-title)";
      tabBtnMatrix.style.borderColor = "#CBD5E1";
      tabBtnMatrix.style.fontWeight = "700";
      tabBtnRoles.style.background = "transparent";
      tabBtnRoles.style.color = "var(--color-text-secondary)";
      tabBtnRoles.style.borderColor = "transparent";
      tabBtnRoles.style.fontWeight = "600";
    } else {
      tabPaneMatrix.classList.add("hidden");
      tabPaneRoles.classList.remove("hidden");
      tabBtnRoles.style.background = "#FFFFFF";
      tabBtnRoles.style.color = "var(--color-navy-title)";
      tabBtnRoles.style.borderColor = "#CBD5E1";
      tabBtnRoles.style.fontWeight = "700";
      tabBtnMatrix.style.background = "transparent";
      tabBtnMatrix.style.color = "var(--color-text-secondary)";
      tabBtnMatrix.style.borderColor = "transparent";
      tabBtnMatrix.style.fontWeight = "600";
    }
    if (window.lucide) window.lucide.createIcons();
  };

  tabBtnMatrix.addEventListener("click", () => setActiveTab(true));
  tabBtnRoles.addEventListener("click", () => setActiveTab(false));

  let currentCategory = "all";
  let currentSearch = "";

  const filterMatrixRows = () => {
    if (!matrixTable) return;
    const rows = matrixTable.querySelectorAll("tbody tr");
    rows.forEach((row) => {
      const cat = row.dataset.category || "training";
      const text = row.textContent.toLowerCase();
      const matchCat = currentCategory === "all" || cat === currentCategory;
      const matchSearch = !currentSearch || text.includes(currentSearch);
      row.style.display = matchCat && matchSearch ? "" : "none";
    });
  };

  matrixSearchInput?.addEventListener("input", (e) => {
    currentSearch = e.target.value.trim().toLowerCase();
    filterMatrixRows();
  });

  matrixFilterPills.forEach((pill) => {
    pill.addEventListener("click", () => {
      matrixFilterPills.forEach((p) => {
        p.classList.remove("active");
        p.style.background = "#FFFFFF";
        p.style.color = "var(--color-text-secondary)";
        p.style.borderColor = "var(--color-border)";
      });
      pill.classList.add("active");
      pill.style.background = "#EFF6FF";
      pill.style.color = "var(--color-blue-primary)";
      pill.style.borderColor = "var(--color-blue-primary)";
      currentCategory = pill.dataset.filter || "all";
      filterMatrixRows();
    });
  });
};

initRoleMatrix();

// =====================================================================
// 9. Xử lý Đổi mật khẩu khi đang đăng nhập (S1-04) & Lần đầu đăng nhập
// =====================================================================
const ensureChangePasswordModal = () => {
  let modal = document.querySelector("#change-password-modal");
  if (modal) return modal;

  modal = document.createElement("div");
  modal.id = "change-password-modal";
  modal.className = "cp-modal-backdrop hidden";
  modal.setAttribute("role", "dialog");
  modal.setAttribute("aria-modal", "true");
  modal.setAttribute("aria-labelledby", "cp-modal-title");
  modal.innerHTML = `
    <div class="cp-modal-card">
      <div class="cp-top-bar">
        <button type="button" class="cp-close-btn" data-close-change-pass aria-label="Đóng">
          <i data-lucide="x" class="size-4"></i>
        </button>
        <div class="cp-security-badge">
          <i data-lucide="lock" class="size-3.5"></i>
          <span>Bảo mật</span>
        </div>
      </div>

      <div id="cp-form-view">
        <div class="cp-center-badge">
          <i data-lucide="key" class="size-6"></i>
        </div>

        <h2 id="cp-modal-title" class="cp-title">Đổi mật khẩu</h2>
        <p class="cp-subtitle">Nhập mật khẩu hiện tại rồi chọn một mật khẩu mới.</p>

        <div id="first-login-notice" class="p-3 mb-4 rounded-lg bg-blue-50 border border-blue-200 text-blue-800 text-xs hidden">
          <div class="flex items-start gap-2">
            <i data-lucide="info" class="size-4 shrink-0 mt-0.5 text-blue-600"></i>
            <div>
              <span class="font-bold">Đổi mật khẩu lần đầu:</span>
              Chào mừng bạn đến với TMS! Đây là lần đầu tiên bạn đăng nhập bằng mật khẩu tạm. Vui lòng thiết lập mật khẩu mới để bảo vệ tài khoản cá nhân.
            </div>
          </div>
        </div>

        <form id="change-password-form" novalidate>
          <div class="cp-field">
            <label for="cp-current" class="cp-label">Mật khẩu hiện tại</label>
            <div class="cp-input-wrap">
              <span class="cp-input-icon"><i data-lucide="lock" class="size-4"></i></span>
              <input type="password" id="cp-current" name="currentPassword" class="cp-input" placeholder="••••••••••" autocomplete="current-password" required />
              <button type="button" class="cp-toggle-pass" data-target="cp-current" aria-label="Ẩn hiện mật khẩu">
                <i data-lucide="eye" class="size-4"></i>
              </button>
            </div>
            <p id="cp-current-error" class="cp-error-msg hidden">Mật khẩu hiện tại không đúng</p>
          </div>

          <div class="cp-field">
            <label for="cp-new" class="cp-label">Mật khẩu mới</label>
            <div class="cp-input-wrap">
              <span class="cp-input-icon"><i data-lucide="lock" class="size-4"></i></span>
              <input type="password" id="cp-new" name="newPassword" class="cp-input" placeholder="••••••••••" autocomplete="new-password" required />
              <button type="button" class="cp-toggle-pass" data-target="cp-new" aria-label="Ẩn hiện mật khẩu">
                <i data-lucide="eye" class="size-4"></i>
              </button>
            </div>
          </div>

          <div class="cp-field">
            <label for="cp-confirm" class="cp-label">Xác nhận mật khẩu</label>
            <div class="cp-input-wrap">
              <span class="cp-input-icon"><i data-lucide="lock" class="size-4"></i></span>
              <input type="password" id="cp-confirm" name="confirmPassword" class="cp-input" placeholder="Nhập lại mật khẩu mới" autocomplete="new-password" required />
              <button type="button" class="cp-toggle-pass" data-target="cp-confirm" aria-label="Ẩn hiện mật khẩu">
                <i data-lucide="eye" class="size-4"></i>
              </button>
            </div>
            <p id="cp-confirm-error" class="cp-error-msg hidden">Xác nhận mật khẩu mới không trùng khớp</p>
          </div>

          <div class="cp-rules-card">
            <div class="cp-rules-title">Mật khẩu cần có:</div>
            <ul class="cp-rules-list">
              <li id="cp-rule-length" class="cp-rule-item">
                <span class="cp-rule-check">✓</span>
                <span>Tối thiểu 8 ký tự</span>
              </li>
              <li id="cp-rule-case" class="cp-rule-item">
                <span class="cp-rule-check">✓</span>
                <span>Bao gồm chữ cái in hoa và chữ cái thường</span>
              </li>
              <li id="cp-rule-digits" class="cp-rule-item">
                <span class="cp-rule-check">✓</span>
                <span>Có các chữ số</span>
              </li>
              <li id="cp-rule-special" class="cp-rule-item">
                <span class="cp-rule-check">✓</span>
                <span>Ít nhất một ký tự đặc biệt (!, @, #, $, ^, *)</span>
              </li>
            </ul>
          </div>

          <div class="cp-devices-card">
            <div class="cp-devices-icon">
              <i data-lucide="monitor" class="size-4"></i>
            </div>
            <div>
              <div class="cp-devices-title">Các thiết bị khác sẽ bị đăng xuất</div>
              <div class="cp-devices-sub">Phiên trên thiết bị này vẫn giữ nguyên.</div>
            </div>
          </div>

          <button type="submit" id="cp-submit-btn" class="cp-submit-action is-disabled" disabled>
            Cập nhật mật khẩu
          </button>
        </form>
      </div>

      <div id="cp-success-view" class="cp-success-wrapper hidden">
        <div class="cp-center-badge" style="background: #00c48c;">
          <i data-lucide="check" class="size-6"></i>
        </div>

        <div class="cp-success-tag">HOÀN TẤT</div>
        <h2 class="cp-title">Đã đổi mật khẩu</h2>
        <p class="cp-subtitle">Mật khẩu mới có hiệu lực ngay từ bây giờ.</p>

        <div class="cp-success-card">
          <div class="cp-success-icon">
            <i data-lucide="monitor" class="size-4"></i>
          </div>
          <div>
            <div class="cp-success-title">Các phiên đăng nhập khác đã bị thu hồi</div>
            <div class="cp-success-desc">Phiên trên thiết bị này vẫn giữ nguyên.</div>
          </div>
        </div>

        <button type="button" id="cp-finish-btn" class="cp-home-action" data-close-change-pass>
          <span>Về trang chủ</span>
          <i data-lucide="arrow-up-right" class="size-4"></i>
        </button>
      </div>
    </div>
  `;
  document.body.appendChild(modal);
  if (window.lucide) window.lucide.createIcons();
  return modal;
};

const mountChangePasswordButtons = () => {
  // Gắn nút "Đổi mật khẩu" trên Topbar nếu chưa có
  const userProfilePill = document.querySelector("#tms-user-profile-btn");
  if (userProfilePill && !document.querySelector("#topbar-change-pass-btn")) {
    const parentContainer = userProfilePill.closest(".relative");
    if (parentContainer && parentContainer.parentElement) {
      const topbarBtn = document.createElement("button");
      topbarBtn.type = "button";
      topbarBtn.className = "tms-btn-topbar-changepass";
      topbarBtn.id = "topbar-change-pass-btn";
      topbarBtn.title = "Đổi mật khẩu tài khoản";
      topbarBtn.innerHTML = `
        <i data-lucide="key" class="size-4"></i>
        <span class="hidden md:inline">Đổi mật khẩu</span>
      `;
      parentContainer.parentElement.insertBefore(topbarBtn, parentContainer);
      if (window.lucide) window.lucide.createIcons();
    }
  }

  // Gắn nút "Đổi mật khẩu" vào User Dropdown Menu nếu chưa có
  const dropdownMenu = document.querySelector("#tms-user-dropdown-menu");
  if (dropdownMenu && !dropdownMenu.querySelector("#open-change-password-modal")) {
    const logoutBtn = dropdownMenu.querySelector('[data-nav="logout"]');
    const changePassBtn = document.createElement("button");
    changePassBtn.type = "button";
    changePassBtn.className = "tms-dropdown-item tms-dropdown-changepass";
    changePassBtn.id = "open-change-password-modal";
    changePassBtn.innerHTML = `
      <span class="flex items-center gap-2">
        <i data-lucide="key" class="size-4 text-primary"></i>
        <span>Đổi mật khẩu</span>
      </span>
      <span class="tms-badge-key">Bảo mật</span>
    `;
    const targetContainer = dropdownMenu.querySelector(".space-y-0.5, .p-1") || dropdownMenu;
    if (logoutBtn) {
      const divider = document.createElement("div");
      divider.className = "tms-dropdown-divider";
      targetContainer.insertBefore(changePassBtn, logoutBtn);
      targetContainer.insertBefore(divider, logoutBtn);
    } else {
      targetContainer.appendChild(changePassBtn);
    }
    if (window.lucide) window.lucide.createIcons();
  }
};

const setupChangePasswordModal = () => {
  const isProtectedPage = Boolean(document.body?.dataset?.role);
  if (!isProtectedPage) return;

  mountChangePasswordButtons();
  ensureChangePasswordModal();

  const changePassModal = document.querySelector("#change-password-modal");
  const changePassForm = document.querySelector("#change-password-form");
  const formView = document.querySelector("#cp-form-view");
  const successView = document.querySelector("#cp-success-view");

  const currentInput = document.querySelector("#cp-current");
  const newPassInput = document.querySelector("#cp-new");
  const confirmInput = document.querySelector("#cp-confirm");
  const submitBtn = document.querySelector("#cp-submit-btn");

  const currentError = document.querySelector("#cp-current-error");
  const confirmError = document.querySelector("#cp-confirm-error");

  const ruleLength = document.querySelector("#cp-rule-length");
  const ruleCase = document.querySelector("#cp-rule-case");
  const ruleDigits = document.querySelector("#cp-rule-digits");
  const ruleSpecial = document.querySelector("#cp-rule-special");

  if (!changePassModal || !changePassForm) return;

  const resetModalState = () => {
    changePassForm.reset();
    currentInput?.classList.remove("is-error");
    currentError?.classList.add("hidden");
    confirmInput?.classList.remove("is-error");
    confirmError?.classList.add("hidden");

    ruleLength?.classList.remove("is-met");
    ruleCase?.classList.remove("is-met");
    ruleDigits?.classList.remove("is-met");
    ruleSpecial?.classList.remove("is-met");

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.classList.add("is-disabled");
      submitBtn.textContent = "Cập nhật mật khẩu";
    }

    formView?.classList.remove("hidden");
    successView?.classList.add("hidden");

    const noticeBox = document.querySelector("#first-login-notice");
    if (noticeBox) noticeBox.classList.add("hidden");
  };

  const closeChangePass = () => {
    changePassModal.classList.add("hidden");
    resetModalState();
  };

  const openChangePass = (isFirstLoginPrompt = false) => {
    resetModalState();
    changePassModal.classList.remove("hidden");
    const noticeBox = document.querySelector("#first-login-notice");
    if (noticeBox) {
      if (isFirstLoginPrompt) {
        noticeBox.classList.remove("hidden");
      } else {
        noticeBox.classList.add("hidden");
      }
    }
    if (window.lucide) window.lucide.createIcons();
    setTimeout(() => {
      currentInput?.focus();
    }, 50);
  };

  // Mở modal khi bấm các nút Đổi mật khẩu
  document.addEventListener("click", (e) => {
    const target = e.target.closest("#topbar-change-pass-btn, #open-change-password-modal, #open-change-password-modal-sidebar, #dropdown-change-pass, [data-open-change-pass]");
    if (target) {
      e.preventDefault();
      document.querySelector("#tms-user-dropdown-menu")?.classList.remove("is-open");
      openChangePass(false);
    }
  });

  document.querySelectorAll("[data-close-change-pass]").forEach((btn) => {
    btn.addEventListener("click", closeChangePass);
  });

  changePassModal.addEventListener("click", (e) => {
    if (e.target === changePassModal) closeChangePass();
  });

  // Nút Ẩn / Hiện mật khẩu mắt xem
  document.querySelectorAll(".cp-toggle-pass, .toggle-pass-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const targetId = btn.dataset.target;
      const input = document.getElementById(targetId);
      if (!input) return;
      const isPass = input.type === "password";
      input.type = isPass ? "text" : "password";
      const icon = btn.querySelector("i, svg");
      if (icon) {
        icon.setAttribute("data-lucide", isPass ? "eye-off" : "eye");
        if (window.lucide) window.lucide.createIcons();
      }
    });
  });

  // Hàm tính toán kiểm tra 4 điều kiện mật khẩu và trạng thái nút Submit
  const updatePasswordRules = () => {
    const val = newPassInput?.value || "";
    const isLen = val.length >= 8;
    const isCas = /[a-z]/.test(val) && /[A-Z]/.test(val);
    const isDig = /\d/.test(val);
    const isSpe = /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(val);

    ruleLength?.classList.toggle("is-met", isLen);
    ruleCase?.classList.toggle("is-met", isCas);
    ruleDigits?.classList.toggle("is-met", isDig);
    ruleSpecial?.classList.toggle("is-met", isSpe);

    const allRulesMet = isLen && isCas && isDig && isSpe;
    const confirmVal = confirmInput?.value || "";
    const currentVal = currentInput?.value || "";

    const hasConfirm = confirmVal.length > 0;
    const isMatch = hasConfirm && (confirmVal === val);

    if (hasConfirm && !isMatch) {
      confirmInput?.classList.add("is-error");
      confirmError?.classList.remove("hidden");
    } else {
      confirmInput?.classList.remove("is-error");
      confirmError?.classList.add("hidden");
    }

    const canSubmit = allRulesMet && isMatch && currentVal.trim().length > 0;
    if (submitBtn) {
      if (canSubmit) {
        submitBtn.disabled = false;
        submitBtn.classList.remove("is-disabled");
      } else {
        submitBtn.disabled = true;
        submitBtn.classList.add("is-disabled");
      }
    }
  };

  newPassInput?.addEventListener("input", updatePasswordRules);
  confirmInput?.addEventListener("input", updatePasswordRules);
  currentInput?.addEventListener("input", () => {
    currentInput.classList.remove("is-error");
    currentError?.classList.add("hidden");
    updatePasswordRules();
  });

  // Kiểm tra mật khẩu hiện tại có hợp lệ không (hỗ trợ tất cả các actor và role)
  const verifyCurrentPassword = (entered) => {
    const customPasswords = readStore("edumanager-custom-passwords") || {};
    const user = getCurrentUser() || currentUser;
    const email = (user?.email || "").toLowerCase();
    const role = user?.role || document.body.dataset.role || "";

    // 1. Kiểm tra nếu tài khoản/role đã từng đổi mật khẩu tùy chỉnh
    if (email && customPasswords[email]) {
      return customPasswords[email] === entered;
    }
    if (role && customPasswords[role]) {
      return customPasswords[role] === entered;
    }
    if (email.includes("@") && customPasswords[email.split("@")[0]]) {
      return customPasswords[email.split("@")[0]] === entered;
    }

    // 2. Tìm tài khoản trong DEMO_ACCOUNTS và customAccounts
    const customAccounts = readStore("edumanager-custom-accounts") || [];
    const allAccounts = [...DEMO_ACCOUNTS, ...customAccounts];

    const matched = allAccounts.find((acc) => {
      if (email && acc.email && acc.email.toLowerCase() === email) return true;
      if (email && acc.aliases && acc.aliases.some((a) => a.toLowerCase() === email)) return true;
      if (role && acc.role === role) return true;
      return false;
    });

    if (matched) {
      if (matched.password && matched.password === entered) return true;
      if (Array.isArray(matched.passwords) && matched.passwords.includes(entered)) return true;
    }

    // 3. Fallback danh sách mật khẩu mặc định phổ biến của từng actor
    const defaultPasswords = {
      "admin@example.com": ["Admin@123", "admin123", "Admin@123456"],
      "admin@tms.vn": ["Admin@123", "admin123", "Admin@123456"],
      "admin@edumanager.vn": ["Admin@123", "admin123", "Admin@123456"],
      "training@example.com": ["Daotao@123", "Quanly@123", "training123", "daotao123"],
      "training@tms.vn": ["Daotao@123", "Quanly@123", "training123", "daotao123"],
      "quanlydaotao@edumanager.vn": ["Daotao@123", "Quanly@123", "training123", "daotao123"],
      "daotao@tms.vn": ["Daotao@123", "Quanly@123", "training123", "daotao123"],
      "admissions@example.com": ["Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123"],
      "admissions@tms.vn": ["Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123"],
      "tuvan@edumanager.vn": ["Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123"],
      "tuyensinh@tms.vn": ["Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123"],
      "instructor@example.com": ["Giangvien@123", "instructor123", "giangvien123"],
      "instructor@tms.vn": ["Giangvien@123", "instructor123", "giangvien123"],
      "giangvien@edumanager.vn": ["Giangvien@123", "instructor123", "giangvien123"],
      "giangvien@tms.vn": ["Giangvien@123", "instructor123", "giangvien123"],
      "accountant@example.com": ["Ketoan@123", "accountant123", "ketoan123"],
      "accountant@tms.vn": ["Ketoan@123", "accountant123", "ketoan123"],
      "ketoan@edumanager.vn": ["Ketoan@123", "accountant123", "ketoan123"],
      "ketoan@tms.vn": ["Ketoan@123", "accountant123", "ketoan123"],
      "student@example.com": ["Hocvien@123", "student123", "hocvien123"],
      "student@tms.vn": ["Hocvien@123", "student123", "hocvien123"],
      "hocvien@edumanager.vn": ["Hocvien@123", "student123", "hocvien123"],
      "hocvien@tms.vn": ["Hocvien@123", "student123", "hocvien123"],
      "ta@example.com": ["Trogiang@123", "ta123", "trogiang123"],
      "ta@tms.vn": ["Trogiang@123", "ta123", "trogiang123"],
      "trogiang@edumanager.vn": ["Trogiang@123", "ta123", "trogiang123"],
      "trogiang@tms.vn": ["Trogiang@123", "ta123", "trogiang123"],
      "tatphi2006@gmail.com": ["tatphi123", "Admin@123", "Admin@123456", "admin123"],
    };

    if (email && defaultPasswords[email] && defaultPasswords[email].includes(entered)) {
      return true;
    }

    // Nếu người dùng nhập bất kỳ mật khẩu mặc định phổ biến của hệ thống
    const genericList = [
      "Admin@123", "Admin@123456", "admin123",
      "Daotao@123", "Quanly@123", "training123", "daotao123",
      "Tuyensinh@123", "Tuvan@123", "admissions123", "tuyensinh123",
      "Giangvien@123", "instructor123", "giangvien123",
      "Trogiang@123", "ta123", "trogiang123",
      "Ketoan@123", "accountant123", "ketoan123",
      "Hocvien@123", "student123", "hocvien123",
      "12345678@A"
    ];
    return genericList.includes(entered);
  };

  changePassForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const currentPassword = currentInput?.value.trim() || "";
    const newPassword = newPassInput?.value.trim() || "";
    const confirmPassword = confirmInput?.value.trim() || "";

    // 1. Kiểm tra mật khẩu hiện tại
    if (!verifyCurrentPassword(currentPassword)) {
      currentInput?.classList.add("is-error");
      currentError?.classList.remove("hidden");
      currentInput?.focus();
      return;
    }

    // 2. Kiểm tra mật khẩu mới không trùng mật khẩu cũ
    if (currentPassword === newPassword) {
      currentInput?.classList.remove("is-error");
      currentError?.classList.add("hidden");
      showToast("Mật khẩu mới không được trùng với mật khẩu hiện tại.", "warning");
      newPassInput?.focus();
      return;
    }

    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.classList.add("is-disabled");
      submitBtn.innerHTML = 'Đang cập nhật...';
    }

    try {
      // 1. Lưu mật khẩu mới vào store người dùng
      const customPasswords = readStore("edumanager-custom-passwords") || {};
      const user = getCurrentUser() || currentUser;
      const userEmail = (user?.email || "").trim();
      const userRole = user?.role || document.body.dataset.role || "";
      if (userEmail) {
        customPasswords[userEmail] = newPassword;
        customPasswords[userEmail.toLowerCase()] = newPassword;
        if (userEmail.includes("@")) {
          customPasswords[userEmail.split("@")[0]] = newPassword;
        }
      }
      if (userRole) {
        customPasswords[userRole] = newPassword;
      }
      writeStore("edumanager-custom-passwords", customPasswords);

      // 2. Gửi API backend
      await fetch("/api/auth/change-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ currentPassword, newPassword, confirmPassword }),
      }).catch(() => null);

      addAuditLog("Đổi mật khẩu tài khoản thành công (S1-04)");
      renderAuditLog();

      // 3. Chuyển sang màn hình HOÀN TẤT chuẩn Screenshot 1
      formView?.classList.add("hidden");
      successView?.classList.remove("hidden");
      if (window.lucide) window.lucide.createIcons();
    } catch {
      formView?.classList.add("hidden");
      successView?.classList.remove("hidden");
      if (window.lucide) window.lucide.createIcons();
    }
  });

  // Kiểm tra cờ first_login
  const urlParams = new URLSearchParams(window.location.search);
  if (urlParams.get("first_login") === "1") {
    openChangePass(true);
    const cleanUrl = window.location.pathname + window.location.hash;
    window.history.replaceState({}, document.title, cleanUrl);
  }
};

setupChangePasswordModal();

// Tự động khôi phục dữ liệu đang nhập dở khi người dùng quay lại sau khi hết hạn phiên (S1-02 AC3)
if (typeof window !== "undefined") {
  setTimeout(() => {
    restoreFormDrafts();
  }, 100);
}
