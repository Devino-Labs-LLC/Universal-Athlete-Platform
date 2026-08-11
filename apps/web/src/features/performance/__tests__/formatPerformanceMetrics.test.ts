import { describe, expect, it } from 'vitest';

import {
  formatPerformanceMetricsSummary,
  performanceMetricsPrIndicators,
} from '@/features/performance/utils/formatPerformanceMetrics';
import type { ExercisePerformanceMetrics } from '@/features/performance/models/schemas';

function baseMetrics(overrides: Partial<ExercisePerformanceMetrics>): ExercisePerformanceMetrics {
  return {
    completedSetCount: 0,
    totalRepetitions: null,
    mostRepetitionsInSet: null,
    heaviestWeight: null,
    bestEstimatedOneRepMax: null,
    bestSetVolume: null,
    totalVolume: null,
    longestSetDurationSeconds: null,
    totalDurationSeconds: null,
    longestSetDistance: null,
    totalDistance: null,
    averageRpe: null,
    ...overrides,
  };
}

describe('formatPerformanceMetricsSummary', () => {
  it('joins available metric fragments with a middle dot', () => {
    const metrics = baseMetrics({
      completedSetCount: 3,
      totalRepetitions: 30,
      heaviestWeight: { measuredValue: 100, measuredUnit: 'KILOGRAM' },
      averageRpe: 7.5,
    });
    expect(formatPerformanceMetricsSummary(metrics)).toBe('3 sets \u00b7 30 reps \u00b7 100 kg \u00b7 RPE 7.5');
  });

  it('reports "No metrics recorded" when nothing is available', () => {
    expect(formatPerformanceMetricsSummary(baseMetrics({}))).toBe('No metrics recorded');
  });

  it('omits duration when not present but includes it when set', () => {
    const withDuration = baseMetrics({ completedSetCount: 1, totalDurationSeconds: 120 });
    expect(formatPerformanceMetricsSummary(withDuration)).toContain('2 min');
  });
});

describe('performanceMetricsPrIndicators', () => {
  it('flags an estimated 1RM indicator distinctly from a heaviest-weight PR', () => {
    const metrics = baseMetrics({ bestEstimatedOneRepMax: { measuredValue: 150, estimated: true } });
    expect(performanceMetricsPrIndicators(metrics)).toEqual(['Est. 1RM']);
  });

  it('flags heaviest weight only when not itself an estimate', () => {
    const metrics = baseMetrics({ heaviestWeight: { measuredValue: 140, estimated: false } });
    expect(performanceMetricsPrIndicators(metrics)).toContain('Heaviest');
  });

  it('returns an empty array when no PR indicators apply', () => {
    expect(performanceMetricsPrIndicators(baseMetrics({}))).toEqual([]);
  });
});
