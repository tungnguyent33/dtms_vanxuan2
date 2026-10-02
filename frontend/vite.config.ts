import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Khi chay dev (npm run dev), moi request /api/* duoc chuyen tiep sang Spring Boot o cong 8080
// -> khong can cau hinh CORS. Khi trien khai, nginx lam viec nay (xem nginx.conf).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    // antd lon nen bundle ~1.8MB (gzip ~570KB). KHONG tach react/antd bang manualChunks:
    // tach sai thu tu se gay loi "Cannot read properties of undefined (reading 'version')".
    chunkSizeWarningLimit: 2000,
  },
})
