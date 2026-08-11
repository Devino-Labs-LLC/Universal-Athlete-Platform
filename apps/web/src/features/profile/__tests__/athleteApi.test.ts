import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  fetchAthleteProfile,
  isAthleteProfileNotFound,
} from '@/features/profile/api/athleteApi';

describe('athleteApi', () => {
  it('treats ATHLETE_PROFILE_NOT_FOUND as null profile', async () => {
    const client = {
      axios: {
        get: async () => {
          throw new ApiError('Not found', {
            category: 'NOT_FOUND',
            status: 404,
            code: 'ATHLETE_PROFILE_NOT_FOUND',
          });
        },
      },
    };

    expect(isAthleteProfileNotFound(new ApiError('x', { category: 'NOT_FOUND', code: 'ATHLETE_PROFILE_NOT_FOUND' }))).toBe(true);
    await expect(fetchAthleteProfile(client as never)).resolves.toBeNull();
  });
});
