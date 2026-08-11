import type { ApiClient } from '@/core/api/apiClient';
import {
  type ActivateScheduleRequest,
  type GenerateOccurrencesRequest,
  type GenerationResult,
  generationResultSchema,
  type TrainingPlan,
  scheduleActivationResponseSchema,
  trainingPlanSchema,
} from '@/features/training/models/schemas';

function basePath(planId: string): string {
  return `/api/v1/training/plans/${planId}/schedule`;
}

function optionalDate(value?: string | null): string | undefined {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

/** Strip empty optional dates — Jackson rejects "" for LocalDate. */
export function toActivateSchedulePayload(request: ActivateScheduleRequest): ActivateScheduleRequest {
  return {
    scheduleStartDate: request.scheduleStartDate.trim(),
    scheduleEndDate: optionalDate(request.scheduleEndDate),
    timezone: request.timezone.trim(),
    recurrenceMode: request.recurrenceMode,
    generateThrough: optionalDate(request.generateThrough),
  };
}

export async function activateSchedule(
  client: ApiClient,
  planId: string,
  request: ActivateScheduleRequest,
): Promise<{ plan: TrainingPlan; generation?: GenerationResult | null }> {
  const response = await client.axios.post(
    `${basePath(planId)}/activate`,
    toActivateSchedulePayload(request),
  );
  return scheduleActivationResponseSchema.parse(response.data);
}

export async function pauseSchedule(client: ApiClient, planId: string): Promise<TrainingPlan> {
  const response = await client.axios.post(`${basePath(planId)}/pause`);
  return trainingPlanSchema.parse(response.data);
}

export async function resumeSchedule(client: ApiClient, planId: string): Promise<TrainingPlan> {
  const response = await client.axios.post(`${basePath(planId)}/resume`);
  return trainingPlanSchema.parse(response.data);
}

export async function completeSchedule(client: ApiClient, planId: string): Promise<TrainingPlan> {
  const response = await client.axios.post(`${basePath(planId)}/complete`);
  return trainingPlanSchema.parse(response.data);
}

export async function generateOccurrences(
  client: ApiClient,
  planId: string,
  request: GenerateOccurrencesRequest,
): Promise<GenerationResult> {
  const response = await client.axios.post(`${basePath(planId)}/generate`, request);
  return generationResultSchema.parse(response.data);
}
