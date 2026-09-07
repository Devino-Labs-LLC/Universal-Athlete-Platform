import { QueryClient, useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  acceptInvitationById,
  acceptInvitationByToken,
  declineInvitationById,
  declineInvitationByToken,
} from '@/src/features/organization/api/invitationsApi';
import { invitationKeys } from '@/src/features/organization/models/queryKeys';

async function invalidateMyInvitations(queryClient: QueryClient) {
  await queryClient.invalidateQueries({ queryKey: invitationKeys.mine() });
}

export function useInvitationMutations() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  const acceptByIdMutation = useMutation({
    mutationFn: (invitationId: string) => acceptInvitationById(apiClient, invitationId),
    onSuccess: async () => {
      await invalidateMyInvitations(queryClient);
    },
  });

  const declineByIdMutation = useMutation({
    mutationFn: (invitationId: string) => declineInvitationById(apiClient, invitationId),
    onSuccess: async () => {
      await invalidateMyInvitations(queryClient);
    },
  });

  const acceptByTokenMutation = useMutation({
    mutationFn: (rawToken: string) => acceptInvitationByToken(apiClient, rawToken),
    onSuccess: async () => {
      await invalidateMyInvitations(queryClient);
    },
  });

  const declineByTokenMutation = useMutation({
    mutationFn: (rawToken: string) => declineInvitationByToken(apiClient, rawToken),
    onSuccess: async () => {
      await invalidateMyInvitations(queryClient);
    },
  });

  return {
    acceptByIdMutation,
    declineByIdMutation,
    acceptByTokenMutation,
    declineByTokenMutation,
  };
}
