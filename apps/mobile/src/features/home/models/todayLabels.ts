import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export const READINESS_BAND_LABELS: Record<string, string> = {
  HIGH: 'High',
  MODERATE: 'Moderate',
  LOW: 'Low',
  INSUFFICIENT_DATA: 'Not enough data',
};

export const RECOMMENDATION_ACTION_LABELS: Record<string, string> = {
  PROCEED_AS_PLANNED: 'Proceed as planned',
  MODIFY_SESSION: 'Modify session',
  CONSIDER_RECOVERY_SESSION: 'Consider recovery session',
  NO_SCHEDULED_TRAINING: 'No scheduled training',
  INSUFFICIENT_DATA: 'Not enough data',
  TRAINING_ALREADY_COMPLETED: 'Training already completed',
};

export const OCCURRENCE_STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Scheduled',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  SKIPPED: 'Skipped',
  CANCELLED: 'Cancelled',
};

export const FEASIBILITY_STATUS_LABELS: Record<string, string> = {
  FULLY_FEASIBLE: 'Fully feasible',
  PARTIALLY_FEASIBLE: 'Partially feasible',
  NOT_FEASIBLE: 'Not feasible',
  NO_ENVIRONMENT_CONTEXT: 'No environment context',
  NO_EXERCISES: 'No exercises',
};

export function readinessBandLabel(band: string | null | undefined): string {
  if (!band) {
    return 'Not available';
  }
  return READINESS_BAND_LABELS[band] ?? formatEnumLabel(band);
}

export function recommendationActionLabel(action: string | null | undefined): string {
  if (!action) {
    return 'Not available';
  }
  return RECOMMENDATION_ACTION_LABELS[action] ?? formatEnumLabel(action);
}

export function occurrenceStatusLabel(status: string | null | undefined): string {
  if (!status) {
    return 'Unknown';
  }
  return OCCURRENCE_STATUS_LABELS[status] ?? formatEnumLabel(status);
}

export function adjustmentTypeLabel(type: string): string {
  return formatEnumLabel(type);
}

export function personalRecordTypeLabel(type: string): string {
  return formatEnumLabel(type);
}
