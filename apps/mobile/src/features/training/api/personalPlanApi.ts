import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import { addDays } from '@/src/features/training/utils/calendarRange';
import {
  TrainingPlan,
  trainingPlanSchema,
  WorkoutDay,
  workoutDaySchema,
} from '@/src/features/training/models/browseSchemas';
import { z } from 'zod';

const PREFERRED_STARTER_EXERCISE_ID = '11111111-1111-1111-1111-111111111101';

const catalogDefinitionSchema = z
  .object({
    id: z.string(),
    canonicalName: z.string(),
    metadata: z
      .object({
        category: z.string().optional(),
        type: z.string().optional(),
      })
      .passthrough()
      .optional(),
  })
  .passthrough();

const catalogPageSchema = z
  .object({
    definitions: z.array(catalogDefinitionSchema),
  })
  .passthrough();

const generationResultSchema = z
  .object({
    createdCount: z.number(),
    existingCount: z.number().optional(),
  })
  .passthrough();

const activationResponseSchema = z
  .object({
    plan: trainingPlanSchema,
    generation: generationResultSchema.nullable().optional(),
  })
  .passthrough();

export interface CreatePersonalPlanResult {
  plan: TrainingPlan;
  createdOccurrenceCount: number;
}

const WEEKDAYS = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
] as const;

function weekdayFor(date: DateOnly): (typeof WEEKDAYS)[number] {
  const [year, month, day] = date.split('-').map(Number);
  return WEEKDAYS[new Date(year, month - 1, day).getDay()];
}

async function fetchStarterExercise(client: ApiClient): Promise<{
  id: string;
  name: string;
  category: string;
  type: string;
}> {
  const response = await client.axios.get('/api/v1/training/exercise-definitions', {
    params: { page: 0, size: 20, scope: 'SYSTEM' },
  });
  const page = catalogPageSchema.parse(response.data);
  const preferred =
    page.definitions.find((item) => item.id === PREFERRED_STARTER_EXERCISE_ID) ??
    page.definitions[0];
  if (!preferred) {
    throw new Error('No system exercise definitions are available to start a personal plan.');
  }
  return {
    id: preferred.id,
    name: preferred.canonicalName,
    category: preferred.metadata?.category ?? 'STRENGTH',
    type: preferred.metadata?.type ?? 'BARBELL',
  };
}

export async function createAndActivatePersonalPlan(
  client: ApiClient,
  input: { name: string; startDate: DateOnly; timezone: string },
): Promise<CreatePersonalPlanResult> {
  const starter = await fetchStarterExercise(client);
  const endDate = addDays(input.startDate, 6);

  const planResponse = await client.axios.post('/api/v1/training/plans', {
    type: 'GENERAL',
    name: input.name.trim(),
    startDate: input.startDate,
    endDate,
  });
  const plan = trainingPlanSchema.parse(planResponse.data);

  const dayResponse = await client.axios.post(`/api/v1/training/plans/${plan.id}/days`, {
    title: 'Day 1',
    planWeekNumber: 1,
    scheduledDayOfWeek: weekdayFor(input.startDate),
  });
  const day: WorkoutDay = workoutDaySchema.parse(dayResponse.data);

  await client.axios.post(`/api/v1/training/plans/${plan.id}/days/${day.id}/exercises`, {
    exerciseDefinitionId: starter.id,
    exerciseName: starter.name,
    category: starter.category,
    type: starter.type,
    sets: 3,
    minimumReps: 5,
    maximumReps: 8,
  });

  const activation = await client.axios.post(
    `/api/v1/training/plans/${plan.id}/schedule/activate`,
    {
      scheduleStartDate: input.startDate,
      scheduleEndDate: endDate,
      timezone: input.timezone,
      recurrenceMode: 'FINITE',
      generateThrough: input.startDate,
    },
  );
  const activated = activationResponseSchema.parse(activation.data);

  return {
    plan: activated.plan,
    createdOccurrenceCount: activated.generation?.createdCount ?? 0,
  };
}
