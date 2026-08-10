import { resolveAthleteOnboardingState } from '@/src/features/onboarding/resolveAthleteOnboardingState';

describe('resolveAthleteOnboardingState', () => {
  it('returns LOADING while data is loading', () => {
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

  it('returns ERROR when loading failed', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: true,
        profile: null,
        sports: [],
        goals: [],
      }),
    ).toBe('ERROR');
  });

  it('returns PROFILE_REQUIRED when profile is missing', () => {
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

  it('returns SPORTS_REQUIRED when profile exists but sports are empty', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile: {
          id: 'a1',
          firstName: 'Ada',
          lastName: 'Lovelace',
          dateOfBirth: '1990-01-01',
          sex: 'FEMALE',
          heightCm: 170,
          weightKg: 65,
          dominantHand: 'RIGHT',
          dominantFoot: 'RIGHT',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
        sports: [],
        goals: [],
      }),
    ).toBe('SPORTS_REQUIRED');
  });

  it('returns GOALS_REQUIRED when sports exist but goals are empty', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile: {
          id: 'a1',
          firstName: 'Ada',
          lastName: 'Lovelace',
          dateOfBirth: '1990-01-01',
          sex: 'FEMALE',
          heightCm: 170,
          weightKg: 65,
          dominantHand: 'RIGHT',
          dominantFoot: 'RIGHT',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
        sports: [
          {
            id: 's1',
            sportType: 'RUNNING',
            customSportName: null,
            primarySport: true,
            participationLevel: 'RECREATIONAL',
            preferredPosition: null,
            yearsExperience: 2,
            seasonStatus: 'YEAR_ROUND',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        goals: [],
      }),
    ).toBe('GOALS_REQUIRED');
  });

  it('returns COMPLETE when profile, sport, and goal exist', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile: {
          id: 'a1',
          firstName: 'Ada',
          lastName: 'Lovelace',
          dateOfBirth: '1990-01-01',
          sex: 'FEMALE',
          heightCm: 170,
          weightKg: 65,
          dominantHand: 'RIGHT',
          dominantFoot: 'RIGHT',
          status: 'ACTIVE',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
        sports: [
          {
            id: 's1',
            sportType: 'RUNNING',
            customSportName: null,
            primarySport: true,
            participationLevel: 'RECREATIONAL',
            preferredPosition: null,
            yearsExperience: 2,
            seasonStatus: 'YEAR_ROUND',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        goals: [
          {
            id: 'g1',
            goalType: 'GENERAL_FITNESS',
            customGoalName: null,
            title: 'Stay fit',
            description: null,
            priority: 'MEDIUM',
            status: 'ACTIVE',
            targetValue: null,
            targetUnit: null,
            customTargetUnit: null,
            targetDate: null,
            athleteSportId: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
            completedAt: null,
          },
        ],
      }),
    ).toBe('COMPLETE');
  });
});
