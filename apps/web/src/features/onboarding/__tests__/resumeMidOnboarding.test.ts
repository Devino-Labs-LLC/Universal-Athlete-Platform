import { describe, expect, it } from 'vitest';

import { onboardingRouteForState } from '@/features/onboarding/onboardingRoutes';
import { resolveAthleteOnboardingState } from '@/features/onboarding/resolveAthleteOnboardingState';
import type { AthleteProfile } from '@/features/profile/schemas';

describe('resume mid-onboarding routing', () => {
  it('routes profile-only athlete to sports step', () => {
    const profile = { id: 'p1' } as AthleteProfile;
    const state = resolveAthleteOnboardingState({
      isLoading: false,
      hasError: false,
      profile,
      sports: [],
      goals: [],
    });

    expect(state).toBe('SPORTS_REQUIRED');
    expect(onboardingRouteForState(state)).toBe('/onboarding/sports');
  });

  it('routes profile+sports athlete to goals step', () => {
    const profile = { id: 'p1' } as AthleteProfile;
    const state = resolveAthleteOnboardingState({
      isLoading: false,
      hasError: false,
      profile,
      sports: [{ id: 's1' } as never],
      goals: [],
    });

    expect(state).toBe('GOALS_REQUIRED');
    expect(onboardingRouteForState(state)).toBe('/onboarding/goals');
  });
});
