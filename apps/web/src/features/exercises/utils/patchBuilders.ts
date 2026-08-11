import type {
  CreateExerciseDefinitionRequest,
  UpdateExerciseDefinitionRequest,
} from '@/features/exercises/models/schemas';

export type ExerciseFormValues = CreateExerciseDefinitionRequest;

export interface ExerciseFormDirtyFields {
  canonicalName?: boolean;
  metadata?: Partial<Record<keyof ExerciseFormValues['metadata'], boolean>>;
}

export function buildCreateExerciseDefinitionRequest(
  values: ExerciseFormValues,
): CreateExerciseDefinitionRequest {
  return {
    canonicalName: values.canonicalName.trim(),
    metadata: { ...values.metadata },
  };
}

/**
 * Flattens only the dirty fields into the bare PatchValue shape expected by
 * PATCH /exercise-definitions/{id}. Fields never touched by the user are
 * omitted entirely so unrelated metadata is left unchanged server-side.
 */
export function buildExerciseDefinitionPatch(
  dirtyFields: ExerciseFormDirtyFields,
  values: ExerciseFormValues,
): UpdateExerciseDefinitionRequest {
  const patch: UpdateExerciseDefinitionRequest = {};

  if (dirtyFields.canonicalName) {
    patch.canonicalName = values.canonicalName.trim();
  }

  const dirtyMetadataKeys = Object.keys(dirtyFields.metadata ?? {}) as Array<
    keyof ExerciseFormValues['metadata']
  >;

  for (const key of dirtyMetadataKeys) {
    if (dirtyFields.metadata?.[key]) {
      (patch as Record<string, unknown>)[key] = values.metadata[key];
    }
  }

  return patch;
}
