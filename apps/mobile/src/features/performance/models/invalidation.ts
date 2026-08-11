import { QueryClient } from '@tanstack/react-query';

import { performanceKeys } from '@/src/features/performance/models/performanceKeys';

export async function invalidatePerformanceQueries(queryClient: QueryClient): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: performanceKeys.all }),
  ]);
}
