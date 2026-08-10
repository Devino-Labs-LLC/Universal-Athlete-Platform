import { ApiClient } from '@/src/core/api/apiClient';
import {
  ExerciseExecutionDetail,
  exerciseExecutionDetailSchema,
  exerciseExecutionsSchema,
} from '@/src/features/training/execution/models/executionSchemas';

function executionBase(
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}`;
}

function exercisesBase(planId: string, dayId: string, occurrenceId: string): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises`;
}

export async function fetchExerciseExecutions(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
): Promise<ExerciseExecutionDetail[]> {
  const response = await client.axios.get(exercisesBase(planId, dayId, occurrenceId));
  return exerciseExecutionsSchema.parse(response.data);
}

export async function fetchExerciseExecution(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<ExerciseExecutionDetail> {
  const response = await client.axios.get(
    `${exercisesBase(planId, dayId, occurrenceId)}/${executionId}`,
  );
  return exerciseExecutionDetailSchema.parse(response.data);
}

export async function completeExerciseExecution(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<ExerciseExecutionDetail> {
  const response = await client.axios.post(
    `${executionBase(planId, dayId, occurrenceId, executionId)}/complete`,
  );
  return exerciseExecutionDetailSchema.parse(response.data);
}

export async function skipExerciseExecution(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<ExerciseExecutionDetail> {
  const response = await client.axios.post(
    `${executionBase(planId, dayId, occurrenceId, executionId)}/skip`,
  );
  return exerciseExecutionDetailSchema.parse(response.data);
}
