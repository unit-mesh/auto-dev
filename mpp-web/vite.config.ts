import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  root: './',
  publicDir: 'public',
  resolve: {
    alias: {
      // Browser polyfills for Node.js modules used by Kotlin/JS dependencies
      'ws': path.resolve(__dirname, 'src/polyfills/ws-polyfill.ts'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
    commonjsOptions: {
      include: [/node_modules/, /kotlin/],
      transformMixedEsModules: true,
    },
  },
  server: {
    port: 3000,
    fs: {
      // Allow serving files from mpp-core build directory
      allow: ['..'],
    },
  },
  optimizeDeps: {
    include: ['@autodev/mpp-core'],
    esbuildOptions: {
      // Define empty modules for Node.js-only dependencies
      define: {
        global: 'globalThis',
      },
    },
  },
  define: {
    // Provide global shims for CommonJS-style code
    'global': 'globalThis',
    'process.env.NODE_ENV': JSON.stringify(process.env.NODE_ENV || 'development'),
  },
});

