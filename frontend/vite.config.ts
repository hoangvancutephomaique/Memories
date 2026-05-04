import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  // VITE_BASE_PATH is set in CI to /<repo-name>/ for GitHub Pages.
  // Locally it's "/" so nothing changes.
  base: process.env.VITE_BASE_PATH ?? "/",
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
