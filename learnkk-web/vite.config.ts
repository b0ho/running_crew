import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// FE/BE 분리 저장소 — dev 프록시로 /api 를 백엔드로 전달(세션 쿠키 same-origin 처리)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
