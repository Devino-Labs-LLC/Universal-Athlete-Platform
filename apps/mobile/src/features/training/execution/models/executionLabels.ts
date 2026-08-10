import { formatEnumLabel } from '@/src/features/profile/enumLabels';

export const SET_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  SKIPPED: 'Skipped',
};

export const EXECUTION_STATUS_LABELS: Record<string, string> = {
  NOT_STARTED: 'Not started',
  IN_PROGRESS: 'In progress',
  COMPLETED: 'Completed',
  SKIPPED: 'Skipped',
};

export function setStatusLabel(status: string | null | undefined): string {
  if (!status) {
    return 'Unknown';
  }
  return SET_STATUS_LABELS[status] ?? formatEnumLabel(status);
}

export function executionStatusLabel(status: string | null | undefined): string {
  if (!status) {
    return 'Unknown';
  }
  return EXECUTION_STATUS_LABELS[status] ?? formatEnumLabel(status);
}

export function setStatusVariant(
  status: string,
): 'default' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'IN_PROGRESS':
      return 'info';
    case 'SKIPPED':
      return 'warning';
    default:
      return 'default';
  }
}

export function executionStatusVariant(
  status: string,
): 'default' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'IN_PROGRESS':
      return 'info';
    case 'SKIPPED':
      return 'warning';
    default:
      return 'default';
  }
}
