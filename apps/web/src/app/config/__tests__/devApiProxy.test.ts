import { describe, expect, it, vi } from 'vitest';

import {
  createDevelopmentApiProxy,
  stripProxiedBrowserOrigin,
} from '@/app/config/devApiProxy';
import { loadAppConfig } from '@/app/config/env';

describe('web mobile-browser development API proxy', () => {
  it('keeps development API base same-origin when VITE_UAP_API_BASE_URL is unset', () => {
    const config = loadAppConfig({ VITE_UAP_ENV: 'development' });
    expect(config.apiBaseUrl).toBe('');
    expect(config.apiBaseUrl).not.toContain('127.0.0.1');
    expect(config.apiBaseUrl).not.toContain('localhost');
  });

  it('does not hardcode a browser-facing 127.0.0.1 API base for development', () => {
    const config = loadAppConfig({
      VITE_UAP_ENV: 'development',
      VITE_UAP_API_BASE_URL: undefined,
    });
    expect(config.apiBaseUrl).toBe('');
  });

  it('configures Vite proxy to Spring on loopback with Origin stripping', () => {
    const proxy = createDevelopmentApiProxy();
    expect(proxy.target).toMatch(/127\.0\.0\.1:8080|localhost:8080/);
    expect(proxy.changeOrigin).toBe(true);
    expect(typeof proxy.configure).toBe('function');
  });

  it('strips browser Origin so LAN same-origin /api is not CORS-rejected by Spring', () => {
    const removeHeader = vi.fn();
    stripProxiedBrowserOrigin({ removeHeader });
    expect(removeHeader).toHaveBeenCalledWith('origin');
    expect(removeHeader).toHaveBeenCalledWith('Origin');
  });
});
