import {
  EQUIPMENT_TYPES,
  EquipmentType,
  TRAINING_ENVIRONMENT_TYPES,
  TrainingEnvironmentType,
} from '@/src/features/environments/models/environmentSchemas';
import { enumOptions, formatEnumLabel } from '@/src/features/profile/enumLabels';

const ENVIRONMENT_TYPE_LABELS: Partial<Record<TrainingEnvironmentType, string>> = {
  HOME_GYM: 'Home gym',
  COMMERCIAL_GYM: 'Commercial gym',
  PRIVATE_GYM: 'Private gym',
  SCHOOL_GYM: 'School gym',
  TEAM_FACILITY: 'Team facility',
  HOTEL: 'Hotel gym',
};

const EQUIPMENT_LABELS: Partial<Record<EquipmentType, string>> = {
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
  PULL_UP_BAR: 'Pull-up bar',
  DIP_STATION: 'Dip station',
  MEDICINE_BALL: 'Medicine ball',
  SLAM_BALL: 'Slam ball',
  STABILITY_BALL: 'Stability ball',
  BOSU_BALL: 'BOSU ball',
  FOAM_ROLLER: 'Foam roller',
  TRAP_BAR: 'Trap bar',
  EZ_BAR: 'EZ bar',
  BATTLE_ROPE: 'Battle ropes',
  PLYOMETRIC_BOX: 'Plyometric box',
  AGILITY_LADDER: 'Agility ladder',
  STATIONARY_BIKE: 'Stationary bike',
  ROWING_MACHINE: 'Rowing machine',
  STAIR_MACHINE: 'Stair machine',
  OPEN_SPACE: 'Open space',
};

export function trainingEnvironmentTypeLabel(type: TrainingEnvironmentType | string): string {
  if (type in ENVIRONMENT_TYPE_LABELS) {
    return ENVIRONMENT_TYPE_LABELS[type as TrainingEnvironmentType]!;
  }
  return formatEnumLabel(type);
}

export function equipmentTypeLabel(equipment: EquipmentType | string): string {
  if (equipment in EQUIPMENT_LABELS) {
    return EQUIPMENT_LABELS[equipment as EquipmentType]!;
  }
  return formatEnumLabel(equipment);
}

export const trainingEnvironmentTypeOptions = enumOptions(TRAINING_ENVIRONMENT_TYPES);

export const equipmentTypeOptions = enumOptions(EQUIPMENT_TYPES).map((option) => ({
  value: option.value,
  label: equipmentTypeLabel(option.value),
}));

export function sortedEquipmentTypes(): EquipmentType[] {
  return [...EQUIPMENT_TYPES];
}
