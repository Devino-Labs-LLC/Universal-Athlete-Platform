import type { ApiClient } from '@/core/api/apiClient';
import {
  type WorkoutOccurrencePerformance,
  workoutOccurrencePerformanceSchema,
} from '@/features/performance/models/schemas';

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
