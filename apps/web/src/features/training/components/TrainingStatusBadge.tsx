import { Badge, type BadgeTone } from '@/core/components/Badge';
import {
  OCCURRENCE_STATUS_LABELS,
  PLAN_STATUS_LABELS,
  SCHEDULE_STATUS_LABELS,
} from '@/features/training/models/labels';

export type TrainingStatusKind = 'plan' | 'schedule' | 'occurrence';

interface TrainingStatusBadgeProps {
  kind: TrainingStatusKind;
  status: string;
}

function labelFor(kind: TrainingStatusKind, status: string): string {
  if (kind === 'plan') {
    return PLAN_STATUS_LABELS[status] ?? status;
  }
  if (kind === 'schedule') {
    return SCHEDULE_STATUS_LABELS[status] ?? status;
  }
  return OCCURRENCE_STATUS_LABELS[status] ?? status;
}

function toneFor(kind: TrainingStatusKind, status: string): BadgeTone {
  switch (status) {
    case 'ACTIVE':
      return kind === 'schedule' ? 'success' : 'accent';
    case 'IN_PROGRESS':
      return 'info';
    case 'SCHEDULED':
      return 'info';
    case 'DRAFT':
      return 'muted';
    case 'PAUSED':
      return 'warning';
    case 'COMPLETED':
      return 'success';
    case 'ARCHIVED':
      return 'neutral';
    case 'SKIPPED':
      return 'warning';
    case 'CANCELLED':
      return 'danger';
    default:
      return 'neutral';
  }
}

export function TrainingStatusBadge({ kind, status }: TrainingStatusBadgeProps) {
  return <Badge tone={toneFor(kind, status)}>{labelFor(kind, status)}</Badge>;
}
