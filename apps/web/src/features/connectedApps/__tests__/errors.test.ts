import { ApiError } from '@/core/api/errors';
import { describe, expect, it } from 'vitest';

import {
  connectedAppsErrorMessage,
  isProviderDisabledError,
} from '@/features/connectedApps/models/errors';

describe('connectedApps errors', () => {
  it('maps provider-disabled and session codes', () => {
    expect(
      connectedAppsErrorMessage(
        new ApiError('disabled', {
          category: 'VALIDATION',
          status: 400,
          code: 'INTEGRATION_PROVIDER_DISABLED',
        }),
      ),
    ).toMatch(/not available yet/i);

    expect(
      connectedAppsErrorMessage(
        new ApiError('expired', { category: 'UNAUTHORIZED', status: 401 }),
      ),
    ).toMatch(/session expired/i);
  });

  it('detects provider disabled errors', () => {
    expect(
      isProviderDisabledError(
        new ApiError('disabled', {
          category: 'VALIDATION',
          status: 400,
          code: 'INTEGRATION_PROVIDER_DISABLED',
        }),
      ),
    ).toBe(true);
    expect(
      isProviderDisabledError(
        new ApiError('other', { category: 'SERVER', status: 500, code: 'OTHER' }),
      ),
    ).toBe(false);
  });
});
