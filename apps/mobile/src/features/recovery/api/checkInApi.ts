import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  CreateDailyRecoveryCheckInRequest,
  DailyRecoveryCheckIn,
  dailyRecoveryCheckInSchema,
} from '@/src/features/recovery/models/recoverySchemas';

const BASE_PATH = '/api/v1/training/recovery-check-ins';

/**
 * Backend PatchValue: omit field = no change; JSON null or value = present.
 * Do NOT wrap as `{ value: T }` — Jackson deserializes the bare field.
 */
export interface UpdateDailyRecoveryCheckInRequest {
  sleepDurationMinutes?: number | null;
  sleepQuality?: number | null;
  fatigue?: number;
  muscleSoreness?: number;
  stress?: number;
  mood?: number;
  motivation?: number;
  discomfortAreas?: {
    bodyArea: string;
    side: string;
    intensity: number;
    notes?: string;
  }[] | null;
  notes?: string | null;
  expectedVersion?: number;
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

export async function fetchRecoveryCheckInById(
  client: ApiClient,
  checkInId: string,
): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/${checkInId}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export async function fetchRecoveryCheckInByDate(
  client: ApiClient,
  date: DateOnly,
): Promise<DailyRecoveryCheckIn> {
  const response = await client.axios.get(`${BASE_PATH}/by-date/${date}`);
  return dailyRecoveryCheckInSchema.parse(response.data);
}

export function buildUpdateRequestFromForm(
  values: {
    fatigue: number;
    muscleSoreness: number;
    stress: number;
    mood: number;
    motivation: number;
    sleepDurationMinutes?: number;
    sleepQuality?: number;
    discomfortAreas: { bodyArea: string; side: string; intensity: number; notes?: string }[];
    notes?: string;
  },
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
    discomfortAreas: values.discomfortAreas.map((item) => ({
      bodyArea: item.bodyArea,
      side: item.side,
      intensity: item.intensity,
      notes: item.notes,
    })),
    notes: values.notes ?? null,
    expectedVersion,
  };
}

export function buildCreateRequestFromForm(values: {
  checkInDate: string;
  fatigue: number;
  muscleSoreness: number;
  stress: number;
  mood: number;
  motivation: number;
  sleepDurationMinutes?: number;
  sleepQuality?: number;
  discomfortAreas: { bodyArea: string; side: string; intensity: number; notes?: string }[];
  notes?: string;
}): CreateDailyRecoveryCheckInRequest {
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
      values.discomfortAreas.length > 0
        ? values.discomfortAreas.map((item) => ({
            bodyArea: item.bodyArea,
            side: item.side,
            intensity: item.intensity,
            notes: item.notes,
          }))
        : undefined,
    notes: values.notes,
  };
}

export function mapCheckInToFormValues(checkIn: DailyRecoveryCheckIn) {
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
