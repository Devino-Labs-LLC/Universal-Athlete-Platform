import type { ApiClient } from '@/core/api/apiClient';
import { isApiError } from '@/core/api/errors';
import {
  type AddAthleteSportRequest,
  athleteGoalSchema,
  athleteGoalsListSchema,
  athleteProfileSchema,
  athleteSportSchema,
  athleteSportsListSchema,
  type AthleteGoal,
  type AthleteProfile,
  type AthleteSport,
  type CreateAthleteGoalRequest,
  type CreateAthleteProfileRequest,
  type GoalStatusAction,
  type UpdateAthleteProfileRequest,
} from '@/features/profile/schemas';

const PROFILE_PATH = '/api/v1/athletes/me';
const SPORTS_PATH = '/api/v1/athletes/me/sports';
const GOALS_PATH = '/api/v1/athletes/me/goals';

export function isAthleteProfileNotFound(error: unknown): boolean {
  return isApiError(error) && error.code === 'ATHLETE_PROFILE_NOT_FOUND';
}

export async function fetchAthleteProfile(client: ApiClient): Promise<AthleteProfile | null> {
  try {
    const response = await client.axios.get(PROFILE_PATH);
    return athleteProfileSchema.parse(response.data);
  } catch (error) {
    if (isAthleteProfileNotFound(error)) {
      return null;
    }
    throw error;
  }
}

export async function createAthleteProfile(
  client: ApiClient,
  request: CreateAthleteProfileRequest,
): Promise<AthleteProfile> {
  const response = await client.axios.post(PROFILE_PATH, request);
  return athleteProfileSchema.parse(response.data);
}

export async function updateAthleteProfile(
  client: ApiClient,
  request: UpdateAthleteProfileRequest,
): Promise<AthleteProfile> {
  const response = await client.axios.patch(PROFILE_PATH, request);
  return athleteProfileSchema.parse(response.data);
}

export async function fetchAthleteSports(client: ApiClient): Promise<AthleteSport[]> {
  try {
    const response = await client.axios.get(SPORTS_PATH);
    return athleteSportsListSchema.parse(response.data);
  } catch (error) {
    if (isAthleteProfileNotFound(error)) {
      return [];
    }
    throw error;
  }
}

export async function addAthleteSport(
  client: ApiClient,
  request: AddAthleteSportRequest,
): Promise<AthleteSport> {
  const payload = {
    ...request,
    customSportName:
      request.sportType === 'OTHER' ? request.customSportName?.trim() : undefined,
    preferredPosition: request.preferredPosition?.trim() || undefined,
  };
  const response = await client.axios.post(SPORTS_PATH, payload);
  return athleteSportSchema.parse(response.data);
}

export async function deleteAthleteSport(client: ApiClient, sportId: string): Promise<void> {
  await client.axios.delete(`${SPORTS_PATH}/${sportId}`);
}

export async function setPrimaryAthleteSport(
  client: ApiClient,
  sportId: string,
): Promise<AthleteSport> {
  const response = await client.axios.put(`${SPORTS_PATH}/${sportId}/primary`);
  return athleteSportSchema.parse(response.data);
}

export async function fetchAthleteGoals(client: ApiClient): Promise<AthleteGoal[]> {
  try {
    const response = await client.axios.get(GOALS_PATH);
    return athleteGoalsListSchema.parse(response.data);
  } catch (error) {
    if (isAthleteProfileNotFound(error)) {
      return [];
    }
    throw error;
  }
}

export async function createAthleteGoal(
  client: ApiClient,
  request: CreateAthleteGoalRequest,
): Promise<AthleteGoal> {
  const response = await client.axios.post(GOALS_PATH, request);
  return athleteGoalSchema.parse(response.data);
}

export async function changeAthleteGoalStatus(
  client: ApiClient,
  goalId: string,
  action: GoalStatusAction,
): Promise<AthleteGoal> {
  const response = await client.axios.patch(`${GOALS_PATH}/${goalId}/status`, { action });
  return athleteGoalSchema.parse(response.data);
}

export async function deleteAthleteGoal(client: ApiClient, goalId: string): Promise<void> {
  await client.axios.delete(`${GOALS_PATH}/${goalId}`);
}

export async function removeAthleteGoal(client: ApiClient, goal: AthleteGoal): Promise<void> {
  if (goal.status !== 'CANCELLED') {
    await changeAthleteGoalStatus(client, goal.id, 'CANCEL');
  }
  await deleteAthleteGoal(client, goal.id);
}
