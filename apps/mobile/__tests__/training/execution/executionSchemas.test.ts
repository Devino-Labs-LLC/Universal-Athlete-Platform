import {
  sessionEffortSchema,
  trainingLoadSummarySchema,
  workoutExerciseSetSchema,
} from '@/src/features/training/execution/models/executionSchemas';

describe('executionSchemas', () => {
  it('parses workout exercise set fixture', () => {
    const parsed = workoutExerciseSetSchema.parse({
      id: 'set-1',
      workoutExerciseExecutionId: 'exec-1',
      setNumber: 1,
      displayOrder: 1,
      setType: 'WORKING',
      prescribedMinimumReps: 8,
      prescribedMaximumReps: 10,
      prescribedWeight: '225',
      prescribedWeightUnit: 'LB',
      prescribedTargetRpe: 8,
      prescribedRestSeconds: 90,
      actualReps: 10,
      actualWeight: 225,
      actualWeightUnit: 'LB',
      actualRpe: 8,
      status: 'COMPLETED',
      startedAt: '2026-08-10T10:00:00Z',
      completedAt: '2026-08-10T10:02:00Z',
    });

    expect(parsed.prescribedWeight).toBe(225);
    expect(parsed.actualReps).toBe(10);
    expect(parsed.status).toBe('COMPLETED');
  });

  it('parses session effort fixture', () => {
    const parsed = sessionEffortSchema.parse({
      sessionRpe: 7.5,
      sessionDurationMinutes: 55,
      perceivedNotes: 'Felt strong',
      createdAt: '2026-08-10T11:00:00Z',
    });

    expect(parsed.sessionRpe).toBe(7.5);
    expect(parsed.sessionDurationMinutes).toBe(55);
  });

  it('parses training load summary fixture', () => {
    const parsed = trainingLoadSummarySchema.parse({
      sessionRpe: '7.5',
      sessionDurationMinutes: 55,
      sessionRpeLoad: '412.5',
      prescribedExerciseCount: 6,
      completedExerciseCount: 6,
      substitutedExerciseCount: 1,
      completedSetCount: 18,
      skippedSetCount: 0,
      completedRepetitionCount: 144,
      totalVolumeKilograms: '6123.5',
      totalDurationSeconds: 3300,
      totalDistanceMeters: '0',
      calculatedAt: '2026-08-10T11:05:00Z',
    });

    expect(parsed.totalVolumeKilograms).toBe(6123.5);
    expect(parsed.completedSetCount).toBe(18);
  });
});
