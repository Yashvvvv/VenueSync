import path from "path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react-swc";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    strictPort: true, // Fail if port 5173 is already in use instead of picking another
    proxy: {
      // Proxy /api to json-server
      // "/api": {
      //   target: "http://localhost:3000",
      //   changeOrigin: true,
      //   rewrite: (path) => path.replace(/^\/api\/v1/, ""),
      // },

      // Proxy /api to Spring Boot
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
