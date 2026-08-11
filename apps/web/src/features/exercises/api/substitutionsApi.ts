import type { ApiClient } from '@/core/api/apiClient';
import {
  type CreateSubstitutionRequest,
  substitutionCandidatesSchema,
  type SubstitutionCandidate,
  type SubstitutionCandidateFilters,
  substitutionRelationshipSchema,
  type SubstitutionRelationship,
  type UpdateSubstitutionRequest,
} from '@/features/exercises/models/schemas';

const DEFINITIONS_BASE = '/api/v1/training/exercise-definitions';
const RELATIONSHIPS_BASE = '/api/v1/training/exercise-substitution-relationships';

export async function createSubstitutionRelationship(
  client: ApiClient,
  sourceId: string,
  request: CreateSubstitutionRequest,
): Promise<SubstitutionRelationship> {
  const response = await client.axios.post(
    `${DEFINITIONS_BASE}/${sourceId}/substitutions`,
    request,
  );
  return substitutionRelationshipSchema.parse(response.data);
}

/**
 * Only equipment OR trainingEnvironmentId may be provided — never both
 * (server rejects with CONFLICTING_EQUIPMENT_CONTEXT_FILTERS).
 */
export async function fetchSubstitutionCandidates(
  client: ApiClient,
  sourceId: string,
  filters?: SubstitutionCandidateFilters,
): Promise<SubstitutionCandidate[]> {
  const response = await client.axios.get(
    `${DEFINITIONS_BASE}/${sourceId}/substitution-candidates`,
    {
      params: {
        equipment: filters?.trainingEnvironmentId ? undefined : filters?.equipment,
        trainingEnvironmentId: filters?.trainingEnvironmentId,
      },
    },
  );
  return substitutionCandidatesSchema.parse(response.data);
}

export async function fetchSubstitutionRelationship(
  client: ApiClient,
  relationshipId: string,
): Promise<SubstitutionRelationship> {
  const response = await client.axios.get(`${RELATIONSHIPS_BASE}/${relationshipId}`);
  return substitutionRelationshipSchema.parse(response.data);
}

export async function updateSubstitutionRelationship(
  client: ApiClient,
  relationshipId: string,
  request: UpdateSubstitutionRequest,
): Promise<SubstitutionRelationship> {
  const response = await client.axios.patch(`${RELATIONSHIPS_BASE}/${relationshipId}`, request);
  return substitutionRelationshipSchema.parse(response.data);
}

export async function deleteSubstitutionRelationship(
  client: ApiClient,
  relationshipId: string,
): Promise<void> {
  await client.axios.delete(`${RELATIONSHIPS_BASE}/${relationshipId}`);
}
