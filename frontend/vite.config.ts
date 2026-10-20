// defineConfig из vitest/config = обычный конфиг Vite + секция test для тестов
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Dev-сервер на 5173. Все запросы /api/* он сам пересылает в gateway (8080).
// Для браузера фронт и API — один адрес, поэтому CORS не нужен.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
  },
});
