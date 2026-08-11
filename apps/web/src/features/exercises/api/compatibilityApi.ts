import type { ApiClient } from '@/core/api/apiClient';
import { compatibilityResultSchema, type CompatibilityResult } from '@/features/exercises/models/schemas';

export async function fetchCompatibility(
  client: ApiClient,
  exerciseDefinitionId: string,
  environmentId: string,
): Promise<CompatibilityResult> {
  const response = await client.axios.get(
    `/api/v1/training/exercise-definitions/${exerciseDefinitionId}/environment-compatibility/${environmentId}`,
  );
  return compatibilityResultSchema.parse(response.data);
}
