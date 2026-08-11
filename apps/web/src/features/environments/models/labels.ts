import { equipmentTypeLabel, equipmentTypeOptions } from '@/features/exercises/models/labels';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import { trainingEnvironmentTypeSchema, type TrainingEnvironmentType } from '@/features/environments/models/schemas';

const ENVIRONMENT_TYPE_LABELS: Partial<Record<TrainingEnvironmentType, string>> = {
  HOME_GYM: 'Home gym',
  COMMERCIAL_GYM: 'Commercial gym',
  PRIVATE_GYM: 'Private gym',
  SCHOOL_GYM: 'School gym',
  TEAM_FACILITY: 'Team facility',
  COURT: 'Court',
  FIELD: 'Field',
  TRACK: 'Track',
  POOL: 'Pool',
  OUTDOOR: 'Outdoor',
  HOTEL: 'Hotel',
  TRAVEL: 'Travel',
  OFFICE: 'Office',
  OTHER: 'Other',
};

export function trainingEnvironmentTypeLabel(type: TrainingEnvironmentType | string): string {
  if (type in ENVIRONMENT_TYPE_LABELS) {
    return ENVIRONMENT_TYPE_LABELS[type as TrainingEnvironmentType]!;
  }
  return formatEnumLabel(type);
}

export const trainingEnvironmentTypeOptions = trainingEnvironmentTypeSchema.options.map((value) => ({
  value,
  label: trainingEnvironmentTypeLabel(value),
}));

export { equipmentTypeLabel, equipmentTypeOptions };
