import type { BadgeTone } from '@/core/components/Badge';

/** Badge tone for readiness bands — text always accompanies color. */
export function readinessBandBadgeTone(band: string | null | undefined): BadgeTone {
  switch (band) {
    case 'VERY_HIGH':
    case 'HIGH':
      return 'success';
    case 'MODERATE':
      return 'warning';
    case 'LOW':
    case 'VERY_LOW':
      return 'danger';
    case 'INSUFFICIENT_DATA':
      return 'muted';
    default:
      return 'neutral';
  }
}

/** ScoreRing tone mapped from readiness band. */
export function readinessRingTone(
  band: string | null | undefined,
): 'accent' | 'cyan' | 'warning' | 'danger' | 'muted' {
  switch (band) {
    case 'VERY_HIGH':
    case 'HIGH':
      return 'accent';
    case 'MODERATE':
      return 'warning';
    case 'LOW':
    case 'VERY_LOW':
      return 'danger';
    default:
      return 'muted';
  }
}

export function comparisonBandBadgeTone(band: string | null | undefined): BadgeTone {
  switch (band) {
    case 'WITHIN_BASELINE_RANGE':
      return 'success';
    case 'ABOVE_BASELINE':
    case 'BELOW_BASELINE':
      return 'info';
    case 'FAR_ABOVE_BASELINE':
    case 'FAR_BELOW_BASELINE':
      return 'warning';
    case 'INSUFFICIENT_DATA':
      return 'muted';
    default:
      return 'neutral';
  }
}

export function discomfortIntensityTone(intensity: number): BadgeTone {
  if (intensity >= 8) {
    return 'danger';
  }
  if (intensity >= 5) {
    return 'warning';
  }
  return 'neutral';
}

export function trendDirectionBadgeTone(direction: string | null | undefined): BadgeTone {
  switch (direction) {
    case 'INCREASING':
      return 'warning';
    case 'DECREASING':
      return 'info';
    case 'STABLE':
      return 'success';
    default:
      return 'muted';
  }
}
