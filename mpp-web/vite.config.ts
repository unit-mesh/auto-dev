import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  root: './',
  publicDir: 'public',
  resolve: {
    alias: {
      '@autodev/mpp-core': path.resolve(__dirname, '../mpp-core/build/compileSync/js/main/productionLibrary/kotlin/autodev-mpp-core.js'),
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: true,
  },
  server: {
    port: 3000,
  },
});

