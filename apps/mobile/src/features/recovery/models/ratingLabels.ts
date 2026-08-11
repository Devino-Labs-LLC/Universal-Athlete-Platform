export type RecoveryRatingMetric =
  | 'fatigue'
  | 'muscleSoreness'
  | 'stress'
  | 'mood'
  | 'motivation'
  | 'sleepQuality';

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

export function ratingLabelForMetric(metric: RecoveryRatingMetric, value: number): string {
  const labels = labelsForMetric(metric);
  return labels[value] ?? String(value);
}

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

export const REQUIRED_RATING_METRICS: RecoveryRatingMetric[] = [
  'fatigue',
  'muscleSoreness',
  'stress',
  'mood',
  'motivation',
];
