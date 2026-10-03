import { ApiError } from '@/src/core/api/errors';
import { connectedAppsErrorMessage } from '@/src/features/connectedApps/models/errors';

describe('connectedAppsErrorMessage', () => {
  it('maps backend CSRF_INVALID distinctly from authorization forbidden', () => {
    const message = connectedAppsErrorMessage(
      new ApiError('CSRF token is missing or invalid', {
        category: 'forbidden',
        status: 403,
        code: 'CSRF_INVALID',
      }),
    );
    expect(message).toMatch(/session security token/i);
    expect(message).not.toMatch(/permission to manage connected apps/i);
  });

  it('maps client CSRF_TOKEN_UNAVAILABLE fail-closed error', () => {
    const message = connectedAppsErrorMessage(
      new ApiError('CSRF token is unavailable for this authenticated session', {
        category: 'unknown',
        code: 'CSRF_TOKEN_UNAVAILABLE',
      }),
    );
    expect(message).toMatch(/could not be prepared/i);
  });

  it('keeps authorization copy for generic forbidden without CSRF code', () => {
    const message = connectedAppsErrorMessage(
      new ApiError('Access is denied', {
        category: 'forbidden',
        status: 403,
        code: 'ACCESS_DENIED',
      }),
    );
    expect(message).toBe('You do not have permission to manage connected apps.');
  });
});
