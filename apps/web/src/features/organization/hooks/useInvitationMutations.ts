import { useMutation, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  acceptInvitationById,
  acceptInvitationByToken,
  createOrganizationInvitation,
  createTeamInvitation,
  declineInvitationById,
  declineInvitationByToken,
} from '@/features/organization/api/invitationsApi';
import { invalidateAfterInvitationMutation } from '@/features/organization/models/invalidation';
import type { CreateInvitationRequest } from '@/features/organization/models/schemas';

export function useAcceptInvitationByIdMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => acceptInvitationById(apiClient, invitationId),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}

export function useDeclineInvitationByIdMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (invitationId: string) => declineInvitationById(apiClient, invitationId),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}

export function useAcceptInvitationByTokenMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (rawToken: string) => acceptInvitationByToken(apiClient, rawToken),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}

export function useDeclineInvitationByTokenMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (rawToken: string) => declineInvitationByToken(apiClient, rawToken),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}

export function useCreateOrganizationInvitationMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { organizationId: string; request: CreateInvitationRequest }) =>
      createOrganizationInvitation(apiClient, input.organizationId, input.request),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}

export function useCreateTeamInvitationMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { teamId: string; request: CreateInvitationRequest }) =>
      createTeamInvitation(apiClient, input.teamId, input.request),
    onSuccess: () => {
      invalidateAfterInvitationMutation(queryClient);
    },
  });
}
