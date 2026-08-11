import {
  trainingEnvironmentFormSchema,
  trainingEnvironmentPageSchema,
  trainingEnvironmentSchema,
} from '@/src/features/environments/models/environmentSchemas';
import {
  allEquipmentTypes,
  allEnvironmentTypes,
  sampleEnvironmentFixture,
} from './fixtures/environmentFixtures';

describe('environmentSchemas', () => {
  it('parses a training environment response', () => {
    const parsed = trainingEnvironmentSchema.parse(sampleEnvironmentFixture);
    expect(parsed.id).toBe('env-1');
    expect(parsed.availableEquipment).toEqual(['BARBELL', 'DUMBBELL', 'SQUAT_RACK']);
    expect(parsed.defaultEnvironment).toBe(true);
  });

  it('parses environment list page', () => {
    const parsed = trainingEnvironmentPageSchema.parse({
      environments: [sampleEnvironmentFixture],
      page: 0,
      size: 50,
      totalElements: 1,
    });
    expect(parsed.environments).toHaveLength(1);
    expect(parsed.totalElements).toBe(1);
  });

  it('validates create form name length', () => {
    expect(() =>
      trainingEnvironmentFormSchema.parse({
        name: 'A',
        type: 'HOME_GYM',
        availableEquipment: [],
      }),
    ).toThrow();

    const valid = trainingEnvironmentFormSchema.parse({
      name: 'Home Gym',
      type: 'HOME_GYM',
      availableEquipment: ['BODYWEIGHT'],
    });
    expect(valid.name).toBe('Home Gym');
  });

  it('includes all backend environment types', () => {
    expect(allEnvironmentTypes).toHaveLength(14);
    expect(allEnvironmentTypes).toContain('TRAVEL');
    expect(allEnvironmentTypes).toContain('OTHER');
  });

  it('includes all backend equipment types without auto bodyweight', () => {
    expect(allEquipmentTypes).toHaveLength(39);
    expect(allEquipmentTypes).toContain('BODYWEIGHT');
    expect(allEquipmentTypes).toContain('OPEN_SPACE');
  });
});
