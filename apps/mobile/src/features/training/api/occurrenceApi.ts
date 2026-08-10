import { ApiClient } from '@/src/core/api/apiClient';
import {
  WorkoutOccurrenceDetail,
  workoutOccurrenceDetailSchema,
} from '@/src/features/training/models/browseSchemas';

export async function fetchOccurrenceDetail(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrenceDetail> {
  const response = await client.axios.get(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}`,
  );
  return workoutOccurrenceDetailSchema.parse(response.data);
}
