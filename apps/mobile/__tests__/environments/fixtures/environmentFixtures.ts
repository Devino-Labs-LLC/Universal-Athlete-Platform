import {
  EQUIPMENT_TYPES,
  TRAINING_ENVIRONMENT_TYPES,
  trainingEnvironmentSchema,
} from '@/src/features/environments/models/environmentSchemas';

export const sampleEnvironmentFixture = {
  id: 'env-1',
  athleteId: 'athlete-1',
  name: 'Home Gym',
  type: 'HOME_GYM',
  availableEquipment: ['BARBELL', 'DUMBBELL', 'SQUAT_RACK'],
  description: 'Garage setup with rack and bench.',
  facilityNotes: 'Limited ceiling height.',
  defaultEnvironment: true,
  active: true,
  archivedAt: null,
  createdAt: '2026-08-01T12:00:00Z',
  updatedAt: '2026-08-10T12:00:00Z',
};

export const archivedEnvironmentFixture = {
  ...sampleEnvironmentFixture,
  id: 'env-archived',
  name: 'Old Hotel Gym',
  type: 'HOTEL',
  defaultEnvironment: false,
  active: false,
  archivedAt: '2026-07-01T12:00:00Z',
};

export function parseEnvironmentFixture() {
  return trainingEnvironmentSchema.parse(sampleEnvironmentFixture);
}

export const allEnvironmentTypes = TRAINING_ENVIRONMENT_TYPES;
export const allEquipmentTypes = EQUIPMENT_TYPES;
