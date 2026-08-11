import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export const PERSONAL_RECORD_TYPE_LABELS: Record<string, string> = {
  HEAVIEST_WEIGHT: 'Heaviest Weight',
  MOST_REPETITIONS: 'Most Reps',
  MOST_REPETITIONS_AT_WEIGHT: 'Most Reps at Weight',
  HIGHEST_ESTIMATED_ONE_REP_MAX: 'Estimated 1RM',
  HIGHEST_SET_VOLUME: 'Highest Set Volume',
  LONGEST_DURATION: 'Longest Duration',
  LONGEST_DISTANCE: 'Longest Distance',
};

export const TRAINING_LOAD_GRANULARITY_LABELS: Record<string, string> = {
  OCCURRENCE: 'Sessions',
  DAILY: 'Daily',
  WEEKLY: 'Weekly',
};

export const LOAD_RANGE_LABELS: Record<string, string> = {
  '7D': '7 days',
  '28D': '28 days',
  '90D': '90 days',
};

export function personalRecordTypeLabel(type: string | null | undefined): string {
  if (!type) {
    return 'Personal record';
  }
  return PERSONAL_RECORD_TYPE_LABELS[type] ?? formatEnumLabel(type);
}

export function trainingLoadGranularityLabel(granularity: string): string {
  return TRAINING_LOAD_GRANULARITY_LABELS[granularity] ?? formatEnumLabel(granularity);
}

export function loadRangeLabel(range: string): string {
  return LOAD_RANGE_LABELS[range] ?? range;
}
