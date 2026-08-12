import { useNavigate } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
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
  canStartWorkout,
  canContinueWorkout,
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

  const showContinue = canContinueWorkout?.allowed && occurrence.status === 'IN_PROGRESS';
  const showStart =
    canStartWorkout?.allowed &&
    (occurrence.status === 'SCHEDULED' || occurrence.status === 'IN_PROGRESS');

  return (
    <HomeCard title="Today's workout" subtitle={occurrence.trainingPlanName ?? 'Up next'}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
        <span className="card" style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}>
          {statusLabel}
        </span>
        {feasibilityLabel ? (
          <span className="card" style={{ padding: '0.25rem 0.75rem', fontSize: '0.875rem' }}>
            {feasibilityLabel}
          </span>
        ) : null}
      </div>
      <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
        <strong>{occurrence.workoutDayName}</strong>
      </p>
      <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
        {occurrence.completedExerciseCount}/{occurrence.exerciseCount} exercises
      </p>
      {occurrence.actualEnvironmentName || occurrence.plannedEnvironmentName ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Environment: {occurrence.actualEnvironmentName ?? occurrence.plannedEnvironmentName}
        </p>
      ) : (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>No environment selected</p>
      )}
      {showContinue ? (
        <Button onClick={() => navigate(occurrencePath)}>Continue Workout</Button>
      ) : null}
      {!showContinue && showStart ? (
        <Button onClick={() => navigate(occurrencePath)}>Start Workout</Button>
      ) : null}
      {!showContinue && !showStart ? (
        <Button variant="secondary" onClick={() => navigate(occurrencePath)}>
          View Workout
        </Button>
      ) : null}
    </HomeCard>
  );
}
