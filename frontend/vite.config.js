import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// Ứng dụng một trang (React Router). Máy chủ dev tự trả index.html cho mọi đường dẫn.
export default defineConfig({
  plugins: [react()],
});
