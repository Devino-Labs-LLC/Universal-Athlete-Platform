import { ApiClient } from '@/src/core/api/apiClient';
import {
  WorkoutLaunchContext,
  workoutLaunchContextSchema,
} from '@/src/features/training/models/browseSchemas';

export async function fetchWorkoutLaunchContext(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutLaunchContext> {
  const response = await client.axios.get(
    `/api/v1/training/client/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/launch-context`,
  );
  return workoutLaunchContextSchema.parse(response.data);
}
