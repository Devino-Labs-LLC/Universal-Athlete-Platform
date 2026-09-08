import { z } from 'zod';

import { ApiClient } from '@/src/core/api/apiClient';
import {
  ConsentGrant,
  CreateConsentGrantRequest,
  MyAthleteTeamMembership,
  consentGrantSchema,
  consentGrantsSchema,
  myAthleteTeamMembershipsSchema,
} from '@/src/features/consent/models/consentSchemas';

const ME_CONSENTS_PATH = '/api/v1/athletes/me/consents';
const ME_ATHLETE_TEAMS_PATH = '/api/v1/athletes/me/teams';

export async function listMyConsents(client: ApiClient): Promise<ConsentGrant[]> {
  const response = await client.axios.get(ME_CONSENTS_PATH);
  return consentGrantsSchema.parse(response.data);
}

export async function createConsentGrant(
  client: ApiClient,
  body: CreateConsentGrantRequest,
): Promise<ConsentGrant> {
  const response = await client.axios.post(ME_CONSENTS_PATH, body);
  return consentGrantSchema.parse(response.data);
}

export async function revokeConsentGrant(
  client: ApiClient,
  consentId: string,
): Promise<void> {
  await client.axios.post(`${ME_CONSENTS_PATH}/${consentId}/revoke`);
}

const transparencyPageSchema = z.object({
  events: z.array(
    z.object({
      type: z.string(),
      occurredAt: z.string(),
      organizationName: z.string().nullable().optional(),
      teamName: z.string().nullable().optional(),
      description: z.string(),
    }),
  ),
  page: z.number(),
  size: z.number(),
  hasMore: z.boolean(),
});

export type TransparencyPage = z.infer<typeof transparencyPageSchema>;

export async function fetchAthleteTransparency(
  client: ApiClient,
  page = 0,
): Promise<TransparencyPage> {
  const response = await client.axios.get('/api/v1/athletes/me/transparency', {
    params: { page, size: 20 },
  });
  return transparencyPageSchema.parse(response.data);
}

export async function listMyAthleteTeamMemberships(
  client: ApiClient,
): Promise<MyAthleteTeamMembership[]> {
  const response = await client.axios.get(ME_ATHLETE_TEAMS_PATH);
  return myAthleteTeamMembershipsSchema.parse(response.data);
}
