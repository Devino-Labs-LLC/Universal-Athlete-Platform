import { loadAppConfig } from '@/src/app/config/env';

describe('loadAppConfig', () => {
  const originalDev = (globalThis as { __DEV__?: boolean }).__DEV__;

  afterEach(() => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = originalDev;
  });

  it('defaults development API URL when unset', () => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = true;
    const config = loadAppConfig({
      EXPO_PUBLIC_UAP_ENV: 'development',
    });

    expect(config.environment).toBe('development');
    expect(config.apiBaseUrl).toBe('http://127.0.0.1:8080');
  });

  it('normalizes trailing slash', () => {
    const config = loadAppConfig({
      EXPO_PUBLIC_UAP_ENV: 'development',
      EXPO_PUBLIC_UAP_API_BASE_URL: 'http://127.0.0.1:8080/',
    });

    expect(config.apiBaseUrl).toBe('http://127.0.0.1:8080');
  });

  it('requires API URL for staging', () => {
    expect(() =>
      loadAppConfig({
        EXPO_PUBLIC_UAP_ENV: 'staging',
      }),
    ).toThrow(/EXPO_PUBLIC_UAP_API_BASE_URL is required/);
  });

  it('requires API URL for production', () => {
    expect(() =>
      loadAppConfig({
        EXPO_PUBLIC_UAP_ENV: 'production',
      }),
    ).toThrow(/EXPO_PUBLIC_UAP_API_BASE_URL is required/);
  });

  it('fails closed when EXPO_PUBLIC_UAP_ENV is missing in release builds', () => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = false;
    expect(() => loadAppConfig({})).toThrow(/EXPO_PUBLIC_UAP_ENV must be explicitly set/);
  });

  it('rejects localhost API URL outside development', () => {
    expect(() =>
      loadAppConfig({
        EXPO_PUBLIC_UAP_ENV: 'production',
        EXPO_PUBLIC_UAP_API_BASE_URL: 'http://127.0.0.1:8080',
      }),
    ).toThrow(/Localhost API URLs are not allowed/);
  });

  it('accepts explicit production HTTPS URL', () => {
    const config = loadAppConfig({
      EXPO_PUBLIC_UAP_ENV: 'production',
      EXPO_PUBLIC_UAP_API_BASE_URL: 'https://api.example.com',
    });
    expect(config.environment).toBe('production');
    expect(config.apiBaseUrl).toBe('https://api.example.com');
  });

  it('rejects insecure HTTP API URL outside development', () => {
    expect(() =>
      loadAppConfig({
        EXPO_PUBLIC_UAP_ENV: 'staging',
        EXPO_PUBLIC_UAP_API_BASE_URL: 'http://api.staging.example.com',
      }),
    ).toThrow(/HTTPS API URLs are required/);
  });
});
