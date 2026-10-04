import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api calls go to the Spring Boot backend, so the browser sees a single origin
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
