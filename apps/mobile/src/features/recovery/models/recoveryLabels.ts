import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export const COMPARISON_BAND_LABELS: Record<string, string> = {
  FAR_BELOW_BASELINE: 'Far below your recent baseline',
  BELOW_BASELINE: 'Below your recent baseline',
  WITHIN_BASELINE_RANGE: 'Within your recent baseline range',
  ABOVE_BASELINE: 'Above your recent baseline',
  FAR_ABOVE_BASELINE: 'Far above your recent baseline',
  INSUFFICIENT_DATA: 'Not enough data',
};

export const BASELINE_SUFFICIENCY_LABELS: Record<string, string> = {
  INSUFFICIENT: 'Not enough prior data',
  LIMITED: 'Limited baseline data',
  SUFFICIENT: 'Baseline established',
};

export const TREND_DIRECTION_LABELS: Record<string, string> = {
  INCREASING: 'Increasing',
  DECREASING: 'Decreasing',
  STABLE: 'Stable',
  INSUFFICIENT_DATA: 'Not enough data',
};

export const BODY_AREAS = [
  'HEAD',
  'NECK',
  'SHOULDER',
  'UPPER_ARM',
  'ELBOW',
  'FOREARM',
  'WRIST',
  'HAND',
  'UPPER_BACK',
  'LOWER_BACK',
  'CHEST',
  'ABDOMEN',
  'HIP',
  'GROIN',
  'THIGH',
  'KNEE',
  'LOWER_LEG',
  'ANKLE',
  'FOOT',
  'GENERAL_FULL_BODY',
  'OTHER',
] as const;

export const BODY_SIDES = ['LEFT', 'RIGHT', 'BILATERAL', 'CENTER', 'NOT_APPLICABLE'] as const;

export type BodyArea = (typeof BODY_AREAS)[number];
export type BodySide = (typeof BODY_SIDES)[number];

export function comparisonBandLabel(band: string | null | undefined): string {
  if (!band) {
    return 'Not available';
  }
  return COMPARISON_BAND_LABELS[band] ?? formatEnumLabel(band);
}

export function baselineSufficiencyLabel(sufficiency: string | null | undefined): string {
  if (!sufficiency) {
    return 'Unknown';
  }
  return BASELINE_SUFFICIENCY_LABELS[sufficiency] ?? formatEnumLabel(sufficiency);
}

export function trendDirectionLabel(direction: string | null | undefined): string {
  if (!direction) {
    return 'Unknown';
  }
  return TREND_DIRECTION_LABELS[direction] ?? formatEnumLabel(direction);
}

export function metricTypeLabel(metricType: string): string {
  return formatEnumLabel(metricType);
}

export function bodyAreaLabel(area: string): string {
  return formatEnumLabel(area);
}

export function bodySideLabel(side: string): string {
  return formatEnumLabel(side);
}

export function readinessDimensionLabel(dimension: string): string {
  return formatEnumLabel(dimension);
}
