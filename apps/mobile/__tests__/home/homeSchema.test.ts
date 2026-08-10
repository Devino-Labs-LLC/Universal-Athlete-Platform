import { trainingTodayDashboardSchema } from '@/src/features/training/schemas';

import {
  emptyTodayFixture,
  generationActionsFixture,
  inProgressTodayFixture,
  populatedTodayFixture,
} from './fixtures/todayFixtures';

describe('trainingTodayDashboardSchema', () => {
  it('parses empty/minimal today payload', () => {
    const parsed = trainingTodayDashboardSchema.parse({
      date: '2026-08-10',
      recovery: { checkInPresent: false },
      readiness: { readinessPresent: false },
      recommendation: { recommendationPresent: false },
      training: { scheduledOccurrenceCount: 0 },
    });

    expect(parsed.date).toBe('2026-08-10');
    expect(parsed.recovery.checkInPresent).toBe(false);
  });

  it('parses populated fixture with coerced numeric strings', () => {
    const parsed = trainingTodayDashboardSchema.parse(populatedTodayFixture);

    expect(parsed.readiness.readinessScore).toBe(78.5);
    expect(parsed.trainingLoad?.totalVolumeKilograms).toBe(4500.5);
    expect(parsed.trainingLoad?.totalDistanceMeters).toBe(2500);
  });

  it('allows unknown optional fields via passthrough', () => {
    const parsed = trainingTodayDashboardSchema.parse({
      ...emptyTodayFixture,
      futureField: 'ok',
      recovery: { checkInPresent: false, futureRecoveryField: true },
    });

    expect((parsed as { futureField?: string }).futureField).toBe('ok');
  });

  it('parses action flags with nullable reason codes', () => {
    const parsed = trainingTodayDashboardSchema.parse(generationActionsFixture);

    expect(parsed.actions?.canGenerateAthleteStateSnapshot.allowed).toBe(true);
    expect(parsed.actions?.canGenerateReadinessAssessment.reasonCode).toBeUndefined();
  });

  it('parses in-progress occurrence fixture', () => {
    const parsed = trainingTodayDashboardSchema.parse(inProgressTodayFixture);

    expect(parsed.training.primaryOccurrence?.status).toBe('IN_PROGRESS');
    expect(parsed.actions?.canContinueWorkout.allowed).toBe(true);
  });
});
