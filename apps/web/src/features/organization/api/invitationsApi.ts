import type { ApiClient } from '@/core/api/apiClient';
import {
  acceptInvitationResponseSchema,
  type AcceptInvitationResponse,
  type CreateInvitationRequest,
  invitationSchema,
  type Invitation,
  myInvitationListSchema,
  type MyInvitation,
} from '@/features/organization/models/schemas';

const ME_INVITATIONS = '/api/v1/me/invitations';
const TOKEN_INVITATIONS = '/api/v1/invitations';

export async function fetchMyInvitations(client: ApiClient): Promise<MyInvitation[]> {
  const response = await client.axios.get(ME_INVITATIONS);
  return myInvitationListSchema.parse(response.data);
}

export async function acceptInvitationById(
  client: ApiClient,
  invitationId: string,
): Promise<AcceptInvitationResponse> {
  const response = await client.axios.post(`${ME_INVITATIONS}/${invitationId}/accept`);
  return acceptInvitationResponseSchema.parse(response.data);
}

export async function declineInvitationById(client: ApiClient, invitationId: string): Promise<void> {
  await client.axios.post(`${ME_INVITATIONS}/${invitationId}/decline`);
}

export async function acceptInvitationByToken(
  client: ApiClient,
  rawToken: string,
): Promise<AcceptInvitationResponse> {
  const response = await client.axios.post(
    `${TOKEN_INVITATIONS}/${encodeURIComponent(rawToken)}/accept`,
  );
  return acceptInvitationResponseSchema.parse(response.data);
}

export async function declineInvitationByToken(client: ApiClient, rawToken: string): Promise<void> {
  await client.axios.post(`${TOKEN_INVITATIONS}/${encodeURIComponent(rawToken)}/decline`);
}

export async function createOrganizationInvitation(
  client: ApiClient,
  organizationId: string,
  request: CreateInvitationRequest,
): Promise<Invitation> {
  const response = await client.axios.post(
    `/api/v1/organizations/${organizationId}/invitations`,
    request,
  );
  return invitationSchema.parse(response.data);
}

export async function createTeamInvitation(
  client: ApiClient,
  teamId: string,
  request: CreateInvitationRequest,
): Promise<Invitation> {
  const response = await client.axios.post(`/api/v1/teams/${teamId}/invitations`, request);
  return invitationSchema.parse(response.data);
}
