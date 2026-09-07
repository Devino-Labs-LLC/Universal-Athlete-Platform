import type { CoachOverviewSectionStatus } from '@/features/coach/models/schemas';

/** Neutral, non-pressuring copy for consent / data section statuses. */
export function coachSectionStatusMessage(status: CoachOverviewSectionStatus): string {
  switch (status) {
    case 'NOT_SHARED':
      return 'Not shared';
    case 'NO_DATA':
      return 'No data for this date';
    case 'AVAILABLE':
      return 'Available';
    default: {
      const _exhaustive: never = status;
      return _exhaustive;
    }
  }
}
