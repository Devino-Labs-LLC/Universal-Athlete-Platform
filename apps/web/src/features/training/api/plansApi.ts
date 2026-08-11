import type { ApiClient } from '@/core/api/apiClient';
import type { PlanListFilters } from '@/features/training/models/queryKeys';
import {
  type CreateTrainingPlanRequest,
  type TrainingPlan,
  trainingPlanSchema,
  trainingPlansSchema,
  type UpdateTrainingPlanRequest,
  planStatusActionSchema,
} from '@/features/training/models/schemas';

const BASE = '/api/v1/training/plans';

export async function fetchPlans(
  client: ApiClient,
  filters?: PlanListFilters,
): Promise<TrainingPlan[]> {
  const response = await client.axios.get(BASE, {
    params: {
      status: filters?.status,
      planType: filters?.planType,
    },
  });
  return trainingPlansSchema.parse(response.data);
}

export async function fetchPlan(client: ApiClient, planId: string): Promise<TrainingPlan> {
  const response = await client.axios.get(`${BASE}/${planId}`);
  return trainingPlanSchema.parse(response.data);
}

export async function createPlan(
  client: ApiClient,
  request: CreateTrainingPlanRequest,
): Promise<TrainingPlan> {
  const response = await client.axios.post(BASE, request);
  return trainingPlanSchema.parse(response.data);
}

export async function updatePlan(
  client: ApiClient,
  planId: string,
  request: UpdateTrainingPlanRequest,
): Promise<TrainingPlan> {
  const response = await client.axios.patch(`${BASE}/${planId}`, request);
  return trainingPlanSchema.parse(response.data);
}

export async function changePlanStatus(
  client: ApiClient,
  planId: string,
  action: 'ACTIVATE' | 'COMPLETE' | 'ARCHIVE',
): Promise<TrainingPlan> {
  const body = planStatusActionSchema.parse({ action });
  const response = await client.axios.patch(`${BASE}/${planId}/status`, body);
  return trainingPlanSchema.parse(response.data);
}

export async function deletePlan(client: ApiClient, planId: string): Promise<void> {
  await client.axios.delete(`${BASE}/${planId}`);
}
