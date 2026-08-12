import { describe, expect, it } from 'vitest';

import {
  comparisonBandBadgeTone,
  discomfortIntensityTone,
  readinessBandBadgeTone,
  readinessRingTone,
  trendDirectionBadgeTone,
} from '@/features/recovery/utils/readinessVisual';

describe('readinessVisual', () => {
  it('maps readiness bands to semantic badge and ring tones', () => {
    expect(readinessBandBadgeTone('HIGH')).toBe('success');
    expect(readinessBandBadgeTone('MODERATE')).toBe('warning');
    expect(readinessBandBadgeTone('VERY_LOW')).toBe('danger');
    expect(readinessRingTone('HIGH')).toBe('accent');
    expect(readinessRingTone('MODERATE')).toBe('warning');
    expect(readinessRingTone(null)).toBe('muted');
  });

  it('maps comparison bands and trend directions without inventing statuses', () => {
    expect(comparisonBandBadgeTone('WITHIN_BASELINE_RANGE')).toBe('success');
    expect(comparisonBandBadgeTone('FAR_ABOVE_BASELINE')).toBe('warning');
    expect(trendDirectionBadgeTone('STABLE')).toBe('success');
    expect(trendDirectionBadgeTone('INCREASING')).toBe('warning');
  });

  it('uses danger only for high discomfort intensity', () => {
    expect(discomfortIntensityTone(3)).toBe('neutral');
    expect(discomfortIntensityTone(5)).toBe('warning');
    expect(discomfortIntensityTone(9)).toBe('danger');
  });
});
