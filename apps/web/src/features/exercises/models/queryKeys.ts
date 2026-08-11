import type {
  ExerciseDefinitionListFilters,
  SubstitutionCandidateFilters,
} from '@/features/exercises/models/schemas';

export const exerciseKeys = {
  all: ['exercises'] as const,
  lists: () => [...exerciseKeys.all, 'list'] as const,
  list: (filters?: ExerciseDefinitionListFilters) =>
    [
      ...exerciseKeys.lists(),
      filters?.name ?? null,
      filters?.scope ?? null,
      filters?.category ?? null,
      filters?.metricMode ?? null,
      filters?.movementPattern ?? null,
      filters?.muscleGroup ?? null,
      filters?.equipment ?? null,
      filters?.laterality ?? null,
      filters?.impactLevel ?? null,
      filters?.difficulty ?? null,
      filters?.page ?? 0,
      filters?.size ?? 20,
    ] as const,
  details: () => [...exerciseKeys.all, 'detail'] as const,
  detail: (definitionId: string) => [...exerciseKeys.details(), definitionId] as const,
  candidatesFor: (sourceId: string) => [...exerciseKeys.all, 'candidates', sourceId] as const,
  candidates: (sourceId: string, filters?: SubstitutionCandidateFilters) =>
    [
      ...exerciseKeys.candidatesFor(sourceId),
      filters?.equipment?.slice().sort().join(',') ?? null,
      filters?.trainingEnvironmentId ?? null,
    ] as const,
  relationships: () => [...exerciseKeys.all, 'relationship'] as const,
  relationship: (relationshipId: string) => [...exerciseKeys.relationships(), relationshipId] as const,
  compatibilityFor: (exerciseDefinitionId: string) =>
    [...exerciseKeys.all, 'compatibility', exerciseDefinitionId] as const,
  compatibility: (exerciseDefinitionId: string, environmentId: string) =>
    [...exerciseKeys.compatibilityFor(exerciseDefinitionId), environmentId] as const,
};

export const TRAINING_EXERCISE_DEFINITIONS_PREFIX = ['training', 'exerciseDefinitions'] as const;
