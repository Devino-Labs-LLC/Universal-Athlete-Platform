import { describe, expect, it } from 'vitest';

import {
  exerciseFixture,
  occurrenceFixture,
} from '@/features/training/__tests__/fixtures/trainingFixtures';
import type { WorkoutExercise } from '@/features/training/models/schemas';
import { workoutExerciseSchema } from '@/features/training/models/schemas';
import {
  formatExercisePrescription,
} from '@/features/training/utils/prescriptionFormat';

const parsedExercise = workoutExerciseSchema.parse(exerciseFixture) as WorkoutExercise;

describe('prescriptionFormat', () => {
  it('formats sets, reps, and weight', () => {
    expect(formatExercisePrescription(parsedExercise)).toContain('4 sets');
    expect(formatExercisePrescription(parsedExercise)).toContain('4–6 reps');
    expect(formatExercisePrescription(parsedExercise)).toContain('100');
  });

  it('formats duration-only prescription', () => {
    expect(
      formatExercisePrescription({
        ...parsedExercise,
        minimumReps: null,
        maximumReps: null,
        targetWeight: null,
        targetDurationSeconds: 600,
      }),
    ).toContain('10 min');
  });

  it('formats execution snapshot separately from live prescription', () => {
    const snapshot = occurrenceFixture.executions![0]!;
    const updatedName = 'Back squat updated';
    expect(snapshot.exerciseName).toBe('Back squat snapshot');
    expect(updatedName).toBe('Back squat updated');
    expect(snapshot.exerciseName).not.toContain('updated');
    expect(formatExercisePrescription(parsedExercise)).toContain('4 sets');
  });

  it('returns fallback when no fields present', () => {
    expect(
      formatExercisePrescription({
        id: 'x',
        displayOrder: 0,
        exerciseName: 'Mystery',
      }),
    ).toBe('Prescription details unavailable');
  });
});
