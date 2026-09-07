import { ApiClient } from '@/src/core/api/apiClient';
import {
  AcceptInvitationResponse,
  MyInvitation,
  acceptInvitationResponseSchema,
  myInvitationsSchema,
} from '@/src/features/organization/models/invitationSchemas';

const ME_INVITATIONS_PATH = '/api/v1/me/invitations';
const TOKEN_INVITATIONS_PATH = '/api/v1/invitations';

export async function listMyInvitations(client: ApiClient): Promise<MyInvitation[]> {
  const response = await client.axios.get(ME_INVITATIONS_PATH);
  return myInvitationsSchema.parse(response.data);
}

export async function acceptInvitationById(
  client: ApiClient,
  invitationId: string,
): Promise<AcceptInvitationResponse> {
  const response = await client.axios.post(`${ME_INVITATIONS_PATH}/${invitationId}/accept`);
  return acceptInvitationResponseSchema.parse(response.data);
}

export async function declineInvitationById(
  client: ApiClient,
  invitationId: string,
): Promise<void> {
  await client.axios.post(`${ME_INVITATIONS_PATH}/${invitationId}/decline`);
}

export async function acceptInvitationByToken(
  client: ApiClient,
  rawToken: string,
): Promise<AcceptInvitationResponse> {
  const encoded = encodeURIComponent(rawToken);
  const response = await client.axios.post(`${TOKEN_INVITATIONS_PATH}/${encoded}/accept`);
  return acceptInvitationResponseSchema.parse(response.data);
}

export async function declineInvitationByToken(
  client: ApiClient,
  rawToken: string,
): Promise<void> {
  const encoded = encodeURIComponent(rawToken);
  await client.axios.post(`${TOKEN_INVITATIONS_PATH}/${encoded}/decline`);
}
