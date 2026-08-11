import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import { performanceKeys } from '@/features/performance/models/queryKeys';

describe('performanceKeys', () => {
  it('builds a recent records key from days and limit', () => {
    expect(performanceKeys.recentRecords(30, 5)).toEqual(['performance', 'recent-records', 30, 5]);
  });

  it('builds a personal records key that defaults optional filters to null', () => {
    expect(performanceKeys.personalRecords()).toEqual(['performance', 'personal-records', null, null]);
    expect(performanceKeys.personalRecords('key-1', 'HEAVIEST_WEIGHT')).toEqual([
      'performance',
      'personal-records',
      'key-1',
      'HEAVIEST_WEIGHT',
    ]);
  });

  it('scopes exercise records by exercisePerformanceKey (identity), not exercise name', () => {
    expect(performanceKeys.exerciseRecords('key-abc')).toEqual(['performance', 'exercise-records', 'key-abc']);
    expect(performanceKeys.exerciseRecords('key-abc')).not.toEqual(performanceKeys.exerciseRecords('key-def'));
  });

  it('builds a deterministic exercise history key with default pagination', () => {
    expect(performanceKeys.exerciseHistory('key-abc')).toEqual([
      'performance',
      'exercise-history',
      'key-abc',
      null,
      null,
      0,
      20,
    ]);
  });

  it('builds an occurrence summary key scoped by plan/day/occurrence', () => {
    expect(performanceKeys.occurrenceSummary('plan-1', 'day-1', 'occ-1')).toEqual([
      'performance',
      'occurrence-summary',
      'plan-1',
      'day-1',
      'occ-1',
    ]);
  });

  it('builds a load history key including granularity, dates, and filters', () => {
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-28');
    expect(performanceKeys.loadHistory('WEEKLY', start, end)).toEqual([
      'performance',
      'load-history',
      'WEEKLY',
      start,
      end,
      null,
      null,
      null,
      null,
      0,
      50,
    ]);
  });

  it('produces distinct load history keys for different granularities on the same date range', () => {
    const start = parseDateOnly('2026-01-01');
    const end = parseDateOnly('2026-01-28');
    expect(performanceKeys.loadHistory('DAILY', start, end)).not.toEqual(
      performanceKeys.loadHistory('WEEKLY', start, end),
    );
  });
});
