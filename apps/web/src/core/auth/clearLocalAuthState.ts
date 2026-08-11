import type { QueryClient } from '@tanstack/react-query';

import { clearCsrfToken } from '@/core/api/csrf';
import type { MeResponse } from '@/features/auth/schemas';

export type AuthSessionStatus =
  | 'INITIALIZING'
  | 'AUTHENTICATED'
  | 'UNAUTHENTICATED'
  | 'REFRESHING'
  | 'EXPIRED';

/** Best-effort local session teardown used by logout and session-expiry paths. */
export async function clearLocalAuthState(options: {
  queryClient: QueryClient;
  setAccount: (account: MeResponse | null) => void;
  setStatus: (status: AuthSessionStatus) => void;
  status?: AuthSessionStatus;
}): Promise<void> {
  const nextStatus = options.status ?? 'UNAUTHENTICATED';
  clearCsrfToken();
  options.queryClient.clear();
  options.setAccount(null);
  options.setStatus(nextStatus);
}
