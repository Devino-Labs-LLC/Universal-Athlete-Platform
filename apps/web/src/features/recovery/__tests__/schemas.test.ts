import { describe, expect, it } from 'vitest';

import {
  athleteRecoveryHistorySchema,
  bodyAreaDiscomfortHistorySchema,
  dailyAthleteStateSnapshotSchema,
  dailyReadinessAssessmentSchema,
  dailyRecoveryCheckInSchema,
  dailyTrainingRecommendationSchema,
  discomfortKey,
  isBaselineWindowDays,
  isRecoveryMetricType,
  isTrendDays,
  recoveryBaselineDashboardSchema,
  recoveryMetricTrendSchema,
  recoveryOverviewSchema,
  trendPointNumericValue,
} from '@/features/recovery/models/schemas';

describe('recovery schema guards', () => {
  it('accepts only 7/14/28 for trend days', () => {
    expect(isTrendDays(7)).toBe(true);
    expect(isTrendDays(14)).toBe(true);
    expect(isTrendDays(28)).toBe(true);
    expect(isTrendDays(21)).toBe(false);
    expect(isTrendDays(0)).toBe(false);
  });

  it('accepts only 7/14/28 for baseline window days', () => {
    expect(isBaselineWindowDays(7)).toBe(true);
    expect(isBaselineWindowDays(28)).toBe(true);
    expect(isBaselineWindowDays(30)).toBe(false);
  });

  it('recognizes the seven recovery metric types and rejects unknown ones', () => {
    expect(isRecoveryMetricType('FATIGUE')).toBe(true);
    expect(isRecoveryMetricType('SLEEP_DURATION')).toBe(true);
    expect(isRecoveryMetricType('MOTIVATION')).toBe(true);
    expect(isRecoveryMetricType('HYDRATION')).toBe(false);
  });

  it('builds a stable discomfort key from area and side', () => {
    expect(discomfortKey('KNEE', 'LEFT')).toBe('KNEE:LEFT');
    expect(discomfortKey('KNEE', 'RIGHT')).not.toBe(discomfortKey('KNEE', 'LEFT'));
  });
});

describe('trendPointNumericValue', () => {
  it('extracts the numeric value from a rating object', () => {
    expect(trendPointNumericValue({ value: 3, label: 'Moderate' })).toBe(3);
  });

  it('passes through a bare number (SLEEP_DURATION)', () => {
    expect(trendPointNumericValue(420)).toBe(420);
  });

  it('coerces a numeric string', () => {
    expect(trendPointNumericValue('7.5' as unknown as number)).toBe(7.5);
  });

  it('returns null for null/undefined values (missing observation, not zero)', () => {
    expect(trendPointNumericValue(null)).toBeNull();
    expect(trendPointNumericValue(undefined)).toBeNull();
  });
});

describe('recovery DTO parsing', () => {
  it('parses a daily recovery check-in with required rating fields', () => {
    const parsed = dailyRecoveryCheckInSchema.parse({
      id: 'ci-1',
      checkInDate: '2026-02-01',
      fatigue: { value: 3, label: 'Moderate' },
      muscleSoreness: { value: 2, label: 'Mild' },
      stress: { value: 3, label: 'Moderate' },
      mood: { value: 4, label: 'Good' },
      motivation: { value: 4, label: 'High' },
      completeness: 'COMPLETE',
      discomfortAreas: [],
      version: 1,
    });
    expect(parsed.id).toBe('ci-1');
    expect(parsed.fatigue.value).toBe(3);
  });

  it('parses a recovery overview envelope with absent readiness/recommendation', () => {
    const parsed = recoveryOverviewSchema.parse({
      date: '2026-02-01',
      trendDays: 7,
      checkInPresent: false,
      checkIn: null,
      baselines: [],
      deviations: [],
      readinessPresent: false,
      readiness: null,
      recommendationPresent: false,
      recommendation: null,
      trends: [],
      discomfort: [],
      trainingLoadContext: null,
    });
    expect(parsed.checkInPresent).toBe(false);
    expect(parsed.readiness).toBeNull();
  });

  it('parses a recovery baseline dashboard envelope', () => {
    const parsed = recoveryBaselineDashboardSchema.parse({
      targetDate: '2026-02-01',
      checkInPresent: true,
      baselineWindowDays: 14,
      baselines: [],
      metricDeviations: [],
      metricTrends: [],
    });
    expect(parsed.baselineWindowDays).toBe(14);
  });

  it('parses a metric trend with points including missing (null) values', () => {
    const parsed = recoveryMetricTrendSchema.parse({
      metricType: 'FATIGUE',
      scaleDirection: 'HIGHER_IS_WORSE',
      startDate: '2026-01-01',
      endDate: '2026-01-28',
      observationCount: 2,
      trendDirection: 'STABLE',
      points: [
        { date: '2026-01-01', value: { value: 3, label: 'Moderate' }, rollingAverage3: null, rollingAverage7: null },
        { date: '2026-01-02', value: null, rollingAverage3: null, rollingAverage7: null },
      ],
    });
    expect(parsed.points).toHaveLength(2);
    expect(trendPointNumericValue(parsed.points[1]!.value)).toBeNull();
  });

  it('parses a bare numeric trend point value for SLEEP_DURATION', () => {
    const parsed = recoveryMetricTrendSchema.parse({
      metricType: 'SLEEP_DURATION',
      scaleDirection: 'LOWER_IS_WORSE',
      startDate: '2026-01-01',
      endDate: '2026-01-07',
      observationCount: 1,
      trendDirection: 'STABLE',
      points: [{ date: '2026-01-01', value: 420, rollingAverage3: null, rollingAverage7: null }],
    });
    expect(trendPointNumericValue(parsed.points[0]!.value)).toBe(420);
  });

  it('parses discomfort history with entries', () => {
    const parsed = bodyAreaDiscomfortHistorySchema.parse({
      startDate: '2026-01-01',
      endDate: '2026-01-31',
      observationCount: 1,
      averageIntensity: 2.5,
      maximumIntensity: 3,
      entries: [
        {
          date: '2026-01-05',
          bodyArea: 'KNEE',
          side: 'LEFT',
          intensity: { value: 3, label: 'Moderate' },
        },
      ],
    });
    expect(parsed.entries).toHaveLength(1);
  });

  it('parses athlete recovery history with a mix of present/absent check-ins', () => {
    const parsed = athleteRecoveryHistorySchema.parse({
      days: [
        { date: '2026-01-01', checkIn: null, trainingLoad: null },
        {
          date: '2026-01-02',
          checkIn: {
            id: 'ci-2',
            checkInDate: '2026-01-02',
            fatigue: { value: 2, label: 'Low' },
            muscleSoreness: { value: 2, label: 'Mild' },
            stress: { value: 2, label: 'Low' },
            mood: { value: 4, label: 'Good' },
            motivation: { value: 4, label: 'High' },
            completeness: 'COMPLETE',
            discomfortAreas: [],
            version: 1,
          },
          trainingLoad: null,
        },
      ],
    });
    expect(parsed.days).toHaveLength(2);
    expect(parsed.days[0]!.checkIn).toBeNull();
    expect(parsed.days[1]!.checkIn?.id).toBe('ci-2');
  });

  it('parses a readiness assessment with contributions and dimensions', () => {
    const parsed = dailyReadinessAssessmentSchema.parse({
      assessmentId: 'ra-1',
      stateDate: '2026-02-01',
      readinessScore: 78.4,
      readinessBand: 'HIGH',
      dataSufficiency: 'SUFFICIENT',
      limitingDimensions: ['FATIGUE'],
      strongestDimensions: ['SLEEP'],
      contributions: [
        {
          dimensionType: 'FATIGUE',
          available: true,
          normalizedScore: 0.8,
          configuredWeight: 0.2,
          effectiveWeight: 0.2,
          weightedContribution: 0.16,
        },
      ],
    });
    expect(parsed.contributions).toHaveLength(1);
    expect(parsed.readinessScore).toBeCloseTo(78.4);
  });

  it('parses a training recommendation with adjustments', () => {
    const parsed = dailyTrainingRecommendationSchema.parse({
      recommendationId: 'rec-1',
      stateDate: '2026-02-01',
      overallAction: 'PROCEED_WITH_MODIFICATIONS',
      recommendationStatus: 'ACTIVE',
      adjustments: [
        { adjustmentId: 'adj-1', type: 'REDUCE_VOLUME', priority: 1, orderIndex: 0 },
      ],
    });
    expect(parsed.adjustments).toHaveLength(1);
    expect(parsed.overallAction).toBe('PROCEED_WITH_MODIFICATIONS');
  });

  it('parses a daily athlete state snapshot with training load and schedule blocks', () => {
    const parsed = dailyAthleteStateSnapshotSchema.parse({
      snapshotId: 'snap-1',
      stateDate: '2026-02-01',
      snapshotVersion: 1,
      current: true,
      recovery: { checkInPresent: false, discomfortObservations: [] },
      recoveryMetrics: [],
      trainingLoad: {
        occurrenceCount: 1,
        completedOccurrenceCount: 1,
        ratedOccurrenceCount: 0,
        unratedOccurrenceCount: 1,
        completedExerciseCount: 5,
        completedSetCount: 15,
        completedRepetitionCount: 120,
        totalVolumeKilograms: 4200,
        totalDurationSeconds: 3600,
        totalDistanceMeters: 0,
        totalSessionRpeLoad: null,
        averageSessionRpe: null,
        totalSessionDurationMinutes: 60,
        noImpactExerciseCount: 0,
        lowImpactExerciseCount: 5,
        moderateImpactExerciseCount: 0,
        highImpactExerciseCount: 0,
        categorySummaries: [],
        movementSummaries: [],
      },
      schedule: {
        scheduledOccurrenceCount: 1,
        scheduledWorkoutCount: 1,
        completedScheduledCount: 1,
        skippedScheduledCount: 0,
        cancelledScheduledCount: 0,
        inProgressScheduledCount: 0,
        scheduledOccurrences: [],
      },
    });
    expect(parsed.trainingLoad.totalSessionRpeLoad).toBeNull();
    expect(parsed.snapshotVersion).toBe(1);
  });
});
