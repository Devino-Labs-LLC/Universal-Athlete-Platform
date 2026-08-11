import { describe, expect, it } from 'vitest';

import {
  formatDailyLoadSummary,
  formatLoadVolume,
  formatOccurrenceLoadSummary,
  formatRatedUnratedSummary,
  formatSessionRpeLoad,
  formatWeeklyLoadSummary,
  isOccurrenceRated,
} from '@/features/performance/utils/formatLoadMetrics';
import type { WeeklyTrainingLoadSummary, WorkoutOccurrenceLoadSummary } from '@/features/performance/models/schemas';

describe('formatSessionRpeLoad — null is not zero', () => {
  it('returns null (not "0.0") for a missing session RPE load', () => {
    expect(formatSessionRpeLoad(null)).toBeNull();
    expect(formatSessionRpeLoad(undefined)).toBeNull();
  });

  it('formats a present session RPE load as a decimal', () => {
    expect(formatSessionRpeLoad(320)).toBe('320.0');
  });

  it('formats a zero session RPE load distinctly from a missing one', () => {
    expect(formatSessionRpeLoad(0)).toBe('0.0');
  });
});

describe('formatLoadVolume', () => {
  it('returns null for a missing volume rather than "0 kg"', () => {
    expect(formatLoadVolume(null)).toBeNull();
    expect(formatLoadVolume(undefined)).toBeNull();
  });

  it('formats a present volume', () => {
    expect(formatLoadVolume(500)).toBe('500 kg');
  });
});

describe('formatRatedUnratedSummary', () => {
  it('reports only nonzero counts', () => {
    expect(formatRatedUnratedSummary({ ratedOccurrenceCount: 3, unratedOccurrenceCount: 0 })).toBe('3 rated');
    expect(formatRatedUnratedSummary({ ratedOccurrenceCount: 0, unratedOccurrenceCount: 2 })).toBe('2 unrated');
    expect(formatRatedUnratedSummary({ ratedOccurrenceCount: 1, unratedOccurrenceCount: 1 })).toBe('1 rated \u00b7 1 unrated');
  });
});

describe('formatDailyLoadSummary', () => {
  it('shows "Session load: not rated" when unrated occurrences exist and load is null', () => {
    const lines = formatDailyLoadSummary({
      ratedOccurrenceCount: 0,
      unratedOccurrenceCount: 1,
      totalDurationSeconds: 0,
      totalSessionRpeLoad: null,
    });
    expect(lines).toContain('Session load: not rated');
    expect(lines.some((line) => line.includes('Session load: 0'))).toBe(false);
  });

  it('shows the numeric session load when it is present, even when it is zero', () => {
    const lines = formatDailyLoadSummary({
      ratedOccurrenceCount: 1,
      unratedOccurrenceCount: 0,
      totalDurationSeconds: 0,
      totalSessionRpeLoad: 0,
    });
    expect(lines).toContain('Session load: 0.0');
  });

  it('omits duration and distance lines when they are zero (nothing recorded)', () => {
    const lines = formatDailyLoadSummary({
      ratedOccurrenceCount: 1,
      unratedOccurrenceCount: 0,
      totalDurationSeconds: 0,
      totalDistanceMeters: 0,
      totalSessionRpeLoad: 250,
    });
    expect(lines.some((line) => line.startsWith('Duration:'))).toBe(false);
    expect(lines.some((line) => line.startsWith('Distance:'))).toBe(false);
  });

  it('includes volume and average RPE lines when present', () => {
    const lines = formatDailyLoadSummary({
      ratedOccurrenceCount: 1,
      unratedOccurrenceCount: 0,
      totalVolumeKilograms: 320,
      totalDurationSeconds: 1800,
      totalSessionRpeLoad: 250,
      averageSessionRpe: 7,
    });
    expect(lines).toContain('Volume: 320 kg');
    expect(lines).toContain('Duration: 30 min');
    expect(lines).toContain('Avg RPE: 7.0');
  });
});

describe('formatWeeklyLoadSummary', () => {
  it('prefixes the training day count', () => {
    const summary: WeeklyTrainingLoadSummary = {
      weekStartDate: '2026-01-26',
      weekEndDate: '2026-02-01',
      trainingDays: 4,
      occurrenceCount: 4,
      ratedOccurrenceCount: 4,
      unratedOccurrenceCount: 0,
      completedExerciseCount: 20,
      completedSetCount: 60,
      completedRepetitionCount: 500,
      totalDurationSeconds: 7200,
      totalSessionRpeLoad: 1200,
      totalSessionDurationMinutes: 120,
      noImpactExerciseCount: 0,
      lowImpactExerciseCount: 20,
      moderateImpactExerciseCount: 0,
      highImpactExerciseCount: 0,
    };
    const lines = formatWeeklyLoadSummary(summary);
    expect(lines[0]).toBe('4 training days');
  });
});

describe('formatOccurrenceLoadSummary / isOccurrenceRated — null session RPE load is not zero', () => {
  const baseOccurrence: WorkoutOccurrenceLoadSummary = {
    id: 'occ-load-1',
    trainingPlanId: 'plan-1',
    workoutDayId: 'day-1',
    workoutOccurrenceId: 'occ-1',
    scheduledDate: '2026-02-01',
    sessionRpe: null,
    sessionRpeLoad: null,
    totalDurationSeconds: 1800,
  };

  it('reports the session as unrated when sessionRpeLoad is null', () => {
    expect(isOccurrenceRated(baseOccurrence)).toBe(false);
    expect(formatOccurrenceLoadSummary(baseOccurrence)).toContain('Session load: not rated');
  });

  it('reports the session as rated once a session RPE load (even 0) is present', () => {
    const rated: WorkoutOccurrenceLoadSummary = { ...baseOccurrence, sessionRpeLoad: 0, sessionRpe: 5 };
    expect(isOccurrenceRated(rated)).toBe(true);
    expect(formatOccurrenceLoadSummary(rated)).toContain('Session load: 0.0');
    expect(formatOccurrenceLoadSummary(rated)).toContain('RPE: 5.0');
  });

  it('omits distance when zero and includes it when present', () => {
    const withDistance: WorkoutOccurrenceLoadSummary = { ...baseOccurrence, totalDistanceMeters: 400 };
    expect(formatOccurrenceLoadSummary(withDistance)).toContain('Distance: 400 m');
    expect(formatOccurrenceLoadSummary(baseOccurrence).some((line) => line.startsWith('Distance:'))).toBe(false);
  });
});
