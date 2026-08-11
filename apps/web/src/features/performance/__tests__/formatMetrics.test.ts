import { describe, expect, it } from 'vitest';

import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/features/performance/utils/formatMetrics';

describe('formatVolumeKg', () => {
  it('renders kilograms below the tonne threshold', () => {
    expect(formatVolumeKg(999)).toBe('999 kg');
  });

  it('renders tonnes with one decimal at/above 1000kg', () => {
    expect(formatVolumeKg(4500)).toBe('4.5 t');
  });
});

describe('formatDistance', () => {
  it('renders meters below the kilometer threshold', () => {
    expect(formatDistance(999)).toBe('999 m');
  });

  it('renders kilometers with one decimal at/above 1000m', () => {
    expect(formatDistance(5000)).toBe('5.0 km');
  });
});

describe('formatDurationSeconds', () => {
  it('renders minutes under an hour', () => {
    expect(formatDurationSeconds(600)).toBe('10 min');
  });

  it('renders hours and minutes when both present', () => {
    expect(formatDurationSeconds(4500)).toBe('1h 15m');
  });
});

describe('formatDecimal', () => {
  it('formats with one decimal place by default', () => {
    expect(formatDecimal(7)).toBe('7.0');
    expect(formatDecimal('7.5')).toBe('7.5');
  });

  it('supports a custom decimal count', () => {
    expect(formatDecimal(7.256, 2)).toBe('7.26');
  });
});
