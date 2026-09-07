import type { QueryClient } from '@tanstack/react-query';

import { consentKeys } from '@/features/consent/models/queryKeys';

export function invalidateConsentQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: consentKeys.grants() });
}

/** After grant or revoke — refresh the athlete consent list. */
export function invalidateAfterConsentMutation(queryClient: QueryClient): void {
  invalidateConsentQueries(queryClient);
}
