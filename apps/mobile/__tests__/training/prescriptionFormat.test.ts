import { formatExercisePrescription } from '@/src/features/training/utils/prescriptionFormat';

describe('prescriptionFormat', () => {
  it('formats sets, reps, and weight', () => {
    const formatted = formatExercisePrescription({
      id: 'ex-1',
      displayOrder: 1,
      exerciseName: 'Back Squat',
      sets: 4,
      minimumReps: 6,
      maximumReps: 8,
      targetWeight: 100,
      weightUnit: 'KILOGRAM',
    });

    expect(formatted).toContain('4 sets');
    expect(formatted).toContain('6–8 reps');
    expect(formatted).toContain('100 kg');
  });

  it('formats duration and distance prescriptions', () => {
    const formatted = formatExercisePrescription({
      id: 'ex-2',
      displayOrder: 2,
      exerciseName: 'Row',
      targetDurationSeconds: 1200,
      targetDistance: 5000,
      distanceUnit: 'METER',
      targetRpe: 8,
    });

    expect(formatted).toContain('20 min');
    expect(formatted).toContain('5.0 km');
    expect(formatted).toContain('RPE 8');
  });
});
