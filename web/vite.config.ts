import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Base relativa: o mesmo bundle serve tanto em localhost:8080 (Spring, a
// partir de static/) quanto num deploy estático em subdiretório.
export default defineConfig({
  plugins: [react()],
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // Em desenvolvimento o Vite encaminha /api para o Spring no host,
    // evitando CORS sem afrouxar a configuração da aplicação.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
