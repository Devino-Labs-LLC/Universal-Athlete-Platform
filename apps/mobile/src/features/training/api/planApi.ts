import { ApiClient } from '@/src/core/api/apiClient';
import {
  TrainingPlan,
  trainingPlanSchema,
  WorkoutDay,
  workoutDaysSchema,
  WorkoutExercise,
  workoutExercisesSchema,
} from '@/src/features/training/models/browseSchemas';

export async function fetchTrainingPlan(
  client: ApiClient,
  planId: string,
): Promise<TrainingPlan> {
  const response = await client.axios.get(`/api/v1/training/plans/${planId}`);
  return trainingPlanSchema.parse(response.data);
}

export async function fetchWorkoutDays(
  client: ApiClient,
  planId: string,
): Promise<WorkoutDay[]> {
  const response = await client.axios.get(`/api/v1/training/plans/${planId}/days`);
  return workoutDaysSchema.parse(response.data);
}

export async function fetchDayExercises(
  client: ApiClient,
  planId: string,
  dayId: string,
): Promise<WorkoutExercise[]> {
  const response = await client.axios.get(
    `/api/v1/training/plans/${planId}/days/${dayId}/exercises`,
  );
  return workoutExercisesSchema.parse(response.data);
}
