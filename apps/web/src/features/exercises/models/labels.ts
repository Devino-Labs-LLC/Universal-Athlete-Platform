import { formatEnumLabel } from '@/features/profile/enumLabels';
import {
  compatibilityLevelSchema,
  difficultySchema,
  EQUIPMENT_TYPES,
  equipmentTypeSchema,
  exerciseCategorySchema,
  exerciseScopeSchema,
  impactLevelSchema,
  kineticChainTypeSchema,
  lateralitySchema,
  metricModeSchema,
  movementPatternSchema,
  muscleGroupSchema,
  relationshipTypeSchema,
} from '@/features/exercises/models/schemas';

export const EXERCISE_SCOPE_LABELS: Record<string, string> = {
  SYSTEM: 'System',
  ATHLETE_CUSTOM: 'Custom',
};

export const EXERCISE_CATEGORY_LABELS: Record<string, string> = {
  STRENGTH: 'Strength',
  POWER: 'Power',
  PLYOMETRIC: 'Plyometric',
  SPEED: 'Speed',
  AGILITY: 'Agility',
  ENDURANCE: 'Endurance',
  MOBILITY: 'Mobility',
  FLEXIBILITY: 'Flexibility',
  BALANCE: 'Balance',
  STABILITY: 'Stability',
  SKILL: 'Skill',
  RECOVERY: 'Recovery',
  BREATHING: 'Breathing',
  OTHER: 'Other',
};

export const METRIC_MODE_LABELS: Record<string, string> = {
  REPETITIONS: 'Repetitions',
  WEIGHT_AND_REPETITIONS: 'Weight & reps',
  DURATION: 'Duration',
  DISTANCE: 'Distance',
  DISTANCE_AND_DURATION: 'Distance & duration',
  REPETITIONS_AND_DURATION: 'Reps & duration',
  WEIGHT_AND_DURATION: 'Weight & duration',
  MIXED: 'Mixed',
};

export const MOVEMENT_PATTERN_LABELS: Record<string, string> = {
  SQUAT: 'Squat',
  HINGE: 'Hinge',
  HORIZONTAL_PUSH: 'Horizontal push',
  VERTICAL_PUSH: 'Vertical push',
  HORIZONTAL_PULL: 'Horizontal pull',
  VERTICAL_PULL: 'Vertical pull',
  LUNGE: 'Lunge',
  CARRY: 'Carry',
  ROTATION: 'Rotation',
  ANTI_ROTATION: 'Anti-rotation',
  ANTI_EXTENSION: 'Anti-extension',
  ANTI_LATERAL_FLEXION: 'Anti-lateral flexion',
  GAIT: 'Gait',
  SPRINT: 'Sprint',
  JUMP: 'Jump',
  LANDING: 'Landing',
  THROW: 'Throw',
  CHANGE_OF_DIRECTION: 'Change of direction',
  LOCOMOTION: 'Locomotion',
  ISOMETRIC: 'Isometric',
  MOBILITY: 'Mobility',
  BREATHING: 'Breathing',
  SPORT_SKILL: 'Sport skill',
  OTHER: 'Other',
};

export const MUSCLE_GROUP_LABELS: Record<string, string> = {
  QUADRICEPS: 'Quadriceps',
  HAMSTRINGS: 'Hamstrings',
  GLUTES: 'Glutes',
  CALVES: 'Calves',
  CHEST: 'Chest',
  UPPER_BACK: 'Upper back',
  LATS: 'Lats',
  TRAPEZIUS: 'Trapezius',
  SHOULDERS: 'Shoulders',
  BICEPS: 'Biceps',
  TRICEPS: 'Triceps',
  FOREARMS: 'Forearms',
  ABDOMINALS: 'Abdominals',
  OBLIQUES: 'Obliques',
  SPINAL_ERECTORS: 'Spinal erectors',
  HIP_FLEXORS: 'Hip flexors',
  ADDUCTORS: 'Adductors',
  ABDUCTORS: 'Abductors',
  ROTATOR_CUFF: 'Rotator cuff',
  FULL_BODY: 'Full body',
  CARDIORESPIRATORY: 'Cardiorespiratory',
  OTHER: 'Other',
};

export const EQUIPMENT_LABELS: Record<string, string> = {
  BODYWEIGHT: 'Bodyweight',
  BARBELL: 'Barbell',
  DUMBBELL: 'Dumbbell',
  KETTLEBELL: 'Kettlebell',
  WEIGHT_PLATE: 'Weight plates',
  RESISTANCE_BAND: 'Resistance bands',
  CABLE_MACHINE: 'Cable machine',
  SELECTORIZED_MACHINE: 'Selectorized machine',
  PLATE_LOADED_MACHINE: 'Plate-loaded machine',
  SMITH_MACHINE: 'Smith machine',
  SQUAT_RACK: 'Squat rack',
  BENCH: 'Bench',
  PULL_UP_BAR: 'Pull-up bar',
  DIP_STATION: 'Dip station',
  MEDICINE_BALL: 'Medicine ball',
  SLAM_BALL: 'Slam ball',
  STABILITY_BALL: 'Stability ball',
  BOSU_BALL: 'BOSU ball',
  FOAM_ROLLER: 'Foam roller',
  TRAP_BAR: 'Trap bar',
  EZ_BAR: 'EZ bar',
  LANDMINE: 'Landmine',
  SLED: 'Sled',
  BATTLE_ROPE: 'Battle ropes',
  PLYOMETRIC_BOX: 'Plyometric box',
  AGILITY_LADDER: 'Agility ladder',
  CONE: 'Cone',
  HURDLE: 'Hurdle',
  TREADMILL: 'Treadmill',
  STATIONARY_BIKE: 'Stationary bike',
  ROWING_MACHINE: 'Rowing machine',
  ELLIPTICAL: 'Elliptical',
  STAIR_MACHINE: 'Stair machine',
  TRACK: 'Track',
  FIELD: 'Field',
  POOL: 'Pool',
  COURT: 'Court',
  OPEN_SPACE: 'Open space',
  OTHER: 'Other',
};

export const LATERALITY_LABELS: Record<string, string> = {
  BILATERAL: 'Bilateral',
  UNILATERAL: 'Unilateral',
  ALTERNATING: 'Alternating',
  ASYMMETRICAL: 'Asymmetrical',
  NOT_APPLICABLE: 'Not applicable',
};

export const KINETIC_CHAIN_TYPE_LABELS: Record<string, string> = {
  OPEN_CHAIN: 'Open chain',
  CLOSED_CHAIN: 'Closed chain',
  MIXED: 'Mixed',
  NOT_APPLICABLE: 'Not applicable',
};

export const IMPACT_LEVEL_LABELS: Record<string, string> = {
  NO_IMPACT: 'No impact',
  LOW_IMPACT: 'Low impact',
  MODERATE_IMPACT: 'Moderate impact',
  HIGH_IMPACT: 'High impact',
};

export const DIFFICULTY_LABELS: Record<string, string> = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

export const RELATIONSHIP_TYPE_LABELS: Record<string, string> = {
  EQUIVALENT_VARIATION: 'Equivalent variation',
  EQUIPMENT_ALTERNATIVE: 'Equipment alternative',
  REGRESSION: 'Regression',
  PROGRESSION: 'Progression',
  LOWER_IMPACT_ALTERNATIVE: 'Lower impact alternative',
  UNILATERAL_ALTERNATIVE: 'Unilateral alternative',
  BILATERAL_ALTERNATIVE: 'Bilateral alternative',
  TEMPORARY_MODIFICATION: 'Temporary modification',
  SPORT_SPECIFIC_VARIATION: 'Sport specific variation',
  OTHER: 'Other',
};

export const COMPATIBILITY_LEVEL_LABELS: Record<string, string> = {
  HIGH: 'High',
  MODERATE: 'Moderate',
  CONDITIONAL: 'Conditional',
};

function toOptions<T extends string>(values: readonly T[], labels: Record<string, string>) {
  return values.map((value) => ({ value, label: labels[value] ?? formatEnumLabel(value) }));
}

export const exerciseScopeOptions = toOptions(exerciseScopeSchema.options, EXERCISE_SCOPE_LABELS);
export const exerciseCategoryOptions = toOptions(exerciseCategorySchema.options, EXERCISE_CATEGORY_LABELS);
export const metricModeOptions = toOptions(metricModeSchema.options, METRIC_MODE_LABELS);
export const movementPatternOptions = toOptions(movementPatternSchema.options, MOVEMENT_PATTERN_LABELS);
export const muscleGroupOptions = toOptions(muscleGroupSchema.options, MUSCLE_GROUP_LABELS);
export const equipmentTypeOptions = toOptions(equipmentTypeSchema.options, EQUIPMENT_LABELS);
export const lateralityOptions = toOptions(lateralitySchema.options, LATERALITY_LABELS);
export const kineticChainTypeOptions = toOptions(kineticChainTypeSchema.options, KINETIC_CHAIN_TYPE_LABELS);
export const impactLevelOptions = toOptions(impactLevelSchema.options, IMPACT_LEVEL_LABELS);
export const difficultyOptions = toOptions(difficultySchema.options, DIFFICULTY_LABELS);
export const relationshipTypeOptions = toOptions(relationshipTypeSchema.options, RELATIONSHIP_TYPE_LABELS);
export const compatibilityLevelOptions = toOptions(
  compatibilityLevelSchema.options,
  COMPATIBILITY_LEVEL_LABELS,
);

export function sortedEquipmentTypes(): typeof EQUIPMENT_TYPES {
  return EQUIPMENT_TYPES;
}

export function equipmentTypeLabel(value: string): string {
  return EQUIPMENT_LABELS[value] ?? formatEnumLabel(value);
}
