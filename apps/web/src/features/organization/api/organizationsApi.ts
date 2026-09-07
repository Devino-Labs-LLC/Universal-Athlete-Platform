import type { ApiClient } from '@/core/api/apiClient';
import {
  myAthleteTeamListSchema,
  type MyAthleteTeam,
  organizationListSchema,
  type Organization,
  teamListSchema,
  type Team,
} from '@/features/organization/models/schemas';

export async function fetchMyOrganizations(client: ApiClient): Promise<Organization[]> {
  const response = await client.axios.get('/api/v1/organizations');
  return organizationListSchema.parse(response.data);
}

export async function fetchOrganizationTeams(
  client: ApiClient,
  organizationId: string,
): Promise<Team[]> {
  const response = await client.axios.get(`/api/v1/organizations/${organizationId}/teams`);
  return teamListSchema.parse(response.data);
}

/** ACTIVE athlete team memberships for consent / sharing team picker. */
export async function fetchMyAthleteTeams(client: ApiClient): Promise<MyAthleteTeam[]> {
  const response = await client.axios.get('/api/v1/athletes/me/teams');
  return myAthleteTeamListSchema.parse(response.data);
}
