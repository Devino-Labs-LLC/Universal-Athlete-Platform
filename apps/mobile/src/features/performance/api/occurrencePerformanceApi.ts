import { ApiClient } from '@/src/core/api/apiClient';
import {
  WorkoutOccurrencePerformance,
  workoutOccurrencePerformanceSchema,
} from '@/src/features/performance/models/performanceSchemas';

function performancePath(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/performance`;
}

export async function fetchOccurrencePerformance(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<WorkoutOccurrencePerformance> {
  const response = await client.axios.get(performancePath(planId, dayId, occurrenceId));
  return workoutOccurrencePerformanceSchema.parse(response.data);
}
