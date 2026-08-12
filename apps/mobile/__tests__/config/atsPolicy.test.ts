import appConfig from '../../app.config';

describe('iOS ATS local-networking policy', () => {
  const originalEnv = process.env.EXPO_PUBLIC_UAP_ENV;

  afterEach(() => {
    if (originalEnv === undefined) {
      delete process.env.EXPO_PUBLIC_UAP_ENV;
    } else {
      process.env.EXPO_PUBLIC_UAP_ENV = originalEnv;
    }
  });

  it('permits NSAllowsLocalNetworking only for development builds', () => {
    process.env.EXPO_PUBLIC_UAP_ENV = 'development';
    const config = appConfig({ config: {} as never });
    expect(config.ios?.infoPlist?.NSAppTransportSecurity?.NSAllowsLocalNetworking).toBe(true);
    expect(config.android?.usesCleartextTraffic).toBe(true);
  });

  it('does not attach local HTTP ATS exceptions for staging', () => {
    process.env.EXPO_PUBLIC_UAP_ENV = 'staging';
    const config = appConfig({ config: {} as never });
    expect(config.ios?.infoPlist).toBeUndefined();
    expect(config.android?.usesCleartextTraffic).toBeUndefined();
  });

  it('does not attach local HTTP ATS exceptions for production', () => {
    process.env.EXPO_PUBLIC_UAP_ENV = 'production';
    const config = appConfig({ config: {} as never });
    expect(config.ios?.infoPlist).toBeUndefined();
    expect(config.android?.usesCleartextTraffic).toBeUndefined();
  });
});
