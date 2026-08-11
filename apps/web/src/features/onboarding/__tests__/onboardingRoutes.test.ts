import { describe, expect, it } from 'vitest';

import {
  isOnboardingIncomplete,
  onboardingRouteForState,
  onboardingStepIndex,
} from '@/features/onboarding/onboardingRoutes';

describe('onboardingRoutes', () => {
  it('maps required states to routes', () => {
    expect(onboardingRouteForState('PROFILE_REQUIRED')).toBe('/onboarding/profile');
    expect(onboardingRouteForState('SPORTS_REQUIRED')).toBe('/onboarding/sports');
    expect(onboardingRouteForState('GOALS_REQUIRED')).toBe('/onboarding/goals');
  });

  it('returns null for non-actionable states', () => {
    expect(onboardingRouteForState('COMPLETE')).toBeNull();
    expect(onboardingRouteForState('LOADING')).toBeNull();
    expect(onboardingRouteForState('ERROR')).toBeNull();
  });

  it('detects incomplete onboarding', () => {
    expect(isOnboardingIncomplete('PROFILE_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('SPORTS_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('GOALS_REQUIRED')).toBe(true);
    expect(isOnboardingIncomplete('COMPLETE')).toBe(false);
  });

  it('returns step indices for stepper', () => {
    expect(onboardingStepIndex('PROFILE_REQUIRED')).toBe(0);
    expect(onboardingStepIndex('SPORTS_REQUIRED')).toBe(1);
    expect(onboardingStepIndex('GOALS_REQUIRED')).toBe(2);
    expect(onboardingStepIndex('COMPLETE')).toBe(-1);
  });
});
