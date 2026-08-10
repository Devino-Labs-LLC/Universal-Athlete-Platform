import { ApiClient } from '@/src/core/api/apiClient';
import {
  TrainingLoadSummary,
  trainingLoadSummarySchema,
} from '@/src/features/training/execution/models/executionSchemas';

function loadBase(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/training-load`;
}

export async function fetchTrainingLoad(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<TrainingLoadSummary | null> {
  try {
    const response = await client.axios.get(loadBase(planId, dayId, occurrenceId));
    return trainingLoadSummarySchema.parse(response.data);
  } catch (error: unknown) {
    if (
      typeof error === 'object' &&
      error !== null &&
      'response' in error &&
      (error as { response?: { status?: number } }).response?.status === 404
    ) {
      return null;
    }
    throw error;
  }
}
