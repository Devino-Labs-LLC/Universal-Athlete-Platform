import type { ApiClient } from '@/core/api/apiClient';
import { fetchExerciseDefinitions as fetchCatalogExerciseDefinitions } from '@/features/exercises/api/exerciseDefinitionsApi';
import type { ExerciseDefinitionFilters } from '@/features/training/models/queryKeys';
import type { ExerciseDefinitionPage } from '@/features/training/models/schemas';

/**
 * Thin delegate onto the exercises feature's catalog API — the exercises
 * feature owns the full exercise-definition schema/filters, so the training
 * planner chooser stays in sync instead of maintaining a second, incomplete
 * schema. The planner chooser only ever offers active, non-archived
 * definitions.
 */
export async function fetchExerciseDefinitions(
  client: ApiClient,
  filters?: ExerciseDefinitionFilters,
): Promise<ExerciseDefinitionPage> {
  const page = await fetchCatalogExerciseDefinitions(client, {
    name: filters?.name,
    scope: filters?.scope,
    category: filters?.category,
    metricMode: filters?.metricMode,
    page: filters?.page ?? 0,
    size: filters?.size ?? 20,
  });
  return {
    ...page,
    definitions: page.definitions.filter((def) => def.active && !def.archivedAt),
  };
}
