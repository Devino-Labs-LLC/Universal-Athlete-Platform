import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchAccountBillingStatus } from '@/src/features/billing/api/accountBillingApi';
import {
  isAccountBillingMissing,
  isAccountBillingUnavailable,
} from '@/src/features/billing/models/errors';
import { accountBillingQueryKeys } from '@/src/features/billing/models/queryKeys';
import { restorePremiumPurchase } from '@/src/features/billing/store/restorePremiumPurchase';
import type { StoreRestorePayload } from '@/src/features/billing/store/storeRestoreBridge';

export function useAccountBillingStatus() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: accountBillingQueryKeys.status(),
    enabled: status === 'AUTHENTICATED',
    retry: false,
    queryFn: async () => {
      try {
        return await fetchAccountBillingStatus(apiClient);
      } catch (cause) {
        if (isAccountBillingMissing(cause)) {
          return null;
        }
        throw cause;
      }
    },
  });
}

export function useRestorePremiumMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload?: StoreRestorePayload | null) =>
      restorePremiumPurchase(apiClient, payload),
    onSuccess: (status) => {
      queryClient.setQueryData(accountBillingQueryKeys.status(), status);
    },
  });
}

export { isAccountBillingUnavailable };
