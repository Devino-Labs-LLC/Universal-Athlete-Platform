import type { BadgeTone } from '@/core/components/Badge';

/** Badge tone for occurrence/exercise completion status. */
export function performanceStatusBadgeTone(status: string | null | undefined): BadgeTone {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'IN_PROGRESS':
      return 'info';
    case 'SKIPPED':
    case 'CANCELLED':
      return 'muted';
    default:
      return 'neutral';
  }
}
