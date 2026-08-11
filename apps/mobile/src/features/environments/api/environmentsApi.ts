import { ApiClient } from '@/src/core/api/apiClient';
import {
  CreateTrainingEnvironmentRequest,
  TrainingEnvironment,
  TrainingEnvironmentFormValues,
  TrainingEnvironmentListFilters,
  TrainingEnvironmentPage,
  trainingEnvironmentPageSchema,
  trainingEnvironmentSchema,
  UpdateTrainingEnvironmentRequest,
} from '@/src/features/environments/models/environmentSchemas';

const BASE_PATH = '/api/v1/training/environments';

export async function createTrainingEnvironment(
  client: ApiClient,
  request: CreateTrainingEnvironmentRequest,
): Promise<TrainingEnvironment> {
  const response = await client.axios.post(BASE_PATH, request);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function listTrainingEnvironments(
  client: ApiClient,
  filters?: TrainingEnvironmentListFilters,
): Promise<TrainingEnvironmentPage> {
  const response = await client.axios.get(BASE_PATH, {
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

export async function fetchTrainingEnvironment(
  client: ApiClient,
  environmentId: string,
): Promise<TrainingEnvironment> {
  const response = await client.axios.get(`${BASE_PATH}/${environmentId}`);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function updateTrainingEnvironment(
  client: ApiClient,
  environmentId: string,
  request: UpdateTrainingEnvironmentRequest,
): Promise<TrainingEnvironment> {
  const response = await client.axios.patch(`${BASE_PATH}/${environmentId}`, request);
  return trainingEnvironmentSchema.parse(response.data);
}

export async function archiveTrainingEnvironment(
  client: ApiClient,
  environmentId: string,
): Promise<void> {
  await client.axios.delete(`${BASE_PATH}/${environmentId}`);
}

export async function setDefaultTrainingEnvironment(
  client: ApiClient,
  environmentId: string,
): Promise<TrainingEnvironment> {
  const response = await client.axios.post(`${BASE_PATH}/${environmentId}/default`);
  return trainingEnvironmentSchema.parse(response.data);
}

export function buildCreateRequestFromForm(
  values: TrainingEnvironmentFormValues,
): CreateTrainingEnvironmentRequest {
  return {
    name: values.name.trim(),
    type: values.type,
    availableEquipment:
      values.availableEquipment.length > 0 ? values.availableEquipment : undefined,
    description: values.description?.trim() || undefined,
    facilityNotes: values.facilityNotes?.trim() || undefined,
    defaultEnvironment: values.defaultEnvironment ?? undefined,
  };
}

export function buildUpdateRequestFromForm(
  values: TrainingEnvironmentFormValues,
): UpdateTrainingEnvironmentRequest {
  return {
    name: values.name.trim(),
    type: values.type,
    availableEquipment: values.availableEquipment,
    description: values.description?.trim() ? values.description.trim() : null,
    facilityNotes: values.facilityNotes?.trim() ? values.facilityNotes.trim() : null,
  };
}

export function mapEnvironmentToFormValues(
  environment: TrainingEnvironment,
): TrainingEnvironmentFormValues {
  return {
    name: environment.name,
    type: environment.type,
    availableEquipment: environment.availableEquipment ?? [],
    description: environment.description ?? undefined,
    facilityNotes: environment.facilityNotes ?? undefined,
    defaultEnvironment: environment.defaultEnvironment,
  };
}
