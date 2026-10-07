import { resolve } from "node:path";
import { createHash } from "node:crypto";
import { defineConfig } from "vite";

const pages = [
  "index.html",
  "login.html",
  "ForgotPasswordForm.html",
  "ResetPasswordForm.html",
  "student.html",
  "instructor.html",
  "ta.html",
  "training-manager.html",
  "admissions.html",
  "accountant.html",
  "admin.html",
  "lead-consultation-prototype.html",
];

export default defineConfig({
  css: {
    modules: {
      generateScopedName: (localName, filename) => {
        const safeName = localName.replace(/[^a-zA-Z0-9_-]/g, "_");
        const hash = createHash("sha1")
          .update(`${filename}:${localName}`)
          .digest("hex")
          .slice(0, 6);

        return `u_${safeName}_${hash}`;
      },
    },
  },
  build: {
    rollupOptions: {
      input: Object.fromEntries(
        pages.map((page) => [
          page.replace(/\.html$/, ""),
          resolve(process.cwd(), page),
        ]),
      ),
    },
  },
});
