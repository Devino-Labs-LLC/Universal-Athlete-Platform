import {
  isOnboardingIncomplete,
  onboardingRouteForState,
} from '@/src/features/onboarding/onboardingRoutes';

describe('onboarding routing helpers', () => {
  it('maps incomplete states to onboarding routes', () => {
    expect(onboardingRouteForState('PROFILE_REQUIRED')).toBe('/(onboarding)/profile');
    expect(onboardingRouteForState('SPORTS_REQUIRED')).toBe('/(onboarding)/sports');
    expect(onboardingRouteForState('GOALS_REQUIRED')).toBe('/(onboarding)/goals');
  });

  it('returns null for non-actionable states', () => {
    expect(onboardingRouteForState('LOADING')).toBeNull();
    expect(onboardingRouteForState('COMPLETE')).toBeNull();
    expect(onboardingRouteForState('ERROR')).toBeNull();
  });

  it('detects incomplete onboarding states', () => {
    expect(isOnboardingIncomplete('PROFILE_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('SPORTS_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('GOALS_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('COMPLETE')).toBe(false);
  });
});
