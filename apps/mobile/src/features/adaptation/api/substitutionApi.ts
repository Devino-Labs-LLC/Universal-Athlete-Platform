import { ApiClient } from '@/src/core/api/apiClient';
import {
  SubstituteExerciseRequest,
  SubstitutionCandidate,
  SubstitutionHistoryEntry,
  substituteExerciseRequestSchema,
  substitutionCandidateSchema,
  substitutionHistoryEntrySchema,
} from '@/src/features/adaptation/models/adaptationSchemas';
import { exerciseExecutionSchema } from '@/src/features/training/models/browseSchemas';

export async function fetchSubstitutionCandidates(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<SubstitutionCandidate[]> {
  const response = await client.axios.get(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/substitution-candidates`,
  );
  return substitutionCandidateSchema.array().parse(response.data);
}

export async function substituteExercise(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  body: SubstituteExerciseRequest,
) {
  const payload = substituteExerciseRequestSchema.parse(body);
  const response = await client.axios.post(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/substitute`,
    payload,
  );
  return exerciseExecutionSchema.parse(response.data);
}

export async function revertExerciseSubstitution(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  notes?: string,
) {
  const response = await client.axios.post(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/substitute/revert`,
    notes ? { notes } : undefined,
  );
  return exerciseExecutionSchema.parse(response.data);
}

export async function fetchSubstitutionHistory(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<SubstitutionHistoryEntry[]> {
  const response = await client.axios.get(
    `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/substitutions`,
  );
  return substitutionHistoryEntrySchema.array().parse(response.data);
}
