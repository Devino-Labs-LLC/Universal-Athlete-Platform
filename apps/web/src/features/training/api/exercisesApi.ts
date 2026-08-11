import type { ApiClient } from '@/core/api/apiClient';
import {
  type CreateWorkoutExerciseRequest,
  type UpdateWorkoutExerciseRequest,
  type WorkoutExercise,
  workoutExerciseSchema,
  workoutExercisesSchema,
} from '@/features/training/models/schemas';

function basePath(planId: string, dayId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/exercises`;
}

export async function fetchDayExercises(
  client: ApiClient,
  planId: string,
  dayId: string,
): Promise<WorkoutExercise[]> {
  const response = await client.axios.get(basePath(planId, dayId));
  return workoutExercisesSchema.parse(response.data);
}

export async function createWorkoutExercise(
  client: ApiClient,
  planId: string,
  dayId: string,
  request: CreateWorkoutExerciseRequest,
): Promise<WorkoutExercise> {
  const response = await client.axios.post(basePath(planId, dayId), request);
  return workoutExerciseSchema.parse(response.data);
}

export async function updateWorkoutExercise(
  client: ApiClient,
  planId: string,
  dayId: string,
  exerciseId: string,
  request: UpdateWorkoutExerciseRequest,
): Promise<WorkoutExercise> {
  const response = await client.axios.patch(`${basePath(planId, dayId)}/${exerciseId}`, request);
  return workoutExerciseSchema.parse(response.data);
}

export async function reorderWorkoutExercises(
  client: ApiClient,
  planId: string,
  dayId: string,
  exerciseIds: string[],
): Promise<WorkoutExercise[]> {
  const response = await client.axios.put(`${basePath(planId, dayId)}/order`, { exerciseIds });
  return workoutExercisesSchema.parse(response.data);
}

export async function deleteWorkoutExercise(
  client: ApiClient,
  planId: string,
  dayId: string,
  exerciseId: string,
): Promise<void> {
  await client.axios.delete(`${basePath(planId, dayId)}/${exerciseId}`);
}
