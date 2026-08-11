import type { ApiClient } from '@/core/api/apiClient';
import {
  type CreateEnvironmentRequest,
  type EnvironmentListFilters,
  trainingEnvironmentPageSchema,
  type TrainingEnvironment,
  type TrainingEnvironmentPage,
  trainingEnvironmentSchema,
  type UpdateEnvironmentRequest,
} from '@/features/environments/models/schemas';

const BASE = '/api/v1/training/environments';

export async function fetchEnvironments(
  client: ApiClient,
  filters?: EnvironmentListFilters,
): Promise<TrainingEnvironmentPage> {
  const response = await client.axios.get(BASE, {
    params: {
      type: filters?.type,
      equipment: filters?.equipment,
      activeOnly: filters?.activeOnly ?? true,
      page: filters?.page ?? 0,
      size: filters?.size ?? 50,
    },
  });
  return trainingEnvironmentPageSchema.parse(response.data);
}

export async function fetchEnvironment(
  client: ApiClient,
  environmentId: string,
): Promise<TrainingEnvironment> {
  const response = await client.axios.get(`${BASE}/${environmentId}`);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function createEnvironment(
  client: ApiClient,
  request: CreateEnvironmentRequest,
): Promise<TrainingEnvironment> {
  const payload = {
    ...request,
    availableEquipment: request.availableEquipment.length > 0 ? request.availableEquipment : undefined,
    description: request.description?.trim() || undefined,
    facilityNotes: request.facilityNotes?.trim() || undefined,
  };
  const response = await client.axios.post(BASE, payload);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function updateEnvironment(
  client: ApiClient,
  environmentId: string,
  patch: UpdateEnvironmentRequest,
): Promise<TrainingEnvironment> {
  const response = await client.axios.patch(`${BASE}/${environmentId}`, patch);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function archiveEnvironment(client: ApiClient, environmentId: string): Promise<void> {
  await client.axios.delete(`${BASE}/${environmentId}`);
}

export async function setDefaultEnvironment(
  client: ApiClient,
  environmentId: string,
): Promise<TrainingEnvironment> {
  const response = await client.axios.post(`${BASE}/${environmentId}/default`, {});
  return trainingEnvironmentSchema.parse(response.data);
}
