import type { ApiClient } from '@/core/api/apiClient';
import {
  type CreateWorkoutDayRequest,
  type UpdateWorkoutDayRequest,
  type WorkoutDay,
  workoutDaySchema,
  workoutDaysSchema,
} from '@/features/training/models/schemas';

function basePath(planId: string): string {
  return `/api/v1/training/plans/${planId}/days`;
}

export async function fetchWorkoutDays(client: ApiClient, planId: string): Promise<WorkoutDay[]> {
  const response = await client.axios.get(basePath(planId));
  return workoutDaysSchema.parse(response.data);
}

export async function fetchWorkoutDay(
  client: ApiClient,
  planId: string,
  dayId: string,
): Promise<WorkoutDay> {
  const response = await client.axios.get(`${basePath(planId)}/${dayId}`);
  return workoutDaySchema.parse(response.data);
}

export async function createWorkoutDay(
  client: ApiClient,
  planId: string,
  request: CreateWorkoutDayRequest,
): Promise<WorkoutDay> {
  const response = await client.axios.post(basePath(planId), request);
  return workoutDaySchema.parse(response.data);
}

export async function updateWorkoutDay(
  client: ApiClient,
  planId: string,
  dayId: string,
  request: UpdateWorkoutDayRequest,
): Promise<WorkoutDay> {
  const response = await client.axios.patch(`${basePath(planId)}/${dayId}`, request);
  return workoutDaySchema.parse(response.data);
}

export async function reorderWorkoutDays(
  client: ApiClient,
  planId: string,
  dayIds: string[],
): Promise<WorkoutDay[]> {
  const response = await client.axios.put(`${basePath(planId)}/order`, { dayIds });
  return workoutDaysSchema.parse(response.data);
}

export async function deleteWorkoutDay(
  client: ApiClient,
  planId: string,
  dayId: string,
): Promise<void> {
  await client.axios.delete(`${basePath(planId)}/${dayId}`);
}

export async function changeDayStatus(
  client: ApiClient,
  planId: string,
  dayId: string,
  action: 'ACTIVATE' | 'COMPLETE' | 'SKIP',
): Promise<WorkoutDay> {
  const response = await client.axios.patch(`${basePath(planId)}/${dayId}/status`, { action });
  return workoutDaySchema.parse(response.data);
}
