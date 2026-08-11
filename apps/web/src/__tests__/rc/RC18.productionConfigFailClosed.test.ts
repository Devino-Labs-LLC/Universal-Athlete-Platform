import { describe, expect, it } from 'vitest';

import { loadAppConfig } from '@/app/config/env';

// Full env.ts branch coverage lives in src/app/config/__tests__/env.test.ts.
// This file is a thin, RC-ID-traceable re-assertion of the three release
// fail-closed rules called out for the RC hardening pass.
describe('RC18 — production config fails closed', () => {
  it('rejects VITE_UAP_ENV=development in a release build', () => {
    expect(() => loadAppConfig({ VITE_UAP_ENV: 'development' }, { releaseBuild: true })).toThrow(
      /development is not allowed in release builds/,
    );
  });

  it('rejects a non-HTTPS API URL for staging/production', () => {
    expect(() =>
      loadAppConfig({ VITE_UAP_ENV: 'staging', VITE_UAP_API_BASE_URL: 'http://staging.example.com' }),
    ).toThrow(/HTTPS API URL is required/);
  });

  it('rejects a missing VITE_UAP_ENV in a release build', () => {
    expect(() => loadAppConfig({}, { releaseBuild: true })).toThrow(/VITE_UAP_ENV must be explicitly set/);
  });
});
