import { QueryClient, useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  createConsentGrant,
  revokeConsentGrant,
} from '@/src/features/consent/api/consentsApi';
import { ConsentGrant, CreateConsentGrantRequest } from '@/src/features/consent/models/consentSchemas';
import { consentKeys } from '@/src/features/consent/models/queryKeys';

async function invalidateConsentQueries(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: consentKeys.mine() }),
    queryClient.invalidateQueries({ queryKey: consentKeys.teamMemberships() }),
  ]);
}

export function useConsentMutations() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (body: CreateConsentGrantRequest) => createConsentGrant(apiClient, body),
    onSuccess: async () => {
      await invalidateConsentQueries(queryClient);
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (consentId: string) => revokeConsentGrant(apiClient, consentId),
    onMutate: async (consentId) => {
      await queryClient.cancelQueries({ queryKey: consentKeys.mine() });
      const previous = queryClient.getQueryData<ConsentGrant[]>(consentKeys.mine());
      if (previous) {
        queryClient.setQueryData<ConsentGrant[]>(
          consentKeys.mine(),
          previous.filter((grant) => grant.id !== consentId),
        );
      }
      return { previous };
    },
    onError: (_error, _consentId, context) => {
      if (context?.previous) {
        queryClient.setQueryData(consentKeys.mine(), context.previous);
      }
    },
    onSettled: async () => {
      await invalidateConsentQueries(queryClient);
    },
  });

  return {
    createMutation,
    revokeMutation,
  };
}
