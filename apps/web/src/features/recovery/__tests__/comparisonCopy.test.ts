import { describe, expect, it } from 'vitest';

import { deviationSummaryCopy, trendSummaryCopy } from '@/features/recovery/utils/comparisonCopy';

describe('deviationSummaryCopy', () => {
  it('uses neutral fatigue-specific phrasing for above/below baseline', () => {
    expect(deviationSummaryCopy('FATIGUE', 'ABOVE_BASELINE')).toBe('Above your usual reported fatigue');
    expect(deviationSummaryCopy('FATIGUE', 'FAR_ABOVE_BASELINE')).toBe('Above your usual reported fatigue');
    expect(deviationSummaryCopy('FATIGUE', 'BELOW_BASELINE')).toBe('Below your usual reported fatigue');
    expect(deviationSummaryCopy('FATIGUE', 'FAR_BELOW_BASELINE')).toBe('Below your usual reported fatigue');
  });

  it('falls back to "metric: band" copy for within-range fatigue', () => {
    expect(deviationSummaryCopy('FATIGUE', 'WITHIN_BASELINE_RANGE')).toBe('Fatigue: Within baseline range');
  });

  it('uses generic "metric: band" copy for non-fatigue metrics', () => {
    expect(deviationSummaryCopy('SLEEP_QUALITY', 'ABOVE_BASELINE')).toBe('Sleep quality: Above baseline');
    expect(deviationSummaryCopy('MOOD', 'INSUFFICIENT_DATA')).toBe('Mood: Insufficient data');
  });

  it('never emits good/bad/healthy/overtrained language', () => {
    const forbidden = /good|bad|healthy|overtrained/i;
    expect(deviationSummaryCopy('FATIGUE', 'FAR_ABOVE_BASELINE')).not.toMatch(forbidden);
    expect(deviationSummaryCopy('STRESS', 'FAR_ABOVE_BASELINE')).not.toMatch(forbidden);
  });
});

describe('trendSummaryCopy', () => {
  it('renders a lowercase trend direction sentence', () => {
    expect(trendSummaryCopy('FATIGUE', 'INCREASING')).toBe('Fatigue trend: increasing.');
    expect(trendSummaryCopy('SLEEP_DURATION', 'INSUFFICIENT_DATA')).toBe('Sleep duration trend: insufficient data.');
  });

  it('replaces underscores with spaces in the direction', () => {
    expect(trendSummaryCopy('MOOD', 'STABLE')).toBe('Mood trend: stable.');
  });
});
