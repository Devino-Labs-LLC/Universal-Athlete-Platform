import { ApiClient } from '@/src/core/api/apiClient';
import {
  ApplyAdaptationProposalRequest,
  GenerateAdaptationProposalRequest,
  UpdateAdaptationItemRequest,
  WorkoutAdaptationApplication,
  WorkoutAdaptationProposal,
  WorkoutAdaptationProposalSummary,
  applyAdaptationProposalRequestSchema,
  generateAdaptationProposalRequestSchema,
  updateAdaptationItemRequestSchema,
  workoutAdaptationApplicationSchema,
  workoutAdaptationProposalSchema,
  workoutAdaptationProposalSummarySchema,
} from '@/src/features/adaptation/models/adaptationSchemas';

export async function fetchAdaptationProposal(
  client: ApiClient,
  proposalId: string,
): Promise<WorkoutAdaptationProposal> {
  const response = await client.axios.get(
    `/api/v1/training/adaptation-proposals/${proposalId}`,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function listAdaptationProposals(
  client: ApiClient,
  params?: { occurrenceId?: string; status?: string; page?: number; size?: number },
): Promise<WorkoutAdaptationProposalSummary[]> {
  const response = await client.axios.get('/api/v1/training/adaptation-proposals', {
    params,
  });
  return workoutAdaptationProposalSummarySchema.array().parse(response.data);
}

export async function generateManualAdaptationProposal(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  body?: GenerateAdaptationProposalRequest,
): Promise<WorkoutAdaptationProposal> {
  const payload = body ? generateAdaptationProposalRequestSchema.parse(body) : undefined;
  const response = await client.axios.post(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/adaptation-proposals`,
    payload,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function generateRecommendedAdaptationProposal(
  client: ApiClient,
  recommendationId: string,
  occurrenceId: string,
  body?: GenerateAdaptationProposalRequest,
): Promise<WorkoutAdaptationProposal> {
  const payload = body ? generateAdaptationProposalRequestSchema.parse(body) : undefined;
  const response = await client.axios.post(
    `/api/v1/training/recommendations/${recommendationId}/occurrences/${occurrenceId}/adaptation-proposals`,
    payload,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function updateAdaptationProposalItem(
  client: ApiClient,
  proposalId: string,
  itemId: string,
  body: UpdateAdaptationItemRequest,
): Promise<WorkoutAdaptationProposal> {
  const payload = updateAdaptationItemRequestSchema.parse(body);
  const response = await client.axios.patch(
    `/api/v1/training/adaptation-proposals/${proposalId}/items/${itemId}`,
    payload,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function cancelAdaptationProposal(
  client: ApiClient,
  proposalId: string,
): Promise<WorkoutAdaptationProposal> {
  const response = await client.axios.post(
    `/api/v1/training/adaptation-proposals/${proposalId}/cancel`,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function regenerateAdaptationProposal(
  client: ApiClient,
  proposalId: string,
  body?: GenerateAdaptationProposalRequest,
): Promise<WorkoutAdaptationProposal> {
  const payload = body ? generateAdaptationProposalRequestSchema.parse(body) : undefined;
  const response = await client.axios.post(
    `/api/v1/training/adaptation-proposals/${proposalId}/regenerate`,
    payload,
  );
  return workoutAdaptationProposalSchema.parse(response.data);
}

export async function applyAdaptationProposal(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  proposalId: string,
  body: ApplyAdaptationProposalRequest,
): Promise<WorkoutAdaptationApplication> {
  const payload = applyAdaptationProposalRequestSchema.parse(body);
  const response = await client.axios.post(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/adaptation-proposals/${proposalId}/apply`,
    payload,
  );
  return workoutAdaptationApplicationSchema.parse(response.data);
}
