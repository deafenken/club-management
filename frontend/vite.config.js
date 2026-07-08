import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  // Force root to this directory — prevents 404 when Vite is started
  // from the wrong working directory (e.g. D:\ instead of D:\club-management\frontend).
  root: __dirname,
  // Always treat as a SPA so /club-list etc. fall back to index.html.
  appType: 'spa',
  server: {
    port: 5173,
    // Bind 127.0.0.1 ONLY — on Windows, 0.0.0.0:5173 and 127.0.0.1:5173
    // can coexist as separate sockets, which breaks strictPort.  Binding
    // a single address makes strictPort reliable: if port 5173 is busy on
    // 127.0.0.1, Vite refuses to start instead of silently coexisting with
    // a zombie and serving 404.
    host: '127.0.0.1',
    // Refuse to start if port 5173 is already in use.
    strictPort: true,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
