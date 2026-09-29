import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  createAccountCheckoutSession,
  fetchAccountBillingStatus,
  syncAccountSubscription,
} from '@/features/billing/api/accountBillingApi';
import {
  isAccountBillingMissing,
  isAccountBillingUnavailable,
} from '@/features/billing/models/errors';
import { accountBillingQueryKeys } from '@/features/billing/queryKeys';

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

export function useCreateAccountCheckoutMutation() {
  const { apiClient } = useAuthSession();

  return useMutation({
    mutationFn: (input: { requestId: string; cadence: 'MONTHLY' | 'ANNUAL' }) =>
      createAccountCheckoutSession(apiClient, {
        requestId: input.requestId,
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: input.cadence,
      }),
  });
}

export function useSyncAccountSubscriptionMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { subscriptionId: string; checkoutSessionId: string }) =>
      syncAccountSubscription(apiClient, input.subscriptionId, input.checkoutSessionId),
    onSuccess: (status) => {
      queryClient.setQueryData(accountBillingQueryKeys.status(), status);
    },
  });
}

export { isAccountBillingUnavailable };
