import { SCHEDULE_STATUS_LABELS } from '@/features/training/models/labels';
import styles from '@/features/training/components/ScheduleStatusBadge.module.scss';

interface ScheduleStatusBadgeProps {
  status: string;
}

export function ScheduleStatusBadge({ status }: ScheduleStatusBadgeProps) {
  return (
    <span className={styles.badge} data-status={status}>
      {SCHEDULE_STATUS_LABELS[status] ?? status}
    </span>
  );
}
