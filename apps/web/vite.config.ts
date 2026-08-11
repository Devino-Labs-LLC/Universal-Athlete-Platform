/// <reference types="vitest/config" />
import path from 'node:path';

import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

import { loadAppConfig } from './src/app/config/env';

export default defineConfig(({ command, mode }) => {
  if (command === 'build') {
    loadAppConfig(loadEnv(mode, __dirname, 'VITE_UAP_'), { releaseBuild: true });
  }

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      sourcemap: false,
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      globals: true,
    },
  };
});
