import {
  inferExerciseFieldProfile,
  resolveSetFieldKinds,
  weightUnitLabel,
} from '@/src/features/training/execution/utils/setMetrics';
import {
  formatSetActual,
  formatSetPrescription,
} from '@/src/features/training/execution/utils/setFormat';
import { WorkoutExerciseSet } from '@/src/features/training/execution/models/executionSchemas';

const strengthSet: WorkoutExerciseSet = {
  id: 'set-1',
  workoutExerciseExecutionId: 'exec-1',
  setNumber: 1,
  status: 'NOT_STARTED',
  prescribedMinimumReps: 8,
  prescribedMaximumReps: 10,
  prescribedWeight: 100,
  prescribedWeightUnit: 'LB',
  prescribedTargetRpe: 8,
  prescribedRestSeconds: 90,
};

const durationSet: WorkoutExerciseSet = {
  id: 'set-2',
  workoutExerciseExecutionId: 'exec-2',
  setNumber: 1,
  status: 'NOT_STARTED',
  prescribedDurationSeconds: 600,
  prescribedTargetRpe: 7,
};

const distanceSet: WorkoutExerciseSet = {
  id: 'set-3',
  workoutExerciseExecutionId: 'exec-3',
  setNumber: 1,
  status: 'NOT_STARTED',
  prescribedDistance: 1609,
  prescribedDistanceUnit: 'METER',
  prescribedDurationSeconds: 720,
};

describe('setMetrics', () => {
  it('selects strength fields', () => {
    const fields = resolveSetFieldKinds(strengthSet);
    expect(fields).toContain('reps');
    expect(fields).toContain('weight');
    expect(fields).toContain('rpe');
    expect(fields).toContain('rest');
    expect(fields).toContain('notes');
    expect(inferExerciseFieldProfile(strengthSet)).toBe('strength');
  });

  it('selects duration fields', () => {
    const fields = resolveSetFieldKinds(durationSet);
    expect(fields).toContain('duration');
    expect(fields).toContain('rpe');
    expect(fields).not.toContain('reps');
    expect(inferExerciseFieldProfile(durationSet)).toBe('duration');
  });

  it('selects distance fields', () => {
    const fields = resolveSetFieldKinds(distanceSet);
    expect(fields).toContain('distance');
    expect(fields).toContain('duration');
    expect(inferExerciseFieldProfile(distanceSet)).toBe('distance');
  });

  it('formats weight unit labels', () => {
    expect(weightUnitLabel('LB')).toBe('lb');
    expect(weightUnitLabel('KILOGRAM')).toBe('kg');
  });
});

describe('setFormat', () => {
  it('formats strength prescription', () => {
    expect(formatSetPrescription(strengthSet)).toContain('8–10 reps');
    expect(formatSetPrescription(strengthSet)).toContain('100 lb');
  });

  it('formats duration prescription', () => {
    expect(formatSetPrescription(durationSet)).toContain('10 min');
    expect(formatSetPrescription(durationSet)).toContain('RPE 7');
  });

  it('formats actual values', () => {
    const actual = formatSetActual({
      ...strengthSet,
      actualReps: 10,
      actualWeight: 105,
      actualWeightUnit: 'LB',
    });
    expect(actual).toContain('10 reps');
    expect(actual).toContain('105 lb');
  });
});
