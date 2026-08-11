import { workoutOccurrenceDetailSchema } from '@/src/features/training/models/browseSchemas';

describe('occurrence environment mutation response contract', () => {
  it('parses WorkoutOccurrenceResponse shape (not TrainingEnvironment)', () => {
    const occurrenceResponse = {
      id: 'occ-1',
      workoutDayId: 'day-1',
      scheduledDate: '2026-08-10',
      status: 'SCHEDULED',
      environment: {
        plannedEnvironment: {
          trainingEnvironmentId: 'env-planned',
          name: 'Commercial Gym',
        },
        actualEnvironment: {
          trainingEnvironmentId: 'env-1',
          name: 'Home Gym',
        },
      },
      executions: [],
    };

    const parsed = workoutOccurrenceDetailSchema.parse(occurrenceResponse);
    expect(parsed.id).toBe('occ-1');
    expect(parsed.environment?.actualEnvironment?.trainingEnvironmentId).toBe('env-1');
    expect(parsed).not.toHaveProperty('availableEquipment');
  });
});
