import { formatDecimal, formatDistance, formatDurationSeconds } from '@/src/features/home/utils/formatMetrics';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';
import { distanceUnitLabel, weightUnitLabel } from '@/src/features/training/execution/utils/setMetrics';

function formatRepsRange(min?: number | null, max?: number | null): string | null {
  if (min == null && max == null) {
    return null;
  }
  if (min != null && max != null && min !== max) {
    return `${min}–${max}`;
  }
  return String(max ?? min);
}

function formatWeightValue(value?: number | null, unit?: string | null): string | null {
  if (value == null) {
    return null;
  }
  const label = weightUnitLabel(unit);
  return `${formatDecimal(value, value >= 100 ? 0 : 1)} ${label}`;
}

function formatDistanceValue(value?: number | null, unit?: string | null): string | null {
  if (value == null) {
    return null;
  }
  const label = distanceUnitLabel(unit);
  if (label === 'm' || label === 'km' || label === 'mi') {
    return `${formatDecimal(value, 1)} ${label}`;
  }
  return formatDistance(value);
}

export function formatSetPrescription(set: WorkoutExerciseSet): string {
  const parts: string[] = [];

  const reps = formatRepsRange(set.prescribedMinimumReps, set.prescribedMaximumReps);
  if (reps) {
    parts.push(`${reps} reps`);
  }

  const weight = formatWeightValue(set.prescribedWeight ?? undefined, set.prescribedWeightUnit);
  if (weight) {
    parts.push(`@ ${weight}`);
  }

  if (set.prescribedDurationSeconds != null) {
    parts.push(formatDurationSeconds(set.prescribedDurationSeconds));
  }

  const distance = formatDistanceValue(set.prescribedDistance ?? undefined, set.prescribedDistanceUnit);
  if (distance) {
    parts.push(distance);
  }

  if (set.prescribedTargetRpe != null) {
    parts.push(`RPE ${set.prescribedTargetRpe}`);
  }

  if (set.prescribedRestSeconds != null) {
    parts.push(`${set.prescribedRestSeconds}s rest`);
  }

  return parts.length > 0 ? parts.join(' · ') : '—';
}

export function formatSetActual(set: WorkoutExerciseSet): string {
  const parts: string[] = [];

  if (set.actualReps != null) {
    parts.push(`${set.actualReps} reps`);
  }

  const weight = formatWeightValue(set.actualWeight ?? undefined, set.actualWeightUnit);
  if (weight) {
    parts.push(weight);
  }

  if (set.actualDurationSeconds != null) {
    parts.push(formatDurationSeconds(set.actualDurationSeconds));
  }

  const distance = formatDistanceValue(set.actualDistance ?? undefined, set.actualDistanceUnit);
  if (distance) {
    parts.push(distance);
  }

  if (set.actualRpe != null) {
    parts.push(`RPE ${set.actualRpe}`);
  }

  if (set.actualRestSeconds != null) {
    parts.push(`${set.actualRestSeconds}s rest`);
  }

  return parts.length > 0 ? parts.join(' · ') : '—';
}

export function formatExecutionPrescription(execution: {
  prescribedSets?: number | null;
  prescribedMinimumReps?: number | null;
  prescribedMaximumReps?: number | null;
  prescribedTargetWeight?: number | null;
  prescribedWeightUnit?: string | null;
  prescribedTargetDurationSeconds?: number | null;
  prescribedTargetDistance?: number | null;
  prescribedDistanceUnit?: string | null;
  prescribedTargetRestSeconds?: number | null;
  prescribedTargetRpe?: number | null;
}): string {
  const parts: string[] = [];

  if (execution.prescribedSets != null) {
    parts.push(`${execution.prescribedSets} sets`);
  }

  const reps = formatRepsRange(execution.prescribedMinimumReps, execution.prescribedMaximumReps);
  if (reps) {
    parts.push(`${reps} reps`);
  }

  const weight = formatWeightValue(
    execution.prescribedTargetWeight ?? undefined,
    execution.prescribedWeightUnit,
  );
  if (weight) {
    parts.push(`@ ${weight}`);
  }

  if (execution.prescribedTargetDurationSeconds != null) {
    parts.push(formatDurationSeconds(execution.prescribedTargetDurationSeconds));
  }

  const distance = formatDistanceValue(
    execution.prescribedTargetDistance ?? undefined,
    execution.prescribedDistanceUnit,
  );
  if (distance) {
    parts.push(distance);
  }

  if (execution.prescribedTargetRpe != null) {
    parts.push(`RPE ${execution.prescribedTargetRpe}`);
  }

  if (execution.prescribedTargetRestSeconds != null) {
    parts.push(`${execution.prescribedTargetRestSeconds}s rest`);
  }

  return parts.length > 0 ? parts.join(' · ') : 'Prescription details unavailable';
}
