import { loadAppConfig } from '@/src/app/config/env';

describe('loadAppConfig', () => {
  it('defaults development API URL when unset', () => {
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
});
