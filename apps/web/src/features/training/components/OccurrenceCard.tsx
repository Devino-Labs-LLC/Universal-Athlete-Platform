import { Link } from 'react-router-dom';

import { MetricPill } from '@/features/training/components/MetricPill';
import { TrainingStatusBadge } from '@/features/training/components/TrainingStatusBadge';
import { OCCURRENCE_STATUS_LABELS } from '@/features/training/models/labels';
import type { CalendarEntry } from '@/features/training/models/schemas';
import styles from '@/features/training/components/OccurrenceCard.module.scss';

interface OccurrenceCardProps {
  entry: CalendarEntry;
}

export function OccurrenceCard({ entry }: OccurrenceCardProps) {
  const detailPath = `/app/training/plans/${entry.trainingPlanId}/days/${entry.workoutDayId}/occurrences/${entry.occurrenceId}`;

  return (
    <article className={styles.card}>
      <div className={styles.header}>
        <h4 className={styles.title}>
          <Link to={detailPath}>{entry.workoutDayName}</Link>
        </h4>
        <TrainingStatusBadge kind="occurrence" status={entry.status} />
      </div>
      <p className={styles.meta}>
        {entry.trainingPlanName}
        {entry.plannedStartTime ? ` · ${entry.plannedStartTime}` : ''}
        {entry.scheduledDate ? ` · ${entry.scheduledDate}` : ''}
      </p>
      <div className={styles.footer}>
        <MetricPill label="Progress">
          {entry.completedExerciseCount}/{entry.exerciseCount}
        </MetricPill>
        <span className="srOnly">{OCCURRENCE_STATUS_LABELS[entry.status] ?? entry.status}</span>
      </div>
    </article>
  );
}
