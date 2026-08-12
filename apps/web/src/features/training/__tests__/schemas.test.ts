import {
  calendarEntrySchema,
  createTrainingPlanSchema,
  createWorkoutDaySchema,
  createWorkoutExerciseSchema,
  exerciseDefinitionSchema,
  generateOccurrencesSchema,
  trainingPlanSchema,
  workoutDaySchema,
  workoutExerciseSchema,
  workoutOccurrenceDetailSchema,
} from '@/features/training/models/schemas';
import {
  dayFixture,
  exerciseFixture,
  occurrenceFixture,
  planFixture,
} from '@/features/training/__tests__/fixtures/trainingFixtures';

describe('training schemas', () => {
  it('parses training plan fixture', () => {
    expect(trainingPlanSchema.parse(planFixture).name).toBe('Off-season strength');
  });

  it('parses workout day fixture', () => {
    expect(workoutDaySchema.parse(dayFixture).title).toBe('Lower body A');
  });

  it('parses workout exercise fixture', () => {
    expect(workoutExerciseSchema.parse(exerciseFixture).exerciseName).toBe('Back squat');
  });

  it('parses calendar entry fixture', () => {
    expect(
      calendarEntrySchema.parse({
        occurrenceId: occurrenceFixture.id,
        trainingPlanId: planFixture.id,
        trainingPlanName: planFixture.name,
        workoutDayId: dayFixture.id,
        workoutDayName: dayFixture.title,
        scheduledDate: '2026-02-02',
        status: 'SCHEDULED',
        exerciseCount: 1,
        completedExerciseCount: 0,
      }).workoutDayName,
    ).toBe('Lower body A');
  });

  it('parses occurrence detail fixture', () => {
    expect(workoutOccurrenceDetailSchema.parse(occurrenceFixture).status).toBe('SCHEDULED');
  });

  it('validates create plan request', () => {
    expect(
      createTrainingPlanSchema.safeParse({
        type: 'STRENGTH',
        name: 'Plan',
        startDate: '2026-01-01',
        endDate: '2026-03-31',
      }).success,
    ).toBe(true);
  });

  it('requires custom type for OTHER plans', () => {
    expect(
      createTrainingPlanSchema.safeParse({
        type: 'OTHER',
        name: 'Plan',
        startDate: '2026-01-01',
        endDate: '2026-03-31',
      }).success,
    ).toBe(false);
  });

  it('validates create day request', () => {
    expect(
      createWorkoutDaySchema.safeParse({
        title: 'Day',
        planWeekNumber: 1,
        scheduledDayOfWeek: 'MONDAY',
      }).success,
    ).toBe(true);
  });

  it('validates create exercise request', () => {
    expect(
      createWorkoutExerciseSchema.safeParse({
        exerciseDefinitionId: exerciseFixture.exerciseDefinitionId,
        category: 'STRENGTH',
        type: 'BARBELL',
        sets: 3,
      }).success,
    ).toBe(true);
  });

  it('accepts SYSTEM seed exercise definition IDs rejected by strict RFC uuid()', () => {
    const benchPressSystemId = '11111111-1111-1111-1111-111111111103';
    expect(createWorkoutExerciseSchema.safeParse({
      exerciseDefinitionId: benchPressSystemId,
      category: 'STRENGTH',
      type: 'BARBELL',
      sets: 4,
      minimumReps: 8,
      targetWeight: 45,
      weightUnit: 'POUND',
      targetRpe: 10,
    }).success).toBe(true);
  });

  it('validates generate range max 90 days', () => {
    expect(
      generateOccurrencesSchema.safeParse({
        scheduledFrom: '2026-01-01',
        scheduledTo: '2026-05-01',
      }).success,
    ).toBe(false);
  });

  it('parses exercise definition fixture', () => {
    expect(
      exerciseDefinitionSchema.parse({
        id: exerciseFixture.exerciseDefinitionId,
        exercisePerformanceKey: exerciseFixture.exerciseDefinitionId,
        scope: 'SYSTEM',
        canonicalName: 'Back squat',
        normalizedName: 'back squat',
        metadata: {
          category: 'STRENGTH',
          metricMode: 'WEIGHT_AND_REPETITIONS',
          primaryMovementPattern: 'SQUAT',
          secondaryMovementPatterns: [],
          primaryMuscleGroups: [],
          secondaryMuscleGroups: [],
          requiredEquipment: [],
          optionalEquipment: [],
          laterality: 'BILATERAL',
          kineticChainType: 'CLOSED_CHAIN',
          impactLevel: 'LOW_IMPACT',
          difficulty: 'INTERMEDIATE',
        },
        active: true,
      }).canonicalName,
    ).toBe('Back squat');
  });
});
