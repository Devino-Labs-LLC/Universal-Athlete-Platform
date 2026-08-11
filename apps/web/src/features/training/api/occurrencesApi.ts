import type { ApiClient } from '@/core/api/apiClient';
import {
  type CreateOccurrenceRequest,
  type RescheduleOccurrenceRequest,
  type WorkoutOccurrenceDetail,
  workoutOccurrenceDetailSchema,
  workoutOccurrencesSchema,
} from '@/features/training/models/schemas';

function basePath(planId: string, dayId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences`;
}

export async function fetchOccurrences(
  client: ApiClient,
  planId: string,
  dayId: string,
): Promise<WorkoutOccurrenceDetail[]> {
  const response = await client.axios.get(basePath(planId, dayId));
  return workoutOccurrencesSchema.parse(response.data);
}

export async function fetchOccurrenceDetail(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.get(`${basePath(planId, dayId)}/${occurrenceId}`);
  return workoutOccurrenceDetailSchema.parse(response.data);
}

export async function createOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  request: CreateOccurrenceRequest,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.post(basePath(planId, dayId), request);
  return workoutOccurrenceDetailSchema.parse(response.data);
}

export async function rescheduleOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  request: RescheduleOccurrenceRequest,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.post(
    `${basePath(planId, dayId)}/${occurrenceId}/reschedule`,
    request,
  );
  return workoutOccurrenceDetailSchema.parse(response.data);
}

export async function deleteOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<void> {
  await client.axios.delete(`${basePath(planId, dayId)}/${occurrenceId}`);
}

export function isOccurrenceReschedulable(occurrence: WorkoutOccurrenceDetail): boolean {
  return occurrence.status === 'SCHEDULED' && !occurrence.startedAt;
}

export function isOccurrenceDeletable(occurrence: WorkoutOccurrenceDetail): boolean {
  return occurrence.status === 'SCHEDULED' && !occurrence.startedAt;
}
