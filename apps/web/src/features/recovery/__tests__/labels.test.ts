import { describe, expect, it } from 'vitest';

import {
  baselineSufficiencyLabel,
  comparisonBandLabel,
  metricDisplayName,
  metricTypeLabel,
  ratingLabelForMetric,
  readinessBandLabel,
  recommendationActionLabel,
  recommendationStatusLabel,
  trendDirectionLabel,
} from '@/features/recovery/models/labels';

describe('comparisonBandLabel', () => {
  it('maps every backend comparison band to neutral copy', () => {
    expect(comparisonBandLabel('FAR_BELOW_BASELINE')).toBe('Far below baseline');
    expect(comparisonBandLabel('BELOW_BASELINE')).toBe('Below baseline');
    expect(comparisonBandLabel('WITHIN_BASELINE_RANGE')).toBe('Within baseline range');
    expect(comparisonBandLabel('ABOVE_BASELINE')).toBe('Above baseline');
    expect(comparisonBandLabel('FAR_ABOVE_BASELINE')).toBe('Far above baseline');
    expect(comparisonBandLabel('INSUFFICIENT_DATA')).toBe('Insufficient data');
  });

  it('never renders good/bad/healthy/overtrained language', () => {
    const forbidden = /good|bad|healthy|overtrained/i;
    for (const band of [
      'FAR_BELOW_BASELINE',
      'BELOW_BASELINE',
      'WITHIN_BASELINE_RANGE',
      'ABOVE_BASELINE',
      'FAR_ABOVE_BASELINE',
      'INSUFFICIENT_DATA',
    ]) {
      expect(comparisonBandLabel(band)).not.toMatch(forbidden);
    }
  });

  it('falls back to "Not available" for missing bands', () => {
    expect(comparisonBandLabel(null)).toBe('Not available');
    expect(comparisonBandLabel(undefined)).toBe('Not available');
  });
});

describe('baselineSufficiencyLabel', () => {
  it('maps sufficiency codes to the mandated copy', () => {
    expect(baselineSufficiencyLabel('INSUFFICIENT')).toBe('Not enough prior data');
    expect(baselineSufficiencyLabel('LIMITED')).toBe('Limited baseline data');
    expect(baselineSufficiencyLabel('SUFFICIENT')).toBe('Baseline established');
  });

  it('falls back to "Unknown" for missing sufficiency', () => {
    expect(baselineSufficiencyLabel(null)).toBe('Unknown');
  });
});

describe('trendDirectionLabel', () => {
  it('maps known directions', () => {
    expect(trendDirectionLabel('INCREASING')).toBe('Increasing');
    expect(trendDirectionLabel('DECREASING')).toBe('Decreasing');
    expect(trendDirectionLabel('STABLE')).toBe('Stable');
    expect(trendDirectionLabel('INSUFFICIENT_DATA')).toBe('Not enough data');
  });
});

describe('metricTypeLabel', () => {
  it('labels all seven recovery metric types', () => {
    expect(metricTypeLabel('SLEEP_DURATION')).toBe('Sleep duration');
    expect(metricTypeLabel('SLEEP_QUALITY')).toBe('Sleep quality');
    expect(metricTypeLabel('FATIGUE')).toBe('Fatigue');
    expect(metricTypeLabel('MUSCLE_SORENESS')).toBe('Muscle soreness');
    expect(metricTypeLabel('STRESS')).toBe('Stress');
    expect(metricTypeLabel('MOOD')).toBe('Mood');
    expect(metricTypeLabel('MOTIVATION')).toBe('Motivation');
  });

  it('falls back to a formatted enum label for unknown metric types', () => {
    expect(metricTypeLabel('HYDRATION_LEVEL')).toBe('Hydration Level');
  });
});

describe('readinessBandLabel / recommendation labels', () => {
  it('labels readiness bands without evaluative language', () => {
    expect(readinessBandLabel('VERY_LOW')).toBe('Very low');
    expect(readinessBandLabel('VERY_HIGH')).toBe('Very high');
    expect(readinessBandLabel(null)).toBe('Not available');
  });

  it('uses modest, non-mandating recommendation action copy', () => {
    expect(recommendationActionLabel('PROCEED_WITH_MODIFICATIONS')).toBe("Consider modifying today\u2019s session");
    expect(recommendationActionLabel('CONSIDER_REST')).toBe('Consider a rest day');
    expect(recommendationActionLabel(null)).toBe('No recommendation');
  });

  it('labels recommendation statuses', () => {
    expect(recommendationStatusLabel('ACTIVE')).toBe('Active');
    expect(recommendationStatusLabel('SUPERSEDED')).toBe('Superseded');
    expect(recommendationStatusLabel(undefined)).toBe('Unknown');
  });
});

describe('ratingLabelForMetric', () => {
  it('labels fatigue/stress/motivation on a Very Low..Very High scale', () => {
    expect(ratingLabelForMetric('fatigue', 1)).toBe('Very Low');
    expect(ratingLabelForMetric('fatigue', 5)).toBe('Very High');
    expect(ratingLabelForMetric('stress', 3)).toBe('Moderate');
    expect(ratingLabelForMetric('motivation', 4)).toBe('High');
  });

  it('labels muscle soreness distinctly', () => {
    expect(ratingLabelForMetric('muscleSoreness', 1)).toBe('None or minimal');
    expect(ratingLabelForMetric('muscleSoreness', 5)).toBe('Very High');
  });

  it('labels mood and sleep quality distinctly', () => {
    expect(ratingLabelForMetric('mood', 4)).toBe('Good');
    expect(ratingLabelForMetric('sleepQuality', 5)).toBe('Excellent');
  });

  it('falls back to the raw value string for an out-of-range rating', () => {
    expect(ratingLabelForMetric('fatigue', 9)).toBe('9');
  });
});

describe('metricDisplayName', () => {
  it('renders friendly display names for camelCase metric keys', () => {
    expect(metricDisplayName('muscleSoreness')).toBe('Muscle soreness');
    expect(metricDisplayName('sleepQuality')).toBe('Sleep quality');
    expect(metricDisplayName('fatigue')).toBe('Fatigue');
  });
});
