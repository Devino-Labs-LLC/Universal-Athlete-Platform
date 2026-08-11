import { TrainingEnvironmentListFilters } from '@/src/features/environments/models/environmentSchemas';

export const environmentKeys = {
  all: ['training', 'environments'] as const,
  lists: () => ['training', 'environments', 'list'] as const,
  list: (filters?: TrainingEnvironmentListFilters) =>
    [
      'training',
      'environments',
      'list',
      filters?.type ?? null,
      filters?.equipment?.join(',') ?? null,
      filters?.activeOnly ?? true,
      filters?.page ?? 0,
      filters?.size ?? 50,
    ] as const,
  detail: (id: string) => ['training', 'environments', 'detail', id] as const,
};
