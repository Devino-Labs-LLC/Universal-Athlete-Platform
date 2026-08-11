import type { ExerciseExecution, WorkoutExercise } from '@/features/training/models/schemas';

function formatDecimal(value: number, decimals = 1): string {
  return value.toFixed(decimals);
}

function formatDurationSeconds(seconds: number): string {
  const totalMinutes = Math.round(seconds / 60);
  if (totalMinutes < 60) {
    return `${totalMinutes} min`;
  }
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return minutes > 0 ? `${hours}h ${minutes}m` : `${hours}h`;
}

function formatDistance(meters: number, unit?: string | null): string {
  if (unit === 'MILE') {
    return `${formatDecimal(meters, 2)} mi`;
  }
  if (unit === 'KILOMETER' || meters >= 1000) {
    return `${formatDecimal(meters / 1000, 2)} km`;
  }
  return `${Math.round(meters)} m`;
}

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
    parts.push(formatDistance(exercise.targetDistance, exercise.distanceUnit));
  }

  if (exercise.targetRpe != null) {
    parts.push(`RPE ${exercise.targetRpe}`);
  }

  if (exercise.targetRestSeconds != null) {
    parts.push(`${exercise.targetRestSeconds}s rest`);
  }

  return parts.length > 0 ? parts.join(' · ') : 'Prescription details unavailable';
}

export function formatExecutionPrescription(execution: ExerciseExecution): string {
  return formatExercisePrescription({
    id: execution.id,
    displayOrder: 0,
    exerciseName: execution.exerciseName,
    sets: execution.prescribedSets,
    minimumReps: execution.prescribedMinimumReps,
    maximumReps: execution.prescribedMaximumReps,
    targetWeight: execution.prescribedTargetWeight,
    weightUnit: execution.prescribedWeightUnit as WorkoutExercise['weightUnit'],
  });
}
