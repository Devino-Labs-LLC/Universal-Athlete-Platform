import { describe, expect, it } from 'vitest';

import {
  compatibilityLevelOptions,
  difficultyOptions,
  equipmentTypeLabel,
  equipmentTypeOptions,
  exerciseCategoryOptions,
  exerciseScopeOptions,
  impactLevelOptions,
  kineticChainTypeOptions,
  lateralityOptions,
  metricModeOptions,
  movementPatternOptions,
  muscleGroupOptions,
  relationshipTypeOptions,
} from '@/features/exercises/models/labels';
import {
  compatibilityLevelSchema,
  difficultySchema,
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

describe('exercise enum label completeness', () => {
  const cases: Array<[string, { options: readonly string[] }, Array<{ value: string; label: string }>]> = [
    ['scope', exerciseScopeSchema, exerciseScopeOptions],
    ['category', exerciseCategorySchema, exerciseCategoryOptions],
    ['metricMode', metricModeSchema, metricModeOptions],
    ['movementPattern', movementPatternSchema, movementPatternOptions],
    ['muscleGroup', muscleGroupSchema, muscleGroupOptions],
    ['equipmentType', equipmentTypeSchema, equipmentTypeOptions],
    ['laterality', lateralitySchema, lateralityOptions],
    ['kineticChainType', kineticChainTypeSchema, kineticChainTypeOptions],
    ['impactLevel', impactLevelSchema, impactLevelOptions],
    ['difficulty', difficultySchema, difficultyOptions],
    ['relationshipType', relationshipTypeSchema, relationshipTypeOptions],
    ['compatibilityLevel', compatibilityLevelSchema, compatibilityLevelOptions],
  ];

  it.each(cases)('%s options cover every enum value with a non-empty label', (_name, schema, options) => {
    expect(options).toHaveLength(schema.options.length);
    for (const value of schema.options) {
      const match = options.find((option) => option.value === value);
      expect(match).toBeDefined();
      expect(match?.label.length).toBeGreaterThan(0);
    }
  });

  it('equipmentTypeLabel falls back gracefully for unknown values', () => {
    expect(equipmentTypeLabel('BARBELL')).toBe('Barbell');
    expect(equipmentTypeLabel('SOME_NEW_EQUIPMENT')).toContain('Some');
  });
});
