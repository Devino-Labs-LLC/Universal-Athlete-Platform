import { describe, expect, it } from 'vitest';

import { resolveAthleteOnboardingState } from '@/features/onboarding/resolveAthleteOnboardingState';
import type { AthleteProfile, AthleteSport, AthleteGoal } from '@/features/profile/schemas';

const profile = { id: 'p1' } as AthleteProfile;
const sport = { id: 's1' } as AthleteSport;
const goal = { id: 'g1' } as AthleteGoal;

describe('resolveAthleteOnboardingState', () => {
  it('returns LOADING when loading', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: true,
        hasError: false,
        profile: null,
        sports: [],
        goals: [],
      }),
    ).toBe('LOADING');
  });

  it('returns ERROR when hasError', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: true,
        profile,
        sports: [sport],
        goals: [goal],
      }),
    ).toBe('ERROR');
  });

  it('returns PROFILE_REQUIRED without profile', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile: null,
        sports: [],
        goals: [],
      }),
    ).toBe('PROFILE_REQUIRED');
  });

  it('returns SPORTS_REQUIRED with profile but no sports', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile,
        sports: [],
        goals: [],
      }),
    ).toBe('SPORTS_REQUIRED');
  });

  it('returns GOALS_REQUIRED with profile and sports but no goals', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile,
        sports: [sport],
        goals: [],
      }),
    ).toBe('GOALS_REQUIRED');
  });

  it('returns COMPLETE when profile, sports, and goals exist', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile,
        sports: [sport],
        goals: [goal],
      }),
    ).toBe('COMPLETE');
  });
});
