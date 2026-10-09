import subprocess

cfg = subprocess.check_output(['git', 'show', '177b853:frontend/vite.config.js'], text=True, encoding='utf-8')
lines = cfg.splitlines()
start = next(i for i, l in enumerate(lines) if 'function emailPlugin' in l)
end = next(i for i, l in enumerate(lines) if 'const pages =' in l)

plugin_code = '\n'.join(lines[start:end])

new_vite_config = """import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

let nodemailer = null;
try {
  const mod = await import("nodemailer");
  nodemailer = mod.default || mod;
} catch (_) {}

""" + plugin_code + """

// Ứng dụng một trang React Router theo thiết kế Figma + Tích hợp Dev API Middleware cho S1 & S2
export default defineConfig({
  plugins: [react(), emailPlugin()],
});
"""

with open('frontend/vite.config.js', 'w', encoding='utf-8') as f:
    f.write(new_vite_config)

print("Updated frontend/vite.config.js successfully!")
