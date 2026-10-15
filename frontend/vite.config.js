import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// Ứng dụng một trang (React Router). Máy chủ dev tự trả index.html cho mọi đường dẫn.
// Có VITE_API_URL (ví dụ trong .env.local: VITE_API_URL=http://localhost:8080) thì /api chuyển sang backend Java.
export default defineConfig(({ mode }) => {
  const { VITE_API_URL } = loadEnv(mode, process.cwd());
  return {
    plugins: [react()],
    server: VITE_API_URL ? { proxy: { "/api": { target: VITE_API_URL, changeOrigin: true } } } : undefined,
  };
});
