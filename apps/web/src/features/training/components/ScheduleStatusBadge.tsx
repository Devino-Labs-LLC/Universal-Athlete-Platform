import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';

interface ScheduleStatusBadgeProps {
  status: string;
}

export function ScheduleStatusBadge({ status }: ScheduleStatusBadgeProps) {
  return <TrainingStatusBadge kind="schedule" status={status} />;
}
