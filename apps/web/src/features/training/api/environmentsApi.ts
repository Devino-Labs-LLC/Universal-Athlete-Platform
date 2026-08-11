import type { ApiClient } from '@/core/api/apiClient';
import { fetchEnvironments } from '@/features/environments/api/environmentsApi';
import { type TrainingEnvironment, trainingEnvironmentsSchema } from '@/features/training/models/schemas';

/**
 * Delegates to the environments feature's list endpoint, which returns a
 * page envelope (`{ environments, page, size, totalElements }`) rather than
 * a bare array. Previously this parsed `response.data` directly as an
 * array, which broke against the real API contract.
 */
export async function fetchTrainingEnvironments(
  client: ApiClient,
  activeOnly = true,
): Promise<TrainingEnvironment[]> {
  const page = await fetchEnvironments(client, { activeOnly });
  return trainingEnvironmentsSchema.parse(page.environments);
}
