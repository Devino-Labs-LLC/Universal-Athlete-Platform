import { connectedAppsQueryKeys } from './queryKeys';

/** Shared React Query key + flags for the connections list (no React import). */
export function connectionsListQueryBase(enabled: boolean) {
  return {
    queryKey: connectedAppsQueryKeys.connections(),
    enabled,
    retry: false as const,
  };
}

/** Target for invalidateQueries after connection mutations. */
export function connectionsInvalidationTarget() {
  return { queryKey: connectedAppsQueryKeys.connections() };
}
