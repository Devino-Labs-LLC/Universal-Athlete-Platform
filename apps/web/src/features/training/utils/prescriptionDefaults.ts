import type { ExerciseDefinition } from '@/features/training/models/schemas';
import type { CreateWorkoutExerciseRequest } from '@/features/training/models/schemas';

type PrescriptionCategory = CreateWorkoutExerciseRequest['category'];
type PrescriptionType = CreateWorkoutExerciseRequest['type'];

/**
 * Maps catalogue {@link ExerciseDefinition} category → prescription-side ExerciseCategory.
 * These enums are intentionally distinct in the domain; STABILITY cannot be persisted on a
 * workout prescription.
 */
export function mapDefinitionCategoryToPrescription(
  catalogCategory: string | undefined,
): PrescriptionCategory {
  switch (catalogCategory) {
    case 'STRENGTH':
      return 'STRENGTH';
    case 'POWER':
      return 'POWER';
    case 'PLYOMETRIC':
      return 'PLYOMETRICS';
    case 'ENDURANCE':
      return 'CARDIO';
    case 'SPEED':
    case 'AGILITY':
      return 'CONDITIONING';
    case 'MOBILITY':
    case 'BALANCE':
    case 'STABILITY':
      return 'MOBILITY';
    case 'FLEXIBILITY':
      return 'FLEXIBILITY';
    case 'SKILL':
      return 'SPORT_SKILL';
    case 'RECOVERY':
    case 'BREATHING':
      return 'RECOVERY';
    default:
      return 'OTHER';
  }
}

export function mapDefinitionEquipmentToType(
  requiredEquipment: string[] | undefined,
): PrescriptionType {
  const equipment = requiredEquipment ?? [];
  if (equipment.includes('BARBELL')) {
    return 'BARBELL';
  }
  if (equipment.includes('DUMBBELL')) {
    return 'DUMBBELL';
  }
  if (equipment.includes('KETTLEBELL')) {
    return 'KETTLEBELL';
  }
  if (equipment.includes('CABLE')) {
    return 'CABLE';
  }
  if (equipment.includes('RESISTANCE_BAND')) {
    return 'RESISTANCE_BAND';
  }
  if (equipment.includes('BODYWEIGHT') || equipment.length === 0) {
    return 'BODYWEIGHT';
  }
  return 'OTHER';
}

export function createDefaultsFromDefinition(
  definition: ExerciseDefinition,
): Pick<
  CreateWorkoutExerciseRequest,
  'exerciseDefinitionId' | 'exerciseName' | 'category' | 'type' | 'sets'
> {
  const metricMode = definition.metadata?.metricMode ?? 'MIXED';
  const type = mapDefinitionEquipmentToType(definition.metadata?.requiredEquipment);

  // Duration / distance holds still use sets (e.g. 3 timed planks); keep a modest default.
  const sets = metricMode === 'DISTANCE' || metricMode === 'DISTANCE_AND_DURATION' ? 1 : 3;

  return {
    exerciseDefinitionId: definition.id,
    exerciseName: definition.canonicalName,
    category: mapDefinitionCategoryToPrescription(definition.metadata?.category),
    type,
    sets,
  };
}
