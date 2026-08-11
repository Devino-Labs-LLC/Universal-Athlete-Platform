import { z } from 'zod';

export const TRAINING_ENVIRONMENT_TYPES = [
  'HOME_GYM',
  'COMMERCIAL_GYM',
  'PRIVATE_GYM',
  'SCHOOL_GYM',
  'TEAM_FACILITY',
  'COURT',
  'FIELD',
  'TRACK',
  'POOL',
  'OUTDOOR',
  'HOTEL',
  'TRAVEL',
  'OFFICE',
  'OTHER',
] as const;

export type TrainingEnvironmentType = (typeof TRAINING_ENVIRONMENT_TYPES)[number];

export const EQUIPMENT_TYPES = [
  'BODYWEIGHT',
  'BARBELL',
  'DUMBBELL',
  'KETTLEBELL',
  'WEIGHT_PLATE',
  'RESISTANCE_BAND',
  'CABLE_MACHINE',
  'SELECTORIZED_MACHINE',
  'PLATE_LOADED_MACHINE',
  'SMITH_MACHINE',
  'SQUAT_RACK',
  'BENCH',
  'PULL_UP_BAR',
  'DIP_STATION',
  'MEDICINE_BALL',
  'SLAM_BALL',
  'STABILITY_BALL',
  'BOSU_BALL',
  'FOAM_ROLLER',
  'TRAP_BAR',
  'EZ_BAR',
  'LANDMINE',
  'SLED',
  'BATTLE_ROPE',
  'PLYOMETRIC_BOX',
  'AGILITY_LADDER',
  'CONE',
  'HURDLE',
  'TREADMILL',
  'STATIONARY_BIKE',
  'ROWING_MACHINE',
  'ELLIPTICAL',
  'STAIR_MACHINE',
  'TRACK',
  'FIELD',
  'POOL',
  'COURT',
  'OPEN_SPACE',
  'OTHER',
] as const;

export type EquipmentType = (typeof EQUIPMENT_TYPES)[number];

export const trainingEnvironmentTypeSchema = z.enum(TRAINING_ENVIRONMENT_TYPES);
export const equipmentTypeSchema = z.enum(EQUIPMENT_TYPES);

export const trainingEnvironmentSchema = z
  .object({
    id: z.string(),
    athleteId: z.string(),
    name: z.string(),
    type: trainingEnvironmentTypeSchema,
    availableEquipment: z.array(equipmentTypeSchema),
    description: z.string().nullable().optional(),
    facilityNotes: z.string().nullable().optional(),
    defaultEnvironment: z.boolean(),
    active: z.boolean(),
    archivedAt: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type TrainingEnvironment = z.infer<typeof trainingEnvironmentSchema>;

export const trainingEnvironmentPageSchema = z
  .object({
    environments: z.array(trainingEnvironmentSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
  })
  .passthrough();

export type TrainingEnvironmentPage = z.infer<typeof trainingEnvironmentPageSchema>;

export const trainingEnvironmentFormSchema = z.object({
  name: z.string().trim().min(2, 'Name must be at least 2 characters').max(100),
  type: trainingEnvironmentTypeSchema,
  availableEquipment: z.array(equipmentTypeSchema),
  description: z.string().max(2000).optional(),
  facilityNotes: z.string().max(2000).optional(),
  defaultEnvironment: z.boolean().optional(),
});

export type TrainingEnvironmentFormValues = z.infer<typeof trainingEnvironmentFormSchema>;

export interface TrainingEnvironmentListFilters {
  type?: TrainingEnvironmentType;
  equipment?: EquipmentType[];
  activeOnly?: boolean;
  page?: number;
  size?: number;
}

export interface CreateTrainingEnvironmentRequest {
  name: string;
  type: TrainingEnvironmentType;
  availableEquipment?: EquipmentType[];
  description?: string;
  facilityNotes?: string;
  defaultEnvironment?: boolean;
}

/**
 * Backend PatchValue: omit field = no change; JSON null or value = present.
 * Do NOT wrap as `{ value: T }`.
 */
export interface UpdateTrainingEnvironmentRequest {
  name?: string;
  type?: TrainingEnvironmentType;
  availableEquipment?: EquipmentType[] | null;
  description?: string | null;
  facilityNotes?: string | null;
  defaultEnvironment?: boolean;
}

export interface SetOccurrenceEnvironmentRequest {
  trainingEnvironmentId: string;
}
