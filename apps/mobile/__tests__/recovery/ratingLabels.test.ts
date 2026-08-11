import {
  labelsForMetric,
  metricDisplayName,
  ratingLabelForMetric,
  REQUIRED_RATING_METRICS,
} from '@/src/features/recovery/models/ratingLabels';

describe('ratingLabels', () => {
  it('maps fatigue labels', () => {
    expect(ratingLabelForMetric('fatigue', 4)).toBe('High');
    expect(ratingLabelForMetric('fatigue', 1)).toBe('Very Low');
  });

  it('maps muscle soreness labels distinctly', () => {
    expect(ratingLabelForMetric('muscleSoreness', 1)).toBe('None or minimal');
    expect(ratingLabelForMetric('muscleSoreness', 5)).toBe('Very High');
  });

  it('maps mood and sleep quality labels', () => {
    expect(ratingLabelForMetric('mood', 3)).toBe('Neutral');
    expect(ratingLabelForMetric('sleepQuality', 5)).toBe('Excellent');
  });

  it('exposes five labels per required metric', () => {
    for (const metric of REQUIRED_RATING_METRICS) {
      expect(Object.keys(labelsForMetric(metric))).toHaveLength(5);
      expect(metricDisplayName(metric).length).toBeGreaterThan(0);
    }
  });
});
