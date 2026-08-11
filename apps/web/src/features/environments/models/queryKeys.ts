import type { EnvironmentListFilters } from '@/features/environments/models/schemas';

export const environmentKeys = {
  all: ['environments'] as const,
  lists: () => [...environmentKeys.all, 'list'] as const,
  list: (filters?: EnvironmentListFilters) =>
    [
      ...environmentKeys.lists(),
      filters?.type ?? null,
      filters?.equipment?.slice().sort().join(',') ?? null,
      filters?.activeOnly ?? true,
      filters?.page ?? 0,
      filters?.size ?? 20,
    ] as const,
  details: () => [...environmentKeys.all, 'detail'] as const,
  detail: (environmentId: string) => [...environmentKeys.details(), environmentId] as const,
};

export const TRAINING_ENVIRONMENTS_KEY = ['training', 'environments'] as const;
