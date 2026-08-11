import { ApiClient } from '@/src/core/api/apiClient';
import { SetOccurrenceEnvironmentRequest } from '@/src/features/environments/models/environmentSchemas';
import {
  WorkoutOccurrenceDetail,
  workoutOccurrenceDetailSchema,
} from '@/src/features/training/models/browseSchemas';

function occurrenceEnvironmentPath(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/environment`;
}

/** Backend returns WorkoutOccurrenceResponse (not TrainingEnvironmentResponse). */
export async function setOccurrenceEnvironment(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  request: SetOccurrenceEnvironmentRequest,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.put(
    occurrenceEnvironmentPath(planId, dayId, occurrenceId),
    request,
  );
  return workoutOccurrenceDetailSchema.parse(response.data);
}

/** Backend returns WorkoutOccurrenceResponse; body unused after invalidate/refetch. */
export async function clearOccurrenceEnvironment(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.delete(
    occurrenceEnvironmentPath(planId, dayId, occurrenceId),
  );
  return workoutOccurrenceDetailSchema.parse(response.data);
}
