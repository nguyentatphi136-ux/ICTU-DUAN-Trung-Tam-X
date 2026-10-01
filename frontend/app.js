// =====================================================================
// EduManager - script dùng chung cho login.html và ForgotPasswordForm.html
// =====================================================================

// ---------------------------------------------------------------------
// 1. Điều hướng giữa các trang
// ---------------------------------------------------------------------
const PAGES = {
  login: "login.html",
  "forgot-password": "ForgotPasswordForm.html",
};

const navigateTo = (page) => {
  const url = PAGES[page] ?? page;
  window.location.assign(url);
};

// ---------------------------------------------------------------------
// 1b. Role và tài khoản dùng thử (CHỈ để demo giao diện, không dùng thật)
// ---------------------------------------------------------------------
// page: trang đích sau khi đăng nhập. Khi có trang riêng cho từng role,
// chỉ cần đổi "dashboard.html" thành file tương ứng (vd: "student.html").
const ROLES = {
  student: { name: "Học viên", page: "dashboard.html" },
  lecturer: { name: "Giảng viên", page: "dashboard.html" },
  assistant: { name: "Trợ giảng", page: "dashboard.html" },
  "training-manager": { name: "Quản lý đào tạo", page: "dashboard.html" },
  admissions: { name: "Tư vấn tuyển sinh", page: "dashboard.html" },
  accountant: { name: "Kế toán", page: "dashboard.html" },
  admin: { name: "Quản trị hệ thống", page: "dashboard.html" },
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
    role: "lecturer",
    fullName: "Trần Thị Giảng",
  },
  {
    email: "trogiang@edumanager.vn",
    password: "123456",
    role: "assistant",
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

const findAccount = (email, password) =>
  DEMO_ACCOUNTS.find(
    (account) =>
      account.email === email.trim().toLowerCase() &&
      account.password === password,
  );

const getCurrentUser = () => {
  try {
    return JSON.parse(sessionStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
};

const login = (account) => {
  const { password, ...user } = account;
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user));
  goToRoleHome(user.role);
};

const logout = () => {
  sessionStorage.removeItem(SESSION_KEY);
  goToLogin();
};

const goToRoleHome = (role) => {
  const target = ROLES[role]?.page ?? "dashboard.html";
  navigateTo(`${target}?role=${encodeURIComponent(role)}`);
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

// Mọi phần tử có data-nav="login" | "forgot-password" | "back" sẽ tự điều hướng.
document.querySelectorAll("[data-nav]").forEach((element) => {
  element.addEventListener("click", (event) => {
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

loginForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  hideStatus();

  const emailIsValid = validateEmail();
  const passwordIsValid = validatePassword();

  if (!emailIsValid || !passwordIsValid) {
    (emailIsValid ? passwordInput : emailInput).focus();
    return;
  }

  const account = findAccount(emailInput.value, passwordInput.value);
  if (!account) {
    showStatus("Email hoặc mật khẩu không đúng.");
    return;
  }

  login(account);
});

// ---------------------------------------------------------------------
// 4b. Trang dashboard: chặn khi chưa đăng nhập, hiển thị thông tin role
// ---------------------------------------------------------------------
const dashboard = document.querySelector("#dashboard");

if (dashboard) {
  const user = getCurrentUser();
  if (!user) {
    goToLogin();
  } else {
    const role = ROLES[user.role];
    document.querySelector("#user-name").textContent = user.fullName;
    document.querySelector("#user-email").textContent = user.email;
    document.querySelector("#user-role").textContent = role?.name ?? user.role;
    document.title = `${role?.name ?? "Trang chủ"} | EduManager`;
  }
}

// ---------------------------------------------------------------------
// 5. Trang quên mật khẩu
// ---------------------------------------------------------------------
const forgotPasswordForm = document.querySelector("#forgot-password-form");

forgotPasswordForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  hideStatus();

  if (!validateEmail()) {
    emailInput.focus();
    return;
  }

  showStatus(
    "Liên kết đặt lại mật khẩu đã được chuẩn bị. Vui lòng kiểm tra email của bạn.",
  );
});

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
