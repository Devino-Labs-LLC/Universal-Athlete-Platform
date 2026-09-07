import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { createConsentGrant, revokeConsentGrant } from '@/features/consent/api/consentsApi';
import { invalidateAfterConsentMutation } from '@/features/consent/models/invalidation';
import type { CreateConsentGrantRequest } from '@/features/consent/models/schemas';

export function useCreateConsentGrantMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateConsentGrantRequest) => createConsentGrant(apiClient, request),
    onSuccess: () => {
      invalidateAfterConsentMutation(queryClient);
    },
  });
}

export function useRevokeConsentGrantMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (consentId: string) => revokeConsentGrant(apiClient, consentId),
    onSuccess: () => {
      invalidateAfterConsentMutation(queryClient);
    },
  });
}
