import {
  addAthleteSportSchema,
  createAthleteGoalSchema,
  createAthleteProfileSchema,
} from '@/src/features/profile/schemas';

describe('profile schemas', () => {
  it('builds a valid create profile payload', () => {
    const result = createAthleteProfileSchema.safeParse({
      firstName: 'Jordan',
      lastName: 'Lee',
      dateOfBirth: '1998-04-12',
      sex: 'MALE',
      heightCm: 180,
      weightKg: 78.5,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    });

    expect(result.success).toBe(true);
  });

  it('requires custom sport name for OTHER sport type', () => {
    const result = addAthleteSportSchema.safeParse({
      sportType: 'OTHER',
      primarySport: true,
      participationLevel: 'RECREATIONAL',
      yearsExperience: 1,
      seasonStatus: 'YEAR_ROUND',
    });

    expect(result.success).toBe(false);
  });

  it('requires custom goal name for OTHER goal type', () => {
    const result = createAthleteGoalSchema.safeParse({
      goalType: 'OTHER',
      title: 'Custom goal',
      priority: 'HIGH',
    });

    expect(result.success).toBe(false);
  });

  it('accepts practical onboarding goal payload', () => {
    const result = createAthleteGoalSchema.safeParse({
      goalType: 'IMPROVE_STRENGTH',
      title: 'Get stronger',
      priority: 'MEDIUM',
      description: 'Focus on compound lifts',
    });

    expect(result.success).toBe(true);
  });
});
