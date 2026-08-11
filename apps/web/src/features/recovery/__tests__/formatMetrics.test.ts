import { describe, expect, it } from 'vitest';

import { formatDistance, formatDurationSeconds, formatVolumeKg } from '@/features/recovery/utils/formatMetrics';

describe('formatVolumeKg', () => {
  it('renders kilograms below the tonne threshold', () => {
    expect(formatVolumeKg(420)).toBe('420 kg');
  });

  it('renders tonnes with one decimal at/above 1000kg', () => {
    expect(formatVolumeKg(1500)).toBe('1.5 t');
  });

  it('accepts a numeric string', () => {
    expect(formatVolumeKg('850')).toBe('850 kg');
  });
});

describe('formatDistance', () => {
  it('renders meters below the kilometer threshold', () => {
    expect(formatDistance(400)).toBe('400 m');
  });

  it('renders kilometers with one decimal at/above 1000m', () => {
    expect(formatDistance(2500)).toBe('2.5 km');
  });
});

describe('formatDurationSeconds', () => {
  it('renders minutes for durations under an hour', () => {
    expect(formatDurationSeconds(1800)).toBe('30 min');
  });

  it('renders hours with no minutes when exact', () => {
    expect(formatDurationSeconds(3600)).toBe('1h');
  });

  it('renders hours and minutes when both are present', () => {
    expect(formatDurationSeconds(5400)).toBe('1h 30m');
  });
});
