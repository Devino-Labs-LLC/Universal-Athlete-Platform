import { ApiClient } from '@/src/core/api/apiClient';
import {
  AddWorkoutExerciseSetRequest,
  PatchWorkoutExerciseSetRequest,
  WorkoutExerciseSet,
  workoutExerciseSetSchema,
  workoutExerciseSetsSchema,
} from '@/src/features/training/execution/models/executionSchemas';

function setsBase(
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): string {
  return `/api/v1/training/plans/${planId}/days/${dayId}/occurrences/${occurrenceId}/exercises/${executionId}/sets`;
}

export async function fetchExerciseSets(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
): Promise<WorkoutExerciseSet[]> {
  const response = await client.axios.get(setsBase(planId, dayId, occurrenceId, executionId));
  return workoutExerciseSetsSchema.parse(response.data);
}

export async function fetchExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  setId: string,
): Promise<WorkoutExerciseSet> {
  const response = await client.axios.get(
    `${setsBase(planId, dayId, occurrenceId, executionId)}/${setId}`,
  );
  return workoutExerciseSetSchema.parse(response.data);
}

export async function addExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  request?: AddWorkoutExerciseSetRequest,
): Promise<WorkoutExerciseSet> {
  const response = await client.axios.post(
    setsBase(planId, dayId, occurrenceId, executionId),
    request ?? {},
  );
  return workoutExerciseSetSchema.parse(response.data);
}

export async function patchExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  setId: string,
  request: PatchWorkoutExerciseSetRequest,
): Promise<WorkoutExerciseSet> {
  const response = await client.axios.patch(
    `${setsBase(planId, dayId, occurrenceId, executionId)}/${setId}`,
    request,
  );
  return workoutExerciseSetSchema.parse(response.data);
}

export async function completeExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  setId: string,
): Promise<WorkoutExerciseSet> {
  const response = await client.axios.post(
    `${setsBase(planId, dayId, occurrenceId, executionId)}/${setId}/complete`,
  );
  return workoutExerciseSetSchema.parse(response.data);
}

export async function skipExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  setId: string,
): Promise<WorkoutExerciseSet> {
  const response = await client.axios.post(
    `${setsBase(planId, dayId, occurrenceId, executionId)}/${setId}/skip`,
  );
  return workoutExerciseSetSchema.parse(response.data);
}

export async function deleteExerciseSet(
  client: ApiClient,
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  setId: string,
): Promise<void> {
  await client.axios.delete(
    `${setsBase(planId, dayId, occurrenceId, executionId)}/${setId}`,
  );
}
