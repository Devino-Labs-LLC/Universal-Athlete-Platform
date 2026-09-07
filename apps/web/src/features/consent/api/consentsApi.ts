import type { ApiClient } from '@/core/api/apiClient';
import {
  consentGrantListSchema,
  consentGrantSchema,
  type ConsentGrant,
  type CreateConsentGrantRequest,
} from '@/features/consent/models/schemas';

const ME_CONSENTS = '/api/v1/athletes/me/consents';

export async function fetchMyConsentGrants(client: ApiClient): Promise<ConsentGrant[]> {
  const response = await client.axios.get(ME_CONSENTS);
  return consentGrantListSchema.parse(response.data);
}

export async function createConsentGrant(
  client: ApiClient,
  request: CreateConsentGrantRequest,
): Promise<ConsentGrant> {
  const response = await client.axios.post(ME_CONSENTS, request);
  return consentGrantSchema.parse(response.data);
}

export async function revokeConsentGrant(client: ApiClient, consentId: string): Promise<void> {
  await client.axios.post(`${ME_CONSENTS}/${encodeURIComponent(consentId)}/revoke`);
}
