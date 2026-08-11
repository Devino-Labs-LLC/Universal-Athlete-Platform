import type { ApiClient } from '@/core/api/apiClient';
import type { ExerciseDefinitionFilters } from '@/features/training/models/queryKeys';
import {
  type ExerciseDefinitionPage,
  exerciseDefinitionPageSchema,
} from '@/features/training/models/schemas';

const BASE = '/api/v1/training/exercise-definitions';

export async function fetchExerciseDefinitions(
  client: ApiClient,
  filters?: ExerciseDefinitionFilters,
): Promise<ExerciseDefinitionPage> {
  const response = await client.axios.get(BASE, {
    params: {
      name: filters?.name,
      scope: filters?.scope,
      category: filters?.category,
      metricMode: filters?.metricMode,
      page: filters?.page ?? 0,
      size: filters?.size ?? 20,
    },
  });
  const parsed = exerciseDefinitionPageSchema.parse(response.data);
  return {
    ...parsed,
    definitions: parsed.definitions.filter((def) => def.active && !def.archivedAt),
  };
}
