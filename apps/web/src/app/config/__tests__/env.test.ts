import { describe, expect, it } from 'vitest';

import { loadAppConfig } from '@/app/config/env';

describe('loadAppConfig', () => {
  it('defaults development API URL to same-origin empty string when unset', () => {
    const config = loadAppConfig({
      VITE_UAP_ENV: 'development',
    });

    expect(config.environment).toBe('development');
    expect(config.apiBaseUrl).toBe('');
  });

  it('normalizes trailing slash', () => {
    const config = loadAppConfig({
      VITE_UAP_ENV: 'development',
      VITE_UAP_API_BASE_URL: 'https://api.example.com/',
    });

    expect(config.apiBaseUrl).toBe('https://api.example.com');
  });

  it('requires API URL for staging', () => {
    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'staging',
      }),
    ).toThrow(/VITE_UAP_API_BASE_URL is required/);
  });

  it('requires API URL for production', () => {
    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'production',
      }),
    ).toThrow(/VITE_UAP_API_BASE_URL is required/);
  });

  it('fails closed when VITE_UAP_ENV is missing in release builds', () => {
    expect(() => loadAppConfig({}, { releaseBuild: true })).toThrow(
      /VITE_UAP_ENV must be explicitly set/,
    );
  });

  it('rejects localhost API URL outside development', () => {
    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'production',
        VITE_UAP_API_BASE_URL: 'http://127.0.0.1:8080',
      }),
    ).toThrow(/Localhost API URLs are not allowed/);
  });

  it('accepts explicit production HTTPS URL', () => {
    const config = loadAppConfig({
      VITE_UAP_ENV: 'production',
      VITE_UAP_API_BASE_URL: 'https://api.example.com',
    });

    expect(config.environment).toBe('production');
    expect(config.apiBaseUrl).toBe('https://api.example.com');
  });
});
