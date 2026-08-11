import { describe, expect, it } from 'vitest';

import {
  buildCreateExerciseDefinitionRequest,
  buildExerciseDefinitionPatch,
  type ExerciseFormValues,
} from '@/features/exercises/utils/patchBuilders';

const baseValues: ExerciseFormValues = {
  canonicalName: '  Back squat  ',
  metadata: {
    category: 'STRENGTH',
    metricMode: 'WEIGHT_AND_REPETITIONS',
    primaryMovementPattern: 'SQUAT',
    secondaryMovementPatterns: [],
    primaryMuscleGroups: ['QUADRICEPS'],
    secondaryMuscleGroups: [],
    requiredEquipment: ['BARBELL'],
    optionalEquipment: [],
    laterality: 'BILATERAL',
    kineticChainType: 'CLOSED_CHAIN',
    impactLevel: 'LOW_IMPACT',
    difficulty: 'INTERMEDIATE',
  },
};

describe('buildCreateExerciseDefinitionRequest', () => {
  it('trims the canonical name and copies metadata', () => {
    const request = buildCreateExerciseDefinitionRequest(baseValues);
    expect(request.canonicalName).toBe('Back squat');
    expect(request.metadata).toEqual(baseValues.metadata);
  });
});

describe('buildExerciseDefinitionPatch', () => {
  it('omits fields that were never touched by the user', () => {
    const patch = buildExerciseDefinitionPatch({}, baseValues);
    expect(patch).toEqual({});
  });

  it('includes only the dirty top-level name field', () => {
    const patch = buildExerciseDefinitionPatch({ canonicalName: true }, baseValues);
    expect(patch).toEqual({ canonicalName: 'Back squat' });
  });

  it('includes only dirty metadata fields, leaving others omitted', () => {
    const patch = buildExerciseDefinitionPatch(
      { metadata: { category: true, requiredEquipment: true } },
      baseValues,
    );
    expect(patch).toEqual({
      category: 'STRENGTH',
      requiredEquipment: ['BARBELL'],
    });
    expect('metricMode' in patch).toBe(false);
    expect('canonicalName' in patch).toBe(false);
  });

  it('combines dirty name and dirty metadata fields', () => {
    const patch = buildExerciseDefinitionPatch(
      { canonicalName: true, metadata: { difficulty: true } },
      baseValues,
    );
    expect(patch).toEqual({ canonicalName: 'Back squat', difficulty: 'INTERMEDIATE' });
  });

  it('ignores metadata keys explicitly marked not dirty', () => {
    const patch = buildExerciseDefinitionPatch(
      { metadata: { category: true, difficulty: false } },
      baseValues,
    );
    expect(patch).toEqual({ category: 'STRENGTH' });
  });
});
