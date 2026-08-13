import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import type {
  CreateCheckInFormValues,
  CreateDailyRecoveryCheckInRequest,
  DiscomfortInput,
  UpdateDailyRecoveryCheckInRequest,
} from '@/features/recovery/models/checkInForm';
import { normalizeDiscomfortSide } from '@/features/recovery/models/checkInForm';
import type { RecoveryCheckInListFilters } from '@/features/recovery/models/queryKeys';
import {
  type AthleteRecoveryHistory,
  athleteRecoveryHistorySchema,
  type DailyRecoveryCheckIn,
  dailyRecoveryCheckInListSchema,
  dailyRecoveryCheckInSchema,
  type DailyRecoveryCheckInList,
  type RecoveryCheckInRevision,
  recoveryCheckInRevisionListSchema,
} from '@/features/recovery/models/schemas';

const BASE_PATH = '/api/v1/training/recovery-check-ins';

function toDiscomfortRequest(item: DiscomfortInput): DiscomfortInput {
  const notes = item.notes?.trim();
  return {
    bodyArea: item.bodyArea,
    side: normalizeDiscomfortSide(item.bodyArea, item.side),
    intensity: item.intensity,
    notes: notes ? notes : undefined,
  };
}

export async function createRecoveryCheckIn(
  client: ApiClient,
  request: CreateDailyRecoveryCheckInRequest,
): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.post(BASE_PATH, request);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function updateRecoveryCheckIn(
  client: ApiClient,
  checkInId: string,
  request: UpdateDailyRecoveryCheckInRequest,
): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.patch(`${BASE_PATH}/${checkInId}`, request);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

/**
 * Backend PatchValue: omit field = no change; JSON null or value = present.
 * Do NOT wrap as `{ value: T }` — Jackson deserializes the bare field.
 */
export function buildUpdateRequestFromForm(
  values: CreateCheckInFormValues,
  expectedVersion: number,
): UpdateDailyRecoveryCheckInRequest {
  return {
    fatigue: values.fatigue,
    muscleSoreness: values.muscleSoreness,
    stress: values.stress,
    mood: values.mood,
    motivation: values.motivation,
    sleepDurationMinutes: values.sleepDurationMinutes ?? null,
    sleepQuality: values.sleepQuality ?? null,
    discomfortAreas: values.discomfortAreas.map(toDiscomfortRequest),
    notes: values.notes?.trim() ? values.notes : null,
    expectedVersion,
  };
}

export function buildCreateRequestFromForm(
  values: CreateCheckInFormValues,
): CreateDailyRecoveryCheckInRequest {
  return {
    checkInDate: values.checkInDate,
    fatigue: values.fatigue,
    muscleSoreness: values.muscleSoreness,
    stress: values.stress,
    mood: values.mood,
    motivation: values.motivation,
    sleepDurationMinutes: values.sleepDurationMinutes,
    sleepQuality: values.sleepQuality,
    discomfortAreas:
      values.discomfortAreas.length > 0 ? values.discomfortAreas.map(toDiscomfortRequest) : undefined,
    notes: values.notes?.trim() ? values.notes : undefined,
  };
}

export function mapCheckInToFormValues(checkIn: DailyRecoveryCheckIn): CreateCheckInFormValues {
  return {
    checkInDate: checkIn.checkInDate,
    fatigue: checkIn.fatigue.value,
    muscleSoreness: checkIn.muscleSoreness.value,
    stress: checkIn.stress.value,
    mood: checkIn.mood.value,
    motivation: checkIn.motivation.value,
    sleepDurationMinutes: checkIn.sleepDurationMinutes ?? undefined,
    sleepQuality: checkIn.sleepQuality?.value,
    discomfortAreas: (checkIn.discomfortAreas ?? []).map((item) => ({
      bodyArea: item.bodyArea,
      side: item.side,
      intensity: item.intensity.value,
      notes: item.notes ?? undefined,
    })),
    notes: checkIn.notes ?? undefined,
  };
}

export async function fetchRecoveryCheckInById(client: ApiClient, checkInId: string): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/${checkInId}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function fetchRecoveryCheckInByDate(client: ApiClient, date: DateOnly): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/by-date/${date}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function fetchRecoveryCheckInList(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  filters: RecoveryCheckInListFilters = {},
): Promise<DailyRecoveryCheckInList> {
  const response = await client.axios.get(BASE_PATH, {
    params: {
      startDate,
      endDate,
      completeness: filters.completeness,
      minimumFatigue: filters.minimumFatigue,
      minimumSoreness: filters.minimumSoreness,
      bodyArea: filters.bodyArea,
      page: filters.page,
      size: filters.size,
    },
  });
  return dailyRecoveryCheckInListSchema.parse(response.data);
}

export async function fetchRecoveryCheckInRevisions(
  client: ApiClient,
  checkInId: string,
): Promise<RecoveryCheckInRevision[]> {
  const response = await client.axios.get(`${BASE_PATH}/${checkInId}/revisions`);
  return recoveryCheckInRevisionListSchema.parse(response.data).revisions;
}

export async function fetchRecoveryHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = true,
): Promise<AthleteRecoveryHistory> {
  const response = await client.axios.get(`${BASE_PATH}/history`, {
    params: { startDate, endDate, includeTrainingLoad },
  });
  return athleteRecoveryHistorySchema.parse(response.data);
}
