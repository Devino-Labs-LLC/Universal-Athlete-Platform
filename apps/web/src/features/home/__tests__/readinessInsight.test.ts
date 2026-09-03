import { describe, expect, it } from 'vitest';

import {
  MISSING_INTELLIGENCE_COPY,
  missingReadinessStep,
  missingRecommendationStep,
  readinessExplanationLines,
} from '@/features/home/labels/readinessInsight';

describe('readinessInsight', () => {
  it('distinguishes missing check-in, state, and readiness', () => {
    expect(
      missingReadinessStep({ checkInPresent: false, snapshotPresent: false }),
    ).toBe('recovery_check_in_not_collected');
    expect(
      missingReadinessStep({ checkInPresent: true, snapshotPresent: false }),
    ).toBe('athlete_state_not_generated');
    expect(
      missingReadinessStep({ checkInPresent: true, snapshotPresent: true }),
    ).toBe('readiness_not_generated');
  });

  it('does not treat a missing recommendation as generic no-data', () => {
    expect(
      missingRecommendationStep({
        checkInPresent: true,
        snapshotPresent: true,
        readinessPresent: true,
      }),
    ).toBe('recommendation_not_generated');
    expect(MISSING_INTELLIGENCE_COPY.recommendation_not_generated).toMatch(/not been generated/);
  });

  it('explains limiting dimensions without inventing causality', () => {
    expect(
      readinessExplanationLines({
        readinessBand: 'MODERATE',
        limitingDimensions: ['MUSCLE_SORENESS', 'SLEEP_DURATION'],
      }),
    ).toEqual([
      "Muscle soreness is a limiting factor from today's evidence.",
      "Sleep duration is a limiting factor from today's evidence.",
    ]);
  });

  it('states limited data honestly and never fabricates a score', () => {
    expect(
      readinessExplanationLines({
        readinessBand: 'INSUFFICIENT_DATA',
        dataSufficiency: 'INSUFFICIENT',
        limitingDimensions: [],
      }),
    ).toEqual(['Limited data available. This is not a precise readiness result.']);
  });
});
