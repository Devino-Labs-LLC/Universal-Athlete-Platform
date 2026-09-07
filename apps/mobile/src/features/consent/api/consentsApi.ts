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

export async function listMyAthleteTeamMemberships(
  client: ApiClient,
): Promise<MyAthleteTeamMembership[]> {
  const response = await client.axios.get(ME_ATHLETE_TEAMS_PATH);
  return myAthleteTeamMembershipsSchema.parse(response.data);
}
