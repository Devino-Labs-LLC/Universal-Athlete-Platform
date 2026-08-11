import { formatEnumLabel } from '@/features/profile/enumLabels';

export const COMPARISON_BAND_LABELS: Record<string, string> = {
  FAR_BELOW_BASELINE: 'Far below baseline',
  BELOW_BASELINE: 'Below baseline',
  WITHIN_BASELINE_RANGE: 'Within baseline range',
  ABOVE_BASELINE: 'Above baseline',
  FAR_ABOVE_BASELINE: 'Far above baseline',
  INSUFFICIENT_DATA: 'Insufficient data',
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

export const RECOVERY_METRIC_TYPE_LABELS: Record<string, string> = {
  SLEEP_DURATION: 'Sleep duration',
  SLEEP_QUALITY: 'Sleep quality',
  FATIGUE: 'Fatigue',
  MUSCLE_SORENESS: 'Muscle soreness',
  STRESS: 'Stress',
  MOOD: 'Mood',
  MOTIVATION: 'Motivation',
};

export const READINESS_BAND_LABELS: Record<string, string> = {
  VERY_LOW: 'Very low',
  LOW: 'Low',
  MODERATE: 'Moderate',
  HIGH: 'High',
  VERY_HIGH: 'Very high',
  INSUFFICIENT_DATA: 'Insufficient data',
};

export const RECOMMENDATION_ACTION_LABELS: Record<string, string> = {
  PROCEED_AS_PLANNED: 'Proceed as planned',
  PROCEED_WITH_MODIFICATIONS: 'Consider modifying today\u2019s session',
  CONSIDER_REST: 'Consider a rest day',
  INSUFFICIENT_DATA: 'Not enough data for a recommendation',
};

export const RECOMMENDATION_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Active',
  SUPERSEDED: 'Superseded',
  ACKNOWLEDGED: 'Acknowledged',
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
  return RECOVERY_METRIC_TYPE_LABELS[metricType] ?? formatEnumLabel(metricType);
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

export function readinessBandLabel(band: string | null | undefined): string {
  if (!band) {
    return 'Not available';
  }
  return READINESS_BAND_LABELS[band] ?? formatEnumLabel(band);
}

export function recommendationActionLabel(action: string | null | undefined): string {
  if (!action) {
    return 'No recommendation';
  }
  return RECOMMENDATION_ACTION_LABELS[action] ?? formatEnumLabel(action);
}

export function recommendationStatusLabel(status: string | null | undefined): string {
  if (!status) {
    return 'Unknown';
  }
  return RECOMMENDATION_STATUS_LABELS[status] ?? formatEnumLabel(status);
}

export function adjustmentTypeLabel(type: string): string {
  return formatEnumLabel(type);
}

export function occurrenceStatusLabel(status: string): string {
  return formatEnumLabel(status);
}

export const RATING_METRICS = [
  'fatigue',
  'muscleSoreness',
  'stress',
  'mood',
  'motivation',
  'sleepQuality',
] as const;
export type RecoveryRatingMetric = (typeof RATING_METRICS)[number];

const FATIGUE_STRESS_MOTIVATION_LABELS: Record<number, string> = {
  1: 'Very Low',
  2: 'Low',
  3: 'Moderate',
  4: 'High',
  5: 'Very High',
};

const MUSCLE_SORENESS_LABELS: Record<number, string> = {
  1: 'None or minimal',
  2: 'Mild',
  3: 'Moderate',
  4: 'High',
  5: 'Very High',
};

const MOOD_LABELS: Record<number, string> = {
  1: 'Very Low',
  2: 'Low',
  3: 'Neutral',
  4: 'Good',
  5: 'Very Good',
};

const SLEEP_QUALITY_LABELS: Record<number, string> = {
  1: 'Very Poor',
  2: 'Poor',
  3: 'Fair',
  4: 'Good',
  5: 'Excellent',
};

export function labelsForMetric(metric: RecoveryRatingMetric): Record<number, string> {
  switch (metric) {
    case 'muscleSoreness':
      return MUSCLE_SORENESS_LABELS;
    case 'mood':
      return MOOD_LABELS;
    case 'sleepQuality':
      return SLEEP_QUALITY_LABELS;
    default:
      return FATIGUE_STRESS_MOTIVATION_LABELS;
  }
}

export function ratingLabelForMetric(metric: RecoveryRatingMetric, value: number): string {
  const labels = labelsForMetric(metric);
  return labels[value] ?? String(value);
}

export function metricDisplayName(metric: RecoveryRatingMetric): string {
  switch (metric) {
    case 'muscleSoreness':
      return 'Muscle soreness';
    case 'sleepQuality':
      return 'Sleep quality';
    default:
      return metric.charAt(0).toUpperCase() + metric.slice(1);
  }
}
