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
      // Bind all interfaces so phones on the LAN can open the Mac's Vite origin.
      // Mobile browsers must use the Mac LAN IP (never 127.0.0.1 — that is the phone).
      // Keep VITE_UAP_API_BASE_URL unset in development so login stays same-origin /api → proxy.
      host: true,
      port: 3000,
      proxy: {
        '/api': {
          // Proxy runs on the Mac; localhost here is the host Spring process, not the phone.
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
      // Form/userEvent flows often exceed the default 5s under parallel load.
      testTimeout: 20000,
      // Cap parallelism so heavy form suites don't starve each other into timeouts.
      maxWorkers: 2,
    },
  };
});
