import { comparisonBandLabel, trendDirectionLabel } from '@/src/features/recovery/models/recoveryLabels';
import { deviationSummaryCopy } from '@/src/features/recovery/utils/comparisonCopy';

describe('comparisonCopy', () => {
  it('builds factual deviation summary', () => {
    const copy = deviationSummaryCopy('MOOD', 'WITHIN_BASELINE_RANGE');
    expect(copy).toContain('Mood');
    expect(copy.toLowerCase()).toContain('within your recent baseline');
  });

  it('uses fatigue-specific factual phrasing', () => {
    expect(deviationSummaryCopy('FATIGUE', 'ABOVE_BASELINE')).toBe(
      'Above your usual reported fatigue',
    );
  });

  it('maps comparison band and trend labels', () => {
    expect(comparisonBandLabel('FAR_ABOVE_BASELINE')).toBe('Far above your recent baseline');
    expect(trendDirectionLabel('INCREASING')).toBe('Increasing');
  });
});
