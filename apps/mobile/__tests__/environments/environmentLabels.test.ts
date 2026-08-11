import {
  equipmentTypeLabel,
  equipmentTypeOptions,
  trainingEnvironmentTypeLabel,
  trainingEnvironmentTypeOptions,
} from '@/src/features/environments/models/environmentLabels';

describe('environmentLabels', () => {
  it('labels environment types with readable copy', () => {
    expect(trainingEnvironmentTypeLabel('HOME_GYM')).toBe('Home gym');
    expect(trainingEnvironmentTypeLabel('COMMERCIAL_GYM')).toBe('Commercial gym');
    expect(trainingEnvironmentTypeLabel('COURT')).toBe('Court');
  });

  it('labels equipment types', () => {
    expect(equipmentTypeLabel('BODYWEIGHT')).toBe('Bodyweight');
    expect(equipmentTypeLabel('SQUAT_RACK')).toBe('Squat rack');
    expect(equipmentTypeLabel('OPEN_SPACE')).toBe('Open space');
  });

  it('exposes select options for all enum values', () => {
    expect(trainingEnvironmentTypeOptions).toHaveLength(14);
    expect(equipmentTypeOptions).toHaveLength(39);
  });
});
