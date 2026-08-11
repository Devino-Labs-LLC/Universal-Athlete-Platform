import type { ApiClient } from '@/core/api/apiClient';
import { type TrainingEnvironment, trainingEnvironmentsSchema } from '@/features/training/models/schemas';

const BASE = '/api/v1/training/environments';

export async function fetchTrainingEnvironments(
  client: ApiClient,
  activeOnly = true,
): Promise<TrainingEnvironment[]> {
  const response = await client.axios.get(BASE, {
    params: { activeOnly },
  });
  const environments = trainingEnvironmentsSchema.parse(response.data);
  return activeOnly ? environments.filter((env) => env.active) : environments;
}
