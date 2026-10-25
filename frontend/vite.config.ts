import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// In development the Vite server proxies /api to the Spring Boot backend, so the browser sees ONE origin.
// That keeps the HttpOnly refresh cookie working (SameSite=Strict) with no CORS setup.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.API_PROXY_TARGET ?? 'http://localhost:8080',
    },
  },
  build: {
    sourcemap: false,
    target: 'es2022',
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
