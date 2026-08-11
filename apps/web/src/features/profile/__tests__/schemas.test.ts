import { describe, expect, it } from 'vitest';

import {
  addAthleteSportSchema,
  athleteProfileSchema,
  createAthleteGoalSchema,
  createAthleteProfileSchema,
} from '@/features/profile/schemas';

describe('athlete schemas', () => {
  it('parses athlete profile fixture', () => {
    const parsed = athleteProfileSchema.parse({
      id: 'athlete-1',
      firstName: 'Alex',
      lastName: 'Runner',
      dateOfBirth: '1995-03-15',
      sex: 'MALE',
      heightCm: '180',
      weightKg: '75.5',
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    });

    expect(parsed.heightCm).toBe(180);
    expect(parsed.weightKg).toBe(75.5);
  });

  it('validates create profile request', () => {
    const parsed = createAthleteProfileSchema.parse({
      firstName: 'Alex',
      lastName: 'Runner',
      dateOfBirth: '1995-03-15',
      sex: 'MALE',
      heightCm: 180,
      weightKg: 75,
      dominantHand: 'RIGHT',
      dominantFoot: 'RIGHT',
    });

    expect(parsed.firstName).toBe('Alex');
  });

  it('requires custom sport name for OTHER', () => {
    const result = addAthleteSportSchema.safeParse({
      sportType: 'OTHER',
      primarySport: true,
      participationLevel: 'RECREATIONAL',
      yearsExperience: 1,
      seasonStatus: 'YEAR_ROUND',
    });

    expect(result.success).toBe(false);
  });

  it('accepts non-OTHER sport without custom name', () => {
    const result = addAthleteSportSchema.safeParse({
      sportType: 'RUNNING',
      primarySport: true,
      participationLevel: 'RECREATIONAL',
      yearsExperience: 1,
      seasonStatus: 'YEAR_ROUND',
    });

    expect(result.success).toBe(true);
  });

  it('requires custom goal name for OTHER', () => {
    const result = createAthleteGoalSchema.safeParse({
      goalType: 'OTHER',
      title: 'My goal',
      priority: 'MEDIUM',
    });

    expect(result.success).toBe(false);
  });

  it('accepts simple goal create payload', () => {
    const result = createAthleteGoalSchema.safeParse({
      goalType: 'GENERAL_FITNESS',
      title: 'Stay active',
      priority: 'MEDIUM',
    });

    expect(result.success).toBe(true);
  });
});
