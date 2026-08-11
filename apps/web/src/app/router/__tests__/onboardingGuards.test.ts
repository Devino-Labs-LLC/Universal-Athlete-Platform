import { describe, expect, it } from 'vitest';

import {
  isOnboardingIncomplete,
  onboardingRouteForState,
} from '@/features/onboarding/onboardingRoutes';

describe('onboarding guard decisions', () => {
  it('blocks app routes when onboarding incomplete', () => {
    expect(isOnboardingIncomplete('PROFILE_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('COMPLETE')).toBe(false);
  });

  it('redirects incomplete athletes to the correct onboarding step', () => {
    expect(onboardingRouteForState('PROFILE_REQUIRED')).toBe('/onboarding/profile');
    expect(onboardingRouteForState('SPORTS_REQUIRED')).toBe('/onboarding/sports');
    expect(onboardingRouteForState('GOALS_REQUIRED')).toBe('/onboarding/goals');
  });

  it('allows complete athletes into app (no onboarding route)', () => {
    expect(onboardingRouteForState('COMPLETE')).toBeNull();
  });
});
