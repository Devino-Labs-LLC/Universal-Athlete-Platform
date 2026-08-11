import type { ApiClient } from '@/core/api/apiClient';
import type { ExerciseDefinitionListFilters } from '@/features/exercises/models/schemas';
import {
  type CreateExerciseDefinitionRequest,
  type ExerciseDefinition,
  exerciseDefinitionPageSchema,
  type ExerciseDefinitionPage,
  exerciseDefinitionSchema,
  type UpdateExerciseDefinitionRequest,
} from '@/features/exercises/models/schemas';

const BASE = '/api/v1/training/exercise-definitions';

export async function fetchExerciseDefinitions(
  client: ApiClient,
  filters?: ExerciseDefinitionListFilters,
): Promise<ExerciseDefinitionPage> {
  const response = await client.axios.get(BASE, {
    params: {
      name: filters?.name || undefined,
      scope: filters?.scope,
      category: filters?.category,
      metricMode: filters?.metricMode,
      movementPattern: filters?.movementPattern,
      muscleGroup: filters?.muscleGroup,
      equipment: filters?.equipment,
      laterality: filters?.laterality,
      impactLevel: filters?.impactLevel,
      difficulty: filters?.difficulty,
      page: filters?.page ?? 0,
      size: filters?.size ?? 20,
    },
  });
  return exerciseDefinitionPageSchema.parse(response.data);
}

export async function fetchExerciseDefinition(
  client: ApiClient,
  definitionId: string,
): Promise<ExerciseDefinition> {
  const response = await client.axios.get(`${BASE}/${definitionId}`);
  return exerciseDefinitionSchema.parse(response.data);
}

export async function createExerciseDefinition(
  client: ApiClient,
  request: CreateExerciseDefinitionRequest,
): Promise<ExerciseDefinition> {
  const response = await client.axios.post(BASE, request);
  return exerciseDefinitionSchema.parse(response.data);
}

export async function updateExerciseDefinition(
  client: ApiClient,
  definitionId: string,
  patch: UpdateExerciseDefinitionRequest,
): Promise<ExerciseDefinition> {
  const response = await client.axios.patch(`${BASE}/${definitionId}`, patch);
  return exerciseDefinitionSchema.parse(response.data);
}

export async function archiveExerciseDefinition(
  client: ApiClient,
  definitionId: string,
): Promise<void> {
  await client.axios.delete(`${BASE}/${definitionId}`);
}
