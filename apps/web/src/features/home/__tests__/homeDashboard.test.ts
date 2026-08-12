import { describe, expect, it } from 'vitest';

import {
  deriveReadinessBand,
  deriveRecommendationAction,
  deriveTrainingOccurrenceCount,
  todayDashboardSchema,
} from '@/features/home/schemas';
import {
  emptyTodayFixture,
  freshAthleteTodayFixture,
  populatedTodayFixture,
} from '@/features/home/__tests__/fixtures/todayFixtures';

describe('today dashboard schema and derivations', () => {
  it('parses populated today payload', () => {
    const parsed = todayDashboardSchema.parse(populatedTodayFixture);
    expect(parsed.date).toBe('2026-08-11');
    expect(parsed.training.primaryOccurrence?.workoutDayName).toBe('Lower Body');
  });

  it('parses empty today payload', () => {
    const parsed = todayDashboardSchema.parse(emptyTodayFixture);
    expect(parsed.training.scheduledOccurrenceCount).toBe(0);
  });

  it('parses backend-shaped fresh-athlete Today with absent optional sections', () => {
    const parsed = todayDashboardSchema.parse(freshAthleteTodayFixture);
    expect(parsed.training.primaryOccurrence).toBeNull();
    expect(parsed.readiness.readinessPresent).toBe(false);
    expect(parsed.trainingLoad?.loadPresent).toBe(false);
    expect(parsed.adaptation?.activeProposalPresent).toBe(false);
    expect(parsed.recentPerformance).toEqual([]);
  });

  it('derives diagnostic values from populated fixture', () => {
    const parsed = todayDashboardSchema.parse(populatedTodayFixture);
    expect(deriveTrainingOccurrenceCount(parsed)).toBe(1);
    expect(deriveReadinessBand(parsed)).toBe('HIGH');
    expect(deriveRecommendationAction(parsed)).toBe('PROCEED_AS_PLANNED');
  });

  it('returns null derivations when sections are absent', () => {
    const parsed = todayDashboardSchema.parse(emptyTodayFixture);
    expect(deriveReadinessBand(parsed)).toBeNull();
    expect(deriveRecommendationAction(parsed)).toBeNull();
    expect(deriveTrainingOccurrenceCount(parsed)).toBe(0);
  });
});
