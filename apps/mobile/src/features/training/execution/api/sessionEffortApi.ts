import { ApiClient } from '@/src/core/api/apiClient';
import {
  SessionEffort,
  SessionEffortRequest,
  sessionEffortSchema,
} from '@/src/features/training/execution/models/executionSchemas';

function effortBase(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/session-effort`;
}

export async function fetchSessionEffort(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<SessionEffort | null> {
  try {
    const response = await client.axios.get(effortBase(planId, dayId, occurrenceId));
    return sessionEffortSchema.parse(response.data);
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

export async function submitSessionEffort(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  request: SessionEffortRequest,
): Promise<SessionEffort> {
  const response = await client.axios.post(effortBase(planId, dayId, occurrenceId), request);
  return sessionEffortSchema.parse(response.data);
}

export async function updateSessionEffort(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  request: SessionEffortRequest,
): Promise<SessionEffort> {
  const response = await client.axios.patch(effortBase(planId, dayId, occurrenceId), request);
  return sessionEffortSchema.parse(response.data);
}
