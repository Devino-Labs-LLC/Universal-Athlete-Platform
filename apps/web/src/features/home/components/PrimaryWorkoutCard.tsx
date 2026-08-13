import { useNavigate } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import styles from '@/features/home/components/PrimaryWorkoutCard.module.scss';
import {
  FEASIBILITY_STATUS_LABELS,
  occurrenceStatusLabel,
} from '@/features/home/labels/todayLabels';
import type { TrainingActionFlag, TrainingDashboardOccurrence } from '@/features/home/schemas';

interface PrimaryWorkoutCardProps {
  occurrence: TrainingDashboardOccurrence | null | undefined;
  canStartWorkout?: TrainingActionFlag;
  canContinueWorkout?: TrainingActionFlag;
}

export function PrimaryWorkoutCard({
  occurrence,
}: PrimaryWorkoutCardProps) {
  const navigate = useNavigate();

  if (!occurrence) {
    return (
      <HomeCard title="Today's workout" subtitle="Up next">
        <p className="emptyHint">No workout scheduled for today.</p>
        <Button variant="secondary" onClick={() => navigate('/app/training')}>
          View Training
        </Button>
      </HomeCard>
    );
  }

  const statusLabel = occurrenceStatusLabel(occurrence.status);
  const feasibilityLabel = occurrence.feasibilityStatus
    ? (FEASIBILITY_STATUS_LABELS[occurrence.feasibilityStatus] ?? occurrence.feasibilityStatus)
    : null;

  const occurrencePath = `/app/training/plans/${occurrence.trainingPlanId}/days/${occurrence.workoutDayId}/occurrences/${occurrence.occurrenceId}`;

  return (
    <HomeCard title="Today's workout" subtitle={occurrence.trainingPlanName ?? 'Up next'}>
      <div className={styles.chipRow}>
        <Badge tone={occurrence.status === 'COMPLETED' ? 'success' : occurrence.status === 'IN_PROGRESS' ? 'info' : 'neutral'}>
          {statusLabel}
        </Badge>
        {feasibilityLabel ? <Badge tone="warning">{feasibilityLabel}</Badge> : null}
      </div>
      <p className={styles.metaStrong}>{occurrence.workoutDayName}</p>
      <p className={styles.meta}>
        {occurrence.completedExerciseCount}/{occurrence.exerciseCount} exercises
      </p>
      {occurrence.actualEnvironmentName || occurrence.plannedEnvironmentName ? (
        <p className={styles.meta}>
          Environment: {occurrence.actualEnvironmentName ?? occurrence.plannedEnvironmentName}
        </p>
      ) : (
        <p className={styles.meta}>No environment selected</p>
      )}
      <Button onClick={() => navigate(occurrencePath)}>View workout</Button>
    </HomeCard>
  );
}
