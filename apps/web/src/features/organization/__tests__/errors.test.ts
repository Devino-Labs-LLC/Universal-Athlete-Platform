import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  invitationErrorMessage,
  isInvitationNotFoundError,
} from '@/features/organization/models/errors';

describe('invitationErrorMessage', () => {
  it('maps known invitation codes', () => {
    expect(
      invitationErrorMessage(
        new ApiError('backend', { category: 'NOT_FOUND', status: 404, code: 'INVITATION_NOT_FOUND' }),
      ),
    ).toMatch(/expired|revoked|already been used/i);

    expect(
      invitationErrorMessage(
        new ApiError('backend', {
          category: 'CONFLICT',
          status: 409,
          code: 'ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE',
        }),
      ),
    ).toMatch(/cannot add another active athlete/i);
    expect(
      invitationErrorMessage(
        new ApiError('backend', {
          category: 'CONFLICT',
          status: 409,
          code: 'ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE',
        }),
      ),
    ).not.toMatch(/upgrade|plan|payment/i);
  });

  it('detects invitation not-found errors', () => {
    expect(
      isInvitationNotFoundError(
        new ApiError('missing', { category: 'NOT_FOUND', status: 404, code: 'INVITATION_NOT_FOUND' }),
      ),
    ).toBe(true);
    expect(
      isInvitationNotFoundError(
        new ApiError('conflict', { category: 'CONFLICT', status: 409, code: 'EMAIL_UNVERIFIED' }),
      ),
    ).toBe(false);
  });
});
