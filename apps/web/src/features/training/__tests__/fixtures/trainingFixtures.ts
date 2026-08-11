export const planFixture = {
  id: '11111111-1111-4111-8111-111111111111',
  type: 'STRENGTH',
  name: 'Off-season strength',
  status: 'DRAFT',
  startDate: '2026-01-01',
  endDate: '2026-03-31',
  scheduleStatus: 'DRAFT',
};

export const dayFixture = {
  id: '22222222-2222-4222-8222-222222222222',
  displayOrder: 1,
  title: 'Lower body A',
  planWeekNumber: 1,
  scheduledDayOfWeek: 'MONDAY',
  status: 'DRAFT',
};

export const exerciseFixture = {
  id: '33333333-3333-4333-8333-333333333333',
  exerciseDefinitionId: '44444444-4444-4444-8444-444444444444',
  displayOrder: 1,
  exerciseName: 'Back squat',
  category: 'STRENGTH',
  type: 'BARBELL',
  sets: 4,
  minimumReps: 4,
  maximumReps: 6,
  targetWeight: 100,
  weightUnit: 'KILOGRAM',
};

export const occurrenceFixture = {
  id: '55555555-5555-4555-8555-555555555555',
  workoutDayId: dayFixture.id,
  scheduledDate: '2026-02-02',
  status: 'SCHEDULED',
  executions: [
    {
      id: '66666666-6666-4666-8666-666666666666',
      exerciseName: 'Back squat snapshot',
      prescribedSets: 4,
      prescribedMinimumReps: 4,
      prescribedMaximumReps: 6,
      prescribedTargetWeight: 100,
      prescribedWeightUnit: 'KILOGRAM',
      status: 'NOT_STARTED',
    },
  ],
};
