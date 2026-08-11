import type {
  CreateEnvironmentRequest,
  EnvironmentFormValues,
  UpdateEnvironmentRequest,
} from '@/features/environments/models/schemas';

export interface EnvironmentFormDirtyFields {
  name?: boolean;
  type?: boolean;
  availableEquipment?: boolean;
  description?: boolean;
  facilityNotes?: boolean;
  defaultEnvironment?: boolean;
}

export function buildCreateEnvironmentRequest(values: EnvironmentFormValues): CreateEnvironmentRequest {
  return {
    name: values.name.trim(),
    type: values.type,
    availableEquipment: values.availableEquipment,
    description: values.description?.trim() || undefined,
    facilityNotes: values.facilityNotes?.trim() || undefined,
    defaultEnvironment: values.defaultEnvironment ?? undefined,
  };
}

/**
 * Flattens only dirty fields into the bare PatchValue shape expected by
 * PATCH /training/environments/{id}. Untouched fields are omitted so they
 * are left unchanged server-side.
 */
export function buildEnvironmentPatch(
  dirtyFields: EnvironmentFormDirtyFields,
  values: EnvironmentFormValues,
): UpdateEnvironmentRequest {
  const patch: UpdateEnvironmentRequest = {};

  if (dirtyFields.name) {
    patch.name = values.name.trim();
  }
  if (dirtyFields.type) {
    patch.type = values.type;
  }
  if (dirtyFields.availableEquipment) {
    patch.availableEquipment = values.availableEquipment;
  }
  if (dirtyFields.description) {
    patch.description = values.description?.trim() || null;
  }
  if (dirtyFields.facilityNotes) {
    patch.facilityNotes = values.facilityNotes?.trim() || null;
  }
  if (dirtyFields.defaultEnvironment) {
    patch.defaultEnvironment = values.defaultEnvironment;
  }

  return patch;
}
