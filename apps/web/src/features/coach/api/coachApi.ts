import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  coachAthleteOverviewSchema,
  teamRosterSchema,
  trainingAssignmentListSchema,
  trainingAssignmentSchema,
  type CoachAthleteOverview,
  type TeamRosterEntry,
  type TrainingAssignment,
} from '@/features/coach/models/schemas';
import {
  fetchMyOrganizations,
  fetchOrganizationTeams,
} from '@/features/organization/api/organizationsApi';
import type { Organization, Team } from '@/features/organization/models/schemas';

export async function fetchCoachOrganizations(client: ApiClient): Promise<Organization[]> {
  return fetchMyOrganizations(client);
}

export async function fetchCoachOrganizationTeams(
  client: ApiClient,
  organizationId: string,
): Promise<Team[]> {
  return fetchOrganizationTeams(client, organizationId);
}

export async function fetchTeamRoster(
  client: ApiClient,
  teamId: string,
): Promise<TeamRosterEntry[]> {
  const response = await client.axios.get(`/api/v1/teams/${teamId}/roster`);
  return teamRosterSchema.parse(response.data);
}

export async function fetchCoachAthleteOverview(
  client: ApiClient,
  teamId: string,
  athleteId: string,
  date?: DateOnly,
): Promise<CoachAthleteOverview> {
  const response = await client.axios.get(
    `/api/v1/teams/${teamId}/athletes/${athleteId}/overview`,
    {
      params: date ? { date } : undefined,
    },
  );
  return coachAthleteOverviewSchema.parse(response.data);
}

export async function fetchCoachAssignments(
  client: ApiClient,
  teamId: string,
  athleteId: string,
) {
  const response = await client.axios.get(
    `/api/v1/teams/${teamId}/athletes/${athleteId}/training/assignments`,
  );
  return trainingAssignmentListSchema.parse(response.data);
}

export async function createCoachAssignment(
  client: ApiClient,
  teamId: string,
  athleteId: string,
  input: { title: string; description: string; scheduledDate: string; idempotencyKey: string },
): Promise<TrainingAssignment> {
  const response = await client.axios.post(
    `/api/v1/teams/${teamId}/athletes/${athleteId}/training/assignments`,
    input,
  );
  return trainingAssignmentSchema.parse(response.data);
}
