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

  it('rejects non-HTTPS API URL outside development', () => {
    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'staging',
        VITE_UAP_API_BASE_URL: 'http://api.example.com',
      }),
    ).toThrow(/HTTPS API URL is required/);
  });

  it('rejects malformed release URLs and embedded credentials', () => {
    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'production',
        VITE_UAP_API_BASE_URL: 'https:///',
      }),
    ).toThrow(/valid absolute API URL/);

    expect(() =>
      loadAppConfig({
        VITE_UAP_ENV: 'production',
        VITE_UAP_API_BASE_URL: 'https://user:secret@api.example.com',
      }),
    ).toThrow(/must not contain embedded credentials/);
  });

  it('rejects IPv6 and wildcard loopback API URLs outside development', () => {
    for (const url of ['https://[::1]', 'https://0.0.0.0']) {
      expect(() =>
        loadAppConfig({
          VITE_UAP_ENV: 'staging',
          VITE_UAP_API_BASE_URL: url,
        }),
      ).toThrow(/Localhost API URLs are not allowed/);
    }
  });

  it('rejects development env in release builds', () => {
    expect(() =>
      loadAppConfig(
        {
          VITE_UAP_ENV: 'development',
          VITE_UAP_API_BASE_URL: '',
        },
        { releaseBuild: true },
      ),
    ).toThrow(/development is not allowed in release builds/);
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
