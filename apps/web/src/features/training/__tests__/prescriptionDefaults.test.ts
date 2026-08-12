import { describe, expect, it } from 'vitest';

import {
  createDefaultsFromDefinition,
  mapDefinitionCategoryToPrescription,
  mapDefinitionEquipmentToType,
} from '@/features/training/utils/prescriptionDefaults';
import type { ExerciseDefinition } from '@/features/training/models/schemas';

function definition(overrides: {
  name: string;
  category: string;
  metricMode: string;
  requiredEquipment: string[];
}): ExerciseDefinition {
  return {
    id: '11111111-1111-1111-1111-111111111103',
    exercisePerformanceKey: '11111111-1111-1111-1111-111111111103',
    scope: 'SYSTEM',
    canonicalName: overrides.name,
    normalizedName: overrides.name.toLowerCase(),
    active: true,
    metadata: {
      category: overrides.category as ExerciseDefinition['metadata']['category'],
      metricMode: overrides.metricMode as ExerciseDefinition['metadata']['metricMode'],
      primaryMovementPattern: 'SQUAT',
      secondaryMovementPatterns: [],
      primaryMuscleGroups: [],
      secondaryMuscleGroups: [],
      requiredEquipment: overrides.requiredEquipment as ExerciseDefinition['metadata']['requiredEquipment'],
      optionalEquipment: [],
      laterality: 'BILATERAL',
      kineticChainType: 'CLOSED_CHAIN',
      impactLevel: 'LOW_IMPACT',
      difficulty: 'BEGINNER',
    },
  };
}

describe('prescriptionDefaults', () => {
  it('maps catalog STABILITY to prescription MOBILITY and BODYWEIGHT for Plank', () => {
    expect(mapDefinitionCategoryToPrescription('STABILITY')).toBe('MOBILITY');
    expect(mapDefinitionEquipmentToType(['BODYWEIGHT'])).toBe('BODYWEIGHT');

    const defaults = createDefaultsFromDefinition(
      definition({
        name: 'Plank',
        category: 'STABILITY',
        metricMode: 'DURATION',
        requiredEquipment: ['BODYWEIGHT'],
      }),
    );

    expect(defaults).toEqual({
      exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
      exerciseName: 'Plank',
      category: 'MOBILITY',
      type: 'BODYWEIGHT',
      sets: 3,
    });
  });

  it('maps catalog STRENGTH + BARBELL for Bench Press', () => {
    const defaults = createDefaultsFromDefinition(
      definition({
        name: 'Bench Press',
        category: 'STRENGTH',
        metricMode: 'WEIGHT_AND_REPETITIONS',
        requiredEquipment: ['BARBELL'],
      }),
    );

    expect(defaults).toEqual({
      exerciseDefinitionId: '11111111-1111-1111-1111-111111111103',
      exerciseName: 'Bench Press',
      category: 'STRENGTH',
      type: 'BARBELL',
      sets: 3,
    });
  });
});
