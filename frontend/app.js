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
    email: "hocvien@edumanager.vn",
    password: "123456",
    role: "student",
    fullName: "Nguyễn Văn Học",
  },
  {
    email: "giangvien@edumanager.vn",
    password: "123456",
    role: "instructor",
    fullName: "Trần Thị Giảng",
  },
  {
    email: "trogiang@edumanager.vn",
    password: "123456",
    role: "ta",
    fullName: "Lê Văn Trợ",
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
    password: "123456",
    role: "accountant",
    fullName: "Đỗ Thị Kế",
  },
  {
    email: "admin@edumanager.vn",
    password: "123456",
    role: "admin",
    fullName: "Quản trị viên",
  },
];

const SESSION_KEY = "edumanager-session";
const LOGIN_FORM_KEY = "edumanager-login-form";
const AUTH_TOKEN_KEY = "edumanager-auth-token";
const ROLE_REVOCATION_KEY = "edumanager-role-revocations";
const SESSION_TIMEOUT_MS = 60 * 60 * 1000;

const getSessionData = () => {
  try {
    const data = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? "null");
    if (!data) return null;

    if (Number(data.expiresAt) && Date.now() > Number(data.expiresAt)) {
      sessionStorage.removeItem(SESSION_KEY);
      localStorage.removeItem(AUTH_TOKEN_KEY);
      return null;
    }

    return data;
  } catch {
    return null;
  }
};

const findAccount = (email, password) =>
  DEMO_ACCOUNTS.find(
    (account) =>
      account.email === email.trim().toLowerCase() &&
      account.password === password,
  );

const getCurrentUser = () => getSessionData();

const saveLoginFormState = (email = "", password = "") => {
  localStorage.setItem(
    LOGIN_FORM_KEY,
    JSON.stringify({ email: email.trim(), password: password.trim() }),
  );
};

const restoreLoginFormState = () => {
  const loginFormElement = document.querySelector("#login-form");
  if (!loginFormElement) return;

  try {
    const saved = JSON.parse(localStorage.getItem(LOGIN_FORM_KEY) ?? "{}");
    const emailInput = document.querySelector("#email");
    const passwordInput = document.querySelector("#password");

    if (emailInput && saved.email) emailInput.value = saved.email;
    if (passwordInput && saved.password) passwordInput.value = saved.password;
  } catch {
    localStorage.removeItem(LOGIN_FORM_KEY);
  }
};

function expireSession(message = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.") {
  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  showToast(message, "warning");
  setTimeout(() => goToLogin(), 450);
}

const readRevokedRoles = () => {
  try {
    return new Set(JSON.parse(localStorage.getItem(ROLE_REVOCATION_KEY) ?? "[]"));
  } catch {
    return new Set();
  }
};

const writeRevokedRoles = (roles) => {
  localStorage.setItem(ROLE_REVOCATION_KEY, JSON.stringify([...roles]));
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

const login = (account) => {
  const { password, ...user } = account;
  const session = {
    ...user,
    token: `demo-token-${Date.now()}`,
    expiresAt: Date.now() + SESSION_TIMEOUT_MS,
  };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  localStorage.setItem(AUTH_TOKEN_KEY, session.token);
  localStorage.removeItem(LOGIN_FORM_KEY);
  addAuditLog("Đăng nhập", user.email);
  goToRoleHome(user.role);
};

const logout = () => {
  addAuditLog("Đăng xuất");
  sessionStorage.removeItem(SESSION_KEY);
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(LOGIN_FORM_KEY);
  goToLogin();
};

const goToRoleHome = (role) => {
  navigateTo(ROLES[role]?.page ?? "login");
};

const goToLogin = () => navigateTo("login");
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
    expireSession();
  } else if (readRevokedRoles().has(currentUser.role)) {
    showToast("Vai trò của bạn đã bị thu hồi. Vui lòng liên hệ quản trị viên để được hỗ trợ.", "warning");
    goToLogin();
  } else if (currentUser.role !== pageRole) {
    goToRoleHome(currentUser.role);
  }
}

const sidebar = document.querySelector("#app-sidebar");

if (sidebar && currentUser && ROLES[pageRole]) {
  const role = ROLES[pageRole];
  sidebar.innerHTML = `
    <div class="app-sidebar-brand">
      <span class="app-logo"><i data-lucide="graduation-cap"></i></span>
      <span>EduManager</span>
    </div>
    <div class="app-sidebar-profile">
      <span class="app-avatar">${escapeHtml(getInitials(currentUser.fullName))}</span>
      <div class="min-w-0">
        <p class="app-user-name">${escapeHtml(currentUser.fullName)}</p>
        <p class="app-user-email">${escapeHtml(currentUser.email)}</p>
      </div>
    </div>
    <p class="app-sidebar-role">${role.name} · ${role.en}</p>
    <nav class="app-nav" aria-label="Điều hướng ${role.name}">
      ${role.menu
        .map(
          ([icon, label, href], index) => `
            <a href="${href}" class="${index === 0 ? "is-active" : ""}">
              <i data-lucide="${icon}" aria-hidden="true"></i>${label}
            </a>`,
        )
        .join("")}
    </nav>
    <div class="app-sidebar-footer">
      <button type="button" class="app-logout" data-nav="logout">
        <i data-lucide="log-out" aria-hidden="true"></i>Đăng xuất
      </button>
      <button type="button" class="app-settings" aria-label="Cài đặt">
        <i data-lucide="settings" aria-hidden="true"></i>
        <span>Cài đặt</span>
      </button>
    </div>`;

  document.title = `${role.name} | EduManager`;

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
    handleForbiddenError();
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
        handleForbiddenError();
      }
      return response;
    });
  },
});

document.querySelectorAll("[data-user]").forEach((element) => {
  element.textContent = currentUser?.[element.dataset.user] ?? "";
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
lucide.createIcons();

const formStatus = document.querySelector("#form-status");

const showStatus = (message) => {
  if (!formStatus) return;
  formStatus.textContent = message;
  formStatus.classList.remove("hidden");
};

function showToast(message, tone = "info") {
  if (!message) return;

  let toast = document.querySelector("#app-toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.id = "app-toast";
    toast.className = "app-toast";
    toast.setAttribute("role", "status");
    document.body.appendChild(toast);
  }

  toast.textContent = message;
  toast.dataset.tone = tone;
  toast.classList.add("is-visible");

  window.clearTimeout(showToast.timeoutId);
  showToast.timeoutId = window.setTimeout(() => {
    toast.classList.remove("is-visible");
  }, 3200);
}

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
  const value = emailInput.value.trim();
  const message = !value
    ? "Vui lòng nhập email."
    : !emailInput.validity.valid || !value.includes("@")
      ? "Vui lòng nhập email đúng định dạng, ví dụ ten@domain.com."
      : "";

  setFieldError(emailInput, emailError, message);
  return !message;
};

if (emailInput && emailError) {
  emailInput.setAttribute("aria-describedby", "email-error");
  emailInput.addEventListener("input", () => {
    saveLoginFormState(emailInput.value, passwordInput?.value ?? "");
    if (emailInput.value.trim() && emailInput.validity.valid) {
      setFieldError(emailInput, emailError, "");
    }
  });
}

restoreLoginFormState();

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
    saveLoginFormState(emailInput?.value ?? "", passwordInput.value);
    if (passwordInput.value.trim()) {
      setFieldError(passwordInput, passwordError, "");
    }
  });
}

loginForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  hideStatus();

  const emailIsValid = validateEmail();
  const passwordIsValid = validatePassword();

  if (!emailIsValid || !passwordIsValid) {
    (emailIsValid ? passwordInput : emailInput).focus();
    return;
  }

  saveLoginFormState(emailInput.value, passwordInput.value);

  const account = findAccount(emailInput.value, passwordInput.value);
  if (!account) {
    showStatus("Email hoặc mật khẩu không đúng.");
    return;
  }

  if (readStore("edumanager-locked-accounts").includes(account.email)) {
    showStatus("Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên.");
    return;
  }

  login(account);
});

// ---------------------------------------------------------------------
// 5. Trang quên mật khẩu
// ---------------------------------------------------------------------
const forgotPasswordForm = document.querySelector("#forgot-password-form");
const otpField = document.querySelector("#otp-field");
const otpInput = document.querySelector("#otp");
const otpError = document.querySelector("#otp-error");
const forgotSubmitLabel = document.querySelector("#forgot-submit-label");

// Mã xác nhận dùng thử (CHỈ để demo giao diện).
const DEMO_OTP = "123456";

const validateOtp = () => {
  const value = otpInput.value.trim();
  const message = !value
    ? "Vui lòng nhập mã xác nhận."
    : !/^\d{6}$/.test(value)
      ? "Mã xác nhận gồm 6 chữ số."
      : value !== DEMO_OTP
        ? "Mã xác nhận không đúng."
        : "";

  setFieldError(otpInput, otpError, message);
  return !message;
};

if (otpInput && otpError) {
  otpInput.setAttribute("aria-describedby", "otp-error");
  otpInput.addEventListener("input", () => {
    otpInput.value = otpInput.value.replace(/\D/g, "").slice(0, 6);
    if (otpInput.value) {
      setFieldError(otpInput, otpError, "");
    }
  });
}

forgotPasswordForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  hideStatus();

  // Bước 1: chưa hiện ô mã -> kiểm tra email, gửi mã, hiện ô mã xác nhận.
  if (otpField.classList.contains("hidden")) {
    if (!validateEmail()) {
      emailInput.focus();
      return;
    }

    emailInput.readOnly = true;
    otpField.classList.remove("hidden");
    otpInput.required = true;
    forgotSubmitLabel.textContent = "Xác nhận mã";
    showStatus(
      `Mã xác nhận đã được gửi tới ${emailInput.value.trim()}. (Mã dùng thử: ${DEMO_OTP})`,
    );
    otpInput.focus();
    return;
  }

  // Bước 2: kiểm tra mã xác nhận rồi chuyển sang trang đặt lại mật khẩu.
  if (!validateOtp()) {
    otpInput.focus();
    return;
  }

  navigateTo("reset-password");
});

// ---------------------------------------------------------------------
// 5b. Trang đặt lại mật khẩu
// ---------------------------------------------------------------------
const resetPasswordForm = document.querySelector("#reset-password-form");

if (resetPasswordForm) {
  const newPasswordInput = document.querySelector("#new-password");
  const confirmPasswordInput = document.querySelector("#confirm-password");
  const newPasswordError = document.querySelector("#new-password-error");
  const confirmPasswordError = document.querySelector(
    "#confirm-password-error",
  );
  const passwordRules = {
    length: (value) => value.length >= 8,
    case: (value) => /[a-z]/.test(value) && /[A-Z]/.test(value),
    symbol: (value) => /[0-9]|[^A-Za-z0-9]/.test(value),
  };

  const updatePasswordRules = () => {
    const value = newPasswordInput.value;
    Object.entries(passwordRules).forEach(([rule, test]) => {
      const item = document.querySelector(`[data-password-rule="${rule}"]`);
      item?.classList.toggle("is-met", test(value));
    });
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

  bindPasswordToggle(
    newPasswordInput,
    document.querySelector("#new-password-toggle"),
  );
  bindPasswordToggle(
    confirmPasswordInput,
    document.querySelector("#confirm-password-toggle"),
  );

  newPasswordInput.addEventListener("input", () => {
    updatePasswordRules();
    if (
      Object.values(passwordRules).every((test) => test(newPasswordInput.value))
    ) {
      setFieldError(newPasswordInput, newPasswordError, "");
    }
    if (confirmPasswordInput.value) {
      setFieldError(
        confirmPasswordInput,
        confirmPasswordError,
        confirmPasswordInput.value === newPasswordInput.value
          ? ""
          : "Mật khẩu xác nhận chưa khớp.",
      );
    }
  });

  confirmPasswordInput.addEventListener("input", () => {
    if (confirmPasswordInput.value === newPasswordInput.value) {
      setFieldError(confirmPasswordInput, confirmPasswordError, "");
    }
  });

  resetPasswordForm.addEventListener("submit", (event) => {
    event.preventDefault();
    hideStatus();
    updatePasswordRules();

    const newPassword = newPasswordInput.value;
    const allRulesMet = Object.values(passwordRules).every((test) =>
      test(newPassword),
    );
    const newPasswordMessage = !newPassword
      ? "Vui lòng nhập mật khẩu mới."
      : !allRulesMet
        ? "Mật khẩu chưa đáp ứng đủ các yêu cầu."
        : "";
    const confirmPasswordMessage = !confirmPasswordInput.value
      ? "Vui lòng xác nhận mật khẩu mới."
      : confirmPasswordInput.value !== newPassword
        ? "Mật khẩu xác nhận chưa khớp."
        : "";

    setFieldError(newPasswordInput, newPasswordError, newPasswordMessage);
    setFieldError(
      confirmPasswordInput,
      confirmPasswordError,
      confirmPasswordMessage,
    );

    if (newPasswordMessage || confirmPasswordMessage) {
      (newPasswordMessage ? newPasswordInput : confirmPasswordInput).focus();
      return;
    }

    showStatus("Mật khẩu đã được đặt lại thành công.");
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
  lucide.createIcons();
};

const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");

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

if (accountTable) {
  const lockedAccounts = new Set(readStore("edumanager-locked-accounts"));

  const renderAccounts = () => {
    accountTable.querySelector("tbody").innerHTML = DEMO_ACCOUNTS.map(
      (account) => {
        const locked = lockedAccounts.has(account.email);
        const isSelf = account.email === currentUser?.email;
        return `
        <tr>
          <td class="font-bold">${escapeHtml(account.fullName)}</td>
          <td>${escapeHtml(account.email)}</td>
          <td>${escapeHtml(ROLES[account.role]?.name ?? account.role)}</td>
          <td>${locked ? badge("Đã khoá", "is-danger") : badge("Hoạt động", "is-success")}</td>
          <td>
            <button type="button" class="app-btn app-btn-outline app-btn-sm" data-toggle-account="${account.email}" ${isSelf ? 'disabled title="Không thể tự khoá tài khoản của mình"' : ""}>
              ${locked ? "Mở khoá" : "Khoá"}
            </button>
          </td>
        </tr>`;
      },
    ).join("");
  };

  accountTable.addEventListener("click", (event) => {
    const button = event.target.closest("[data-toggle-account]");
    if (!button) return;
    const email = button.dataset.toggleAccount;
    const locking = !lockedAccounts.has(email);
    locking ? lockedAccounts.add(email) : lockedAccounts.delete(email);
    writeStore("edumanager-locked-accounts", [...lockedAccounts]);
    addAuditLog(`${locking ? "Khoá" : "Mở khoá"} tài khoản ${email}`);
    renderAccounts();
    renderAuditLog();
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
  });
});

const roleTable = document.querySelector("#current-role-table");

if (roleTable) {
  const renderCurrentRoles = () => {
    const revoked = readRevokedRoles();
    roleTable.querySelector("tbody").innerHTML = Object.entries(ROLES)
      .map(([key, role]) => {
        const revokedRole = revoked.has(key);
        const isCurrent = currentUser?.role === key;

        return `
          <tr>
            <td class="font-bold">${escapeHtml(role.name)} <p class="app-muted">${escapeHtml(role.en)}</p></td>
            <td>${escapeHtml(role.page)}</td>
            <td>${revokedRole ? badge("Đã thu hồi", "is-danger") : badge("Hoạt động", "is-success")}</td>
            <td>
              <button
                type="button"
                class="app-btn app-btn-outline app-btn-sm"
                data-role-action="${key}"
                ${isCurrent ? 'disabled title="Vai trò đang được sử dụng ở phiên hiện tại"' : ""}
              >
                ${revokedRole ? "Khôi phục" : "Thu hồi"}
              </button>
            </td>
          </tr>`;
      })
      .join("");
  };

  roleTable.addEventListener("click", (event) => {
    const button = event.target.closest("[data-role-action]");
    if (!button) return;

    const roleKey = button.dataset.roleAction;
    if (roleKey === currentUser?.role) {
      showToast("Không thể thu hồi vai trò đang được sử dụng bởi tài khoản hiện tại.", "warning");
      return;
    }

    const revoked = readRevokedRoles();
    if (revoked.has(roleKey)) {
      revoked.delete(roleKey);
      showToast(`Đã khôi phục quyền ${ROLES[roleKey]?.name ?? roleKey}.`, "success");
    } else {
      revoked.add(roleKey);
      showToast(`Đã thu hồi quyền ${ROLES[roleKey]?.name ?? roleKey}. Tài khoản thuộc vai trò này sẽ nhận thông báo 403 khi truy cập.`, "warning");
    }

    writeRevokedRoles(revoked);
    renderCurrentRoles();
  });

  renderCurrentRoles();
}

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
