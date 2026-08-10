import { formatDecimal, formatDistance, formatDurationSeconds } from '@/src/features/home/utils/formatMetrics';
import { WorkoutExercise } from '@/src/features/training/models/browseSchemas';

function formatReps(minimumReps?: number | null, maximumReps?: number | null): string | null {
  if (minimumReps == null && maximumReps == null) {
    return null;
  }
  if (minimumReps != null && maximumReps != null && minimumReps !== maximumReps) {
    return `${minimumReps}–${maximumReps} reps`;
  }
  const reps = maximumReps ?? minimumReps;
  return reps != null ? `${reps} reps` : null;
}

function formatWeight(targetWeight?: number | null, weightUnit?: string | null): string | null {
  if (targetWeight == null) {
    return null;
  }
  const unit = weightUnit === 'POUND' ? 'lb' : 'kg';
  return `${formatDecimal(targetWeight, targetWeight >= 100 ? 0 : 1)} ${unit}`;
}

export function formatExercisePrescription(exercise: WorkoutExercise): string {
  const parts: string[] = [];

  if (exercise.sets != null) {
    parts.push(`${exercise.sets} sets`);
  }

  const reps = formatReps(exercise.minimumReps, exercise.maximumReps);
  if (reps) {
    parts.push(reps);
  }

  const weight = formatWeight(exercise.targetWeight ?? undefined, exercise.weightUnit);
  if (weight) {
    parts.push(`@ ${weight}`);
  }

  if (exercise.targetDurationSeconds != null) {
    parts.push(formatDurationSeconds(exercise.targetDurationSeconds));
  }

  if (exercise.targetDistance != null) {
    parts.push(formatDistance(exercise.targetDistance));
  }

  if (exercise.targetRpe != null) {
    parts.push(`RPE ${exercise.targetRpe}`);
  }

  if (exercise.targetRestSeconds != null) {
    parts.push(`${exercise.targetRestSeconds}s rest`);
  }

  return parts.length > 0 ? parts.join(' · ') : 'Prescription details unavailable';
}

export function formatLaunchExercisePrescription(exercise: {
  prescribedSets?: number | null;
  prescribedMinimumReps?: number | null;
  prescribedMaximumReps?: number | null;
  prescribedTargetWeight?: number | null;
  prescribedWeightUnit?: string | null;
  prescribedTargetDurationSeconds?: number | null;
  prescribedTargetDistance?: number | null;
  prescribedTargetRpe?: number | null;
  prescribedTargetRestSeconds?: number | null;
}): string {
  return formatExercisePrescription({
    id: '',
    displayOrder: 0,
    exerciseName: '',
    sets: exercise.prescribedSets,
    minimumReps: exercise.prescribedMinimumReps,
    maximumReps: exercise.prescribedMaximumReps,
    targetWeight: exercise.prescribedTargetWeight,
    weightUnit: exercise.prescribedWeightUnit,
    targetDurationSeconds: exercise.prescribedTargetDurationSeconds,
    targetDistance: exercise.prescribedTargetDistance,
    targetRpe: exercise.prescribedTargetRpe,
    targetRestSeconds: exercise.prescribedTargetRestSeconds,
  });
}
