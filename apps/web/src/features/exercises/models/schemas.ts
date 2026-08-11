import { z } from 'zod';

export const exerciseScopeSchema = z.enum(['SYSTEM', 'ATHLETE_CUSTOM']);
export type ExerciseScope = z.infer<typeof exerciseScopeSchema>;

export const exerciseCategorySchema = z.enum([
  'STRENGTH',
  'POWER',
  'PLYOMETRIC',
  'SPEED',
  'AGILITY',
  'ENDURANCE',
  'MOBILITY',
  'FLEXIBILITY',
  'BALANCE',
  'STABILITY',
  'SKILL',
  'RECOVERY',
  'BREATHING',
  'OTHER',
]);
export type ExerciseCategory = z.infer<typeof exerciseCategorySchema>;

export const metricModeSchema = z.enum([
  'REPETITIONS',
  'WEIGHT_AND_REPETITIONS',
  'DURATION',
  'DISTANCE',
  'DISTANCE_AND_DURATION',
  'REPETITIONS_AND_DURATION',
  'WEIGHT_AND_DURATION',
  'MIXED',
]);
export type MetricMode = z.infer<typeof metricModeSchema>;

export const movementPatternSchema = z.enum([
  'SQUAT',
  'HINGE',
  'HORIZONTAL_PUSH',
  'VERTICAL_PUSH',
  'HORIZONTAL_PULL',
  'VERTICAL_PULL',
  'LUNGE',
  'CARRY',
  'ROTATION',
  'ANTI_ROTATION',
  'ANTI_EXTENSION',
  'ANTI_LATERAL_FLEXION',
  'GAIT',
  'SPRINT',
  'JUMP',
  'LANDING',
  'THROW',
  'CHANGE_OF_DIRECTION',
  'LOCOMOTION',
  'ISOMETRIC',
  'MOBILITY',
  'BREATHING',
  'SPORT_SKILL',
  'OTHER',
]);
export type MovementPattern = z.infer<typeof movementPatternSchema>;

export const muscleGroupSchema = z.enum([
  'QUADRICEPS',
  'HAMSTRINGS',
  'GLUTES',
  'CALVES',
  'CHEST',
  'UPPER_BACK',
  'LATS',
  'TRAPEZIUS',
  'SHOULDERS',
  'BICEPS',
  'TRICEPS',
  'FOREARMS',
  'ABDOMINALS',
  'OBLIQUES',
  'SPINAL_ERECTORS',
  'HIP_FLEXORS',
  'ADDUCTORS',
  'ABDUCTORS',
  'ROTATOR_CUFF',
  'FULL_BODY',
  'CARDIORESPIRATORY',
  'OTHER',
]);
export type MuscleGroup = z.infer<typeof muscleGroupSchema>;

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
export const equipmentTypeSchema = z.enum(EQUIPMENT_TYPES);
export type EquipmentType = z.infer<typeof equipmentTypeSchema>;

export const lateralitySchema = z.enum([
  'BILATERAL',
  'UNILATERAL',
  'ALTERNATING',
  'ASYMMETRICAL',
  'NOT_APPLICABLE',
]);
export type Laterality = z.infer<typeof lateralitySchema>;

export const kineticChainTypeSchema = z.enum([
  'OPEN_CHAIN',
  'CLOSED_CHAIN',
  'MIXED',
  'NOT_APPLICABLE',
]);
export type KineticChainType = z.infer<typeof kineticChainTypeSchema>;

export const impactLevelSchema = z.enum([
  'NO_IMPACT',
  'LOW_IMPACT',
  'MODERATE_IMPACT',
  'HIGH_IMPACT',
]);
export type ImpactLevel = z.infer<typeof impactLevelSchema>;

export const difficultySchema = z.enum(['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT']);
export type Difficulty = z.infer<typeof difficultySchema>;

function hasOverlap<T>(a: readonly T[] = [], b: readonly T[] = []): boolean {
  const setB = new Set(b);
  return a.some((value) => setB.has(value));
}

const metadataShape = {
  category: exerciseCategorySchema,
  metricMode: metricModeSchema,
  primaryMovementPattern: movementPatternSchema,
  secondaryMovementPatterns: z.array(movementPatternSchema),
  primaryMuscleGroups: z.array(muscleGroupSchema),
  secondaryMuscleGroups: z.array(muscleGroupSchema),
  requiredEquipment: z.array(equipmentTypeSchema),
  optionalEquipment: z.array(equipmentTypeSchema),
  laterality: lateralitySchema,
  kineticChainType: kineticChainTypeSchema,
  impactLevel: impactLevelSchema,
  difficulty: difficultySchema,
};

export const exerciseDefinitionMetadataSchema = z.object(metadataShape).passthrough();
export type ExerciseDefinitionMetadata = z.infer<typeof exerciseDefinitionMetadataSchema>;

export const exerciseDefinitionSchema = z
  .object({
    id: z.string(),
    exercisePerformanceKey: z.string().optional(),
    scope: exerciseScopeSchema,
    canonicalName: z.string(),
    normalizedName: z.string().optional(),
    metadata: exerciseDefinitionMetadataSchema,
    active: z.boolean(),
    archivedAt: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type ExerciseDefinition = z.infer<typeof exerciseDefinitionSchema>;

export const exerciseDefinitionPageSchema = z
  .object({
    definitions: z.array(exerciseDefinitionSchema),
    page: z.number(),
    size: z.number(),
    totalElements: z.number(),
    totalPages: z.number(),
  })
  .passthrough();

export type ExerciseDefinitionPage = z.infer<typeof exerciseDefinitionPageSchema>;

function attachMetadataConflictChecks<T extends z.ZodTypeAny>(schema: T) {
  return schema.superRefine((value, ctx) => {
    const data = value as {
      primaryMovementPattern?: MovementPattern;
      secondaryMovementPatterns?: MovementPattern[];
      primaryMuscleGroups?: MuscleGroup[];
      secondaryMuscleGroups?: MuscleGroup[];
      requiredEquipment?: EquipmentType[];
      optionalEquipment?: EquipmentType[];
    };

    if (
      data.primaryMovementPattern &&
      data.secondaryMovementPatterns?.includes(data.primaryMovementPattern)
    ) {
      ctx.addIssue({
        code: 'custom',
        path: ['secondaryMovementPatterns'],
        message: 'Primary movement pattern cannot also be listed as secondary.',
      });
    }

    if (hasOverlap(data.primaryMuscleGroups, data.secondaryMuscleGroups)) {
      ctx.addIssue({
        code: 'custom',
        path: ['secondaryMuscleGroups'],
        message: 'A muscle group cannot be both primary and secondary.',
      });
    }

    if (hasOverlap(data.requiredEquipment, data.optionalEquipment)) {
      ctx.addIssue({
        code: 'custom',
        path: ['optionalEquipment'],
        message: 'Equipment cannot be both required and optional.',
      });
    }
  });
}

export const exerciseDefinitionMetadataInputSchema = attachMetadataConflictChecks(
  z.object(metadataShape),
);

export const createExerciseDefinitionSchema = z.object({
  canonicalName: z.string().trim().min(1, 'Name is required').max(200),
  metadata: exerciseDefinitionMetadataInputSchema,
});

export type CreateExerciseDefinitionRequest = z.infer<typeof createExerciseDefinitionSchema>;

/**
 * Backend PatchValue contract: omit field = no change; present value = set.
 * Do NOT wrap fields as `{ value: T }`.
 */
export const updateExerciseDefinitionSchema = attachMetadataConflictChecks(
  z.object({
    canonicalName: z.string().trim().min(1).max(200).optional(),
    category: exerciseCategorySchema.optional(),
    metricMode: metricModeSchema.optional(),
    primaryMovementPattern: movementPatternSchema.optional(),
    secondaryMovementPatterns: z.array(movementPatternSchema).optional(),
    primaryMuscleGroups: z.array(muscleGroupSchema).optional(),
    secondaryMuscleGroups: z.array(muscleGroupSchema).optional(),
    requiredEquipment: z.array(equipmentTypeSchema).optional(),
    optionalEquipment: z.array(equipmentTypeSchema).optional(),
    laterality: lateralitySchema.optional(),
    kineticChainType: kineticChainTypeSchema.optional(),
    impactLevel: impactLevelSchema.optional(),
    difficulty: difficultySchema.optional(),
  }),
);

export type UpdateExerciseDefinitionRequest = z.infer<typeof updateExerciseDefinitionSchema>;

export interface ExerciseDefinitionListFilters {
  name?: string;
  scope?: ExerciseScope;
  category?: ExerciseCategory;
  metricMode?: MetricMode;
  movementPattern?: MovementPattern;
  muscleGroup?: MuscleGroup;
  equipment?: EquipmentType;
  laterality?: Laterality;
  impactLevel?: ImpactLevel;
  difficulty?: Difficulty;
  page?: number;
  size?: number;
}

// --- Substitution relationships -------------------------------------------------

export const relationshipTypeSchema = z.enum([
  'EQUIVALENT_VARIATION',
  'EQUIPMENT_ALTERNATIVE',
  'REGRESSION',
  'PROGRESSION',
  'LOWER_IMPACT_ALTERNATIVE',
  'UNILATERAL_ALTERNATIVE',
  'BILATERAL_ALTERNATIVE',
  'TEMPORARY_MODIFICATION',
  'SPORT_SPECIFIC_VARIATION',
  'OTHER',
]);
export type RelationshipType = z.infer<typeof relationshipTypeSchema>;

export const compatibilityLevelSchema = z.enum(['HIGH', 'MODERATE', 'CONDITIONAL']);
export type CompatibilityLevel = z.infer<typeof compatibilityLevelSchema>;

export const createSubstitutionRequestSchema = z
  .object({
    targetExerciseDefinitionId: z.string().min(1, 'Choose a target exercise'),
    relationshipType: relationshipTypeSchema,
    compatibilityLevel: compatibilityLevelSchema,
    rationale: z.string().trim().max(1000).optional(),
  })
  .refine((value) => value.targetExerciseDefinitionId.length > 0, {
    message: 'Choose a target exercise',
    path: ['targetExerciseDefinitionId'],
  });

export type CreateSubstitutionRequest = z.infer<typeof createSubstitutionRequestSchema>;

export function createSubstitutionRefinedSchema(sourceId: string) {
  return createSubstitutionRequestSchema.refine(
    (value) => value.targetExerciseDefinitionId !== sourceId,
    { message: 'Target exercise must differ from the source exercise.', path: ['targetExerciseDefinitionId'] },
  );
}

/** NOT a PatchValue endpoint: relationshipType + compatibilityLevel are required on every update. */
export const updateSubstitutionRequestSchema = z.object({
  relationshipType: relationshipTypeSchema,
  compatibilityLevel: compatibilityLevelSchema,
  rationale: z.string().trim().max(1000).optional(),
});

export type UpdateSubstitutionRequest = z.infer<typeof updateSubstitutionRequestSchema>;

export const substitutionRelationshipSchema = z
  .object({
    id: z.string(),
    sourceExerciseDefinitionId: z.string().optional(),
    targetExerciseDefinitionId: z.string(),
    targetCanonicalName: z.string().optional(),
    relationshipType: relationshipTypeSchema,
    compatibilityLevel: compatibilityLevelSchema,
    rationale: z.string().nullable().optional(),
    ownerAthleteId: z.string().nullable().optional(),
    createdAt: z.string().nullable().optional(),
    updatedAt: z.string().nullable().optional(),
  })
  .passthrough();

export type SubstitutionRelationship = z.infer<typeof substitutionRelationshipSchema>;

export function isSystemRelationship(relationship: SubstitutionRelationship): boolean {
  return relationship.ownerAthleteId == null;
}

export const substitutionCandidateSchema = z
  .object({
    relationshipId: z.string(),
    targetExerciseDefinitionId: z.string(),
    targetCanonicalName: z.string(),
    relationshipType: relationshipTypeSchema,
    compatibilityLevel: compatibilityLevelSchema,
    rationale: z.string().nullable().optional(),
    trainingEnvironmentId: z.string().nullable().optional(),
    trainingEnvironmentName: z.string().nullable().optional(),
  })
  .passthrough();

export type SubstitutionCandidate = z.infer<typeof substitutionCandidateSchema>;

export const substitutionCandidatesSchema = z.array(substitutionCandidateSchema);

export interface SubstitutionCandidateFilters {
  equipment?: EquipmentType[];
  trainingEnvironmentId?: string;
}

// --- Compatibility ---------------------------------------------------------------

export const compatibilityResultSchema = z
  .object({
    exerciseDefinitionId: z.string(),
    trainingEnvironmentId: z.string(),
    trainingEnvironmentName: z.string(),
    compatible: z.boolean(),
    requiredEquipment: z.array(equipmentTypeSchema).default([]),
    availableEquipment: z.array(equipmentTypeSchema).default([]),
    missingRequiredEquipment: z.array(equipmentTypeSchema).default([]),
  })
  .passthrough();

export type CompatibilityResult = z.infer<typeof compatibilityResultSchema>;
