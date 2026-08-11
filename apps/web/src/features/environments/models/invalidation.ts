import type { QueryClient } from '@tanstack/react-query';

import { environmentKeys, TRAINING_ENVIRONMENTS_KEY } from '@/features/environments/models/queryKeys';

/**
 * W3's planner environment picker (`trainingKeys.environments()`) reads from
 * the same backend list, so every environment mutation must invalidate both
 * this feature's cache and the training feature's cache to stay in sync.
 */
export function invalidateEnvironmentQueries(
  queryClient: QueryClient,
  environmentId?: string,
): void {
  void queryClient.invalidateQueries({ queryKey: environmentKeys.lists() });
  void queryClient.invalidateQueries({ queryKey: TRAINING_ENVIRONMENTS_KEY });
  if (environmentId) {
    void queryClient.invalidateQueries({ queryKey: environmentKeys.detail(environmentId) });
  }
}
