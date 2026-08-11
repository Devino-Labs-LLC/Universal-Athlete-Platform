import { describe, expect, it } from 'vitest';

import {
  deriveReadinessBand,
  deriveRecommendationAction,
  deriveTrainingOccurrenceCount,
  todayDashboardSchema,
} from '@/features/home/schemas';

const populatedFixture = {
  date: '2026-08-11',
  recovery: { checkInPresent: true },
  readiness: { readinessPresent: true, readinessBand: 'GREEN' },
  recommendation: { recommendationPresent: true, overallAction: 'TRAIN' },
  training: { scheduledOccurrenceCount: 2 },
};

describe('Home diagnostic schema and derivations', () => {
  it('parses today payload with passthrough fields', () => {
    const parsed = todayDashboardSchema.parse({
      ...populatedFixture,
      futureField: 'ok',
    });

    expect(parsed.date).toBe('2026-08-11');
    expect((parsed as { futureField?: string }).futureField).toBe('ok');
  });

  it('derives diagnostic values safely from nested optionals', () => {
    const parsed = todayDashboardSchema.parse(populatedFixture);

    expect(deriveTrainingOccurrenceCount(parsed)).toBe(2);
    expect(deriveReadinessBand(parsed)).toBe('GREEN');
    expect(deriveRecommendationAction(parsed)).toBe('TRAIN');
  });

  it('returns null derivations when sections are absent', () => {
    const parsed = todayDashboardSchema.parse({
      date: '2026-08-11',
      recovery: { checkInPresent: false },
      readiness: { readinessPresent: false },
      recommendation: { recommendationPresent: false },
      training: { scheduledOccurrenceCount: 0 },
    });

    expect(deriveReadinessBand(parsed)).toBeNull();
    expect(deriveRecommendationAction(parsed)).toBeNull();
  });
});
