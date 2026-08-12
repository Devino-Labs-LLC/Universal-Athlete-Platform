import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  fetchAthleteProfile,
  isAthleteProfileNotFound,
} from '@/features/profile/api/athleteApi';

describe('athleteApi', () => {
  it('treats ATHLETE_PROFILE_NOT_FOUND (404) as null profile — legitimate onboarding state', async () => {
    const client = {
      axios: {
        get: async () => {
          throw new ApiError('Athlete profile was not found', {
            category: 'NOT_FOUND',
            status: 404,
            code: 'ATHLETE_PROFILE_NOT_FOUND',
          });
        },
      },
    };

    expect(
      isAthleteProfileNotFound(
        new ApiError('Athlete profile was not found', {
          category: 'NOT_FOUND',
          status: 404,
          code: 'ATHLETE_PROFILE_NOT_FOUND',
        }),
      ),
    ).toBe(true);
    await expect(fetchAthleteProfile(client as never)).resolves.toBeNull();
  });

  it('does not suppress unrelated 404 codes as missing profile', async () => {
    const client = {
      axios: {
        get: async () => {
          throw new ApiError('Other missing resource', {
            category: 'NOT_FOUND',
            status: 404,
            code: 'SOME_OTHER_NOT_FOUND',
          });
        },
      },
    };

    await expect(fetchAthleteProfile(client as never)).rejects.toMatchObject({
      code: 'SOME_OTHER_NOT_FOUND',
    });
  });
});
