import { resolveAthleteOnboardingState } from '@/src/features/onboarding/resolveAthleteOnboardingState';

describe('resume mid-onboarding', () => {
  const profile = {
    id: 'a1',
    firstName: 'Sam',
    lastName: 'Taylor',
    dateOfBirth: '1995-05-05',
    sex: 'UNKNOWN' as const,
    heightCm: 175,
    weightKg: 72,
    dominantHand: 'RIGHT' as const,
    dominantFoot: 'BOTH' as const,
    status: 'ACTIVE',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  };

  it('routes a returning athlete with profile but no sports to sports step', () => {
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

  it('routes a returning athlete with sports but no goals to goals step', () => {
    expect(
      resolveAthleteOnboardingState({
        isLoading: false,
        hasError: false,
        profile,
        sports: [
          {
            id: 's1',
            sportType: 'BASKETBALL',
            customSportName: null,
            primarySport: true,
            participationLevel: 'RECREATIONAL',
            preferredPosition: null,
            yearsExperience: 3,
            seasonStatus: 'IN_SEASON',
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-01T00:00:00Z',
          },
        ],
        goals: [],
      }),
    ).toBe('GOALS_REQUIRED');
  });
});
