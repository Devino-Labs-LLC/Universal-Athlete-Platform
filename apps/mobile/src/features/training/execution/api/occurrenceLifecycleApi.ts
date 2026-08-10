import { ApiClient } from '@/src/core/api/apiClient';
import {
  WorkoutOccurrenceDetail,
  workoutOccurrenceDetailSchema,
} from '@/src/features/training/models/browseSchemas';

function occurrenceBase(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}`;
}

export async function startWorkoutOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.post(`${occurrenceBase(planId, dayId, occurrenceId)}/start`);
  return workoutOccurrenceDetailSchema.parse(response.data);
}

export async function completeWorkoutOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.post(
    `${occurrenceBase(planId, dayId, occurrenceId)}/complete`,
  );
  return workoutOccurrenceDetailSchema.parse(response.data);
}

export async function skipWorkoutOccurrence(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.post(`${occurrenceBase(planId, dayId, occurrenceId)}/skip`);
  return workoutOccurrenceDetailSchema.parse(response.data);
}
