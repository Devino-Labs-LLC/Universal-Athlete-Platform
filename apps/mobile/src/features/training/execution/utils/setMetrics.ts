import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';

export type SetFieldKind = 'reps' | 'weight' | 'duration' | 'distance' | 'rpe' | 'rest' | 'notes';

export function resolveSetFieldKinds(set: WorkoutExerciseSet): SetFieldKind[] {
  const fields: SetFieldKind[] = [];

  const hasReps =
    set.prescribedMinimumReps != null ||
    set.prescribedMaximumReps != null ||
    set.actualReps != null;
  const hasWeight = set.prescribedWeight != null || set.actualWeight != null;
  const hasDuration = set.prescribedDurationSeconds != null || set.actualDurationSeconds != null;
  const hasDistance = set.prescribedDistance != null || set.actualDistance != null;
  const hasRpe = set.prescribedTargetRpe != null || set.actualRpe != null;
  const hasRest = set.prescribedRestSeconds != null || set.actualRestSeconds != null;

  if (hasReps) {
    fields.push('reps');
  }
  if (hasWeight) {
    fields.push('weight');
  }
  if (hasDuration) {
    fields.push('duration');
  }
  if (hasDistance) {
    fields.push('distance');
  }
  if (hasRpe) {
    fields.push('rpe');
  }
  if (hasRest) {
    fields.push('rest');
  }

  fields.push('notes');
  return fields;
}

export function inferExerciseFieldProfile(set: WorkoutExerciseSet): 'strength' | 'duration' | 'distance' {
  if (set.prescribedDistance != null || set.actualDistance != null) {
    return 'distance';
  }
  if (set.prescribedDurationSeconds != null || set.actualDurationSeconds != null) {
    return 'duration';
  }
  return 'strength';
}

export function isSetMutable(status: string): boolean {
  return status === 'NOT_STARTED' || status === 'IN_PROGRESS';
}

export function isSetTerminal(status: string): boolean {
  return status === 'COMPLETED' || status === 'SKIPPED';
}

export function isExecutionTerminal(status: string): boolean {
  return status === 'COMPLETED' || status === 'SKIPPED';
}

export function allSetsTerminal(sets: WorkoutExerciseSet[]): boolean {
  return sets.length > 0 && sets.every((set) => isSetTerminal(set.status));
}

export function weightUnitLabel(unit: string | null | undefined): string {
  if (unit === 'LB' || unit === 'POUND') {
    return 'lb';
  }
  return 'kg';
}

export function distanceUnitLabel(unit: string | null | undefined): string {
  if (unit === 'MILE' || unit === 'MI') {
    return 'mi';
  }
  if (unit === 'KILOMETER' || unit === 'KM') {
    return 'km';
  }
  return 'm';
}
