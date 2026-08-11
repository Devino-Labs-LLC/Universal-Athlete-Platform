import { describe, expect, it } from 'vitest';

import {
  createExerciseDefinitionSchema,
  createSubstitutionRefinedSchema,
  exerciseDefinitionPageSchema,
  exerciseDefinitionSchema,
  isSystemRelationship,
  substitutionCandidatesSchema,
  updateExerciseDefinitionSchema,
  updateSubstitutionRequestSchema,
  compatibilityResultSchema,
} from '@/features/exercises/models/schemas';

const validMetadata = {
  category: 'STRENGTH',
  metricMode: 'WEIGHT_AND_REPETITIONS',
  primaryMovementPattern: 'SQUAT',
  secondaryMovementPatterns: [] as string[],
  primaryMuscleGroups: ['QUADRICEPS'],
  secondaryMuscleGroups: [] as string[],
  requiredEquipment: ['BARBELL'],
  optionalEquipment: [] as string[],
  laterality: 'BILATERAL',
  kineticChainType: 'CLOSED_CHAIN',
  impactLevel: 'LOW_IMPACT',
  difficulty: 'INTERMEDIATE',
};

describe('exercise definition schemas', () => {
  it('parses a full definition detail response', () => {
    const parsed = exerciseDefinitionSchema.parse({
      id: 'def-1',
      exercisePerformanceKey: 'def-1',
      scope: 'SYSTEM',
      canonicalName: 'Back squat',
      normalizedName: 'back squat',
      metadata: validMetadata,
      active: true,
    });
    expect(parsed.canonicalName).toBe('Back squat');
    expect(parsed.scope).toBe('SYSTEM');
  });

  it('parses a list page envelope', () => {
    const page = exerciseDefinitionPageSchema.parse({
      definitions: [
        {
          id: 'def-1',
          scope: 'ATHLETE_CUSTOM',
          canonicalName: 'Goblet squat',
          metadata: validMetadata,
          active: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    expect(page.definitions).toHaveLength(1);
  });

  it('rejects create payload missing canonicalName', () => {
    const result = createExerciseDefinitionSchema.safeParse({
      canonicalName: '',
      metadata: validMetadata,
    });
    expect(result.success).toBe(false);
  });

  it('flags a movement pattern listed as both primary and secondary', () => {
    const result = createExerciseDefinitionSchema.safeParse({
      canonicalName: 'Test exercise',
      metadata: { ...validMetadata, secondaryMovementPatterns: ['SQUAT'] },
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues.some((issue) => issue.path.includes('secondaryMovementPatterns'))).toBe(
        true,
      );
    }
  });

  it('flags a muscle group listed as both primary and secondary', () => {
    const result = createExerciseDefinitionSchema.safeParse({
      canonicalName: 'Test exercise',
      metadata: {
        ...validMetadata,
        primaryMuscleGroups: ['QUADRICEPS'],
        secondaryMuscleGroups: ['QUADRICEPS'],
      },
    });
    expect(result.success).toBe(false);
  });

  it('flags equipment listed as both required and optional', () => {
    const result = createExerciseDefinitionSchema.safeParse({
      canonicalName: 'Test exercise',
      metadata: { ...validMetadata, requiredEquipment: ['BARBELL'], optionalEquipment: ['BARBELL'] },
    });
    expect(result.success).toBe(false);
  });

  it('accepts a partial PatchValue update payload', () => {
    const result = updateExerciseDefinitionSchema.safeParse({ canonicalName: 'New name' });
    expect(result.success).toBe(true);
  });

  it('omitted fields on update are undefined (PatchValue: omit = no change)', () => {
    const parsed = updateExerciseDefinitionSchema.parse({ canonicalName: 'New name' });
    expect(parsed.category).toBeUndefined();
    expect('category' in parsed).toBe(false);
  });

  it('distinguishes two exercises with the same name by scope and id', () => {
    const system = exerciseDefinitionSchema.parse({
      id: 'def-system-1',
      scope: 'SYSTEM',
      canonicalName: 'Back squat',
      metadata: validMetadata,
      active: true,
    });
    const custom = exerciseDefinitionSchema.parse({
      id: 'def-custom-1',
      scope: 'ATHLETE_CUSTOM',
      canonicalName: 'Back squat',
      metadata: validMetadata,
      active: true,
    });
    expect(system.canonicalName).toBe(custom.canonicalName);
    expect(system.scope).not.toBe(custom.scope);
    expect(system.id).not.toBe(custom.id);
  });
});

describe('substitution schemas', () => {
  it('requires a target exercise on create', () => {
    const result = createSubstitutionRefinedSchema('source-1').safeParse({
      targetExerciseDefinitionId: '',
      relationshipType: 'EQUIVALENT_VARIATION',
      compatibilityLevel: 'HIGH',
    });
    expect(result.success).toBe(false);
  });

  it('rejects a target exercise equal to the source', () => {
    const result = createSubstitutionRefinedSchema('source-1').safeParse({
      targetExerciseDefinitionId: 'source-1',
      relationshipType: 'EQUIVALENT_VARIATION',
      compatibilityLevel: 'HIGH',
    });
    expect(result.success).toBe(false);
  });

  it('accepts a valid create payload with a different target', () => {
    const result = createSubstitutionRefinedSchema('source-1').safeParse({
      targetExerciseDefinitionId: 'target-1',
      relationshipType: 'EQUIVALENT_VARIATION',
      compatibilityLevel: 'HIGH',
    });
    expect(result.success).toBe(true);
  });

  it('update schema requires relationshipType and compatibilityLevel (not PatchValue)', () => {
    const result = updateSubstitutionRequestSchema.safeParse({ rationale: 'ok' });
    expect(result.success).toBe(false);
  });

  it('identifies system relationships by null ownerAthleteId', () => {
    expect(
      isSystemRelationship({
        id: 'rel-1',
        targetExerciseDefinitionId: 'target-1',
        relationshipType: 'EQUIVALENT_VARIATION',
        compatibilityLevel: 'HIGH',
        ownerAthleteId: null,
      }),
    ).toBe(true);
    expect(
      isSystemRelationship({
        id: 'rel-2',
        targetExerciseDefinitionId: 'target-1',
        relationshipType: 'EQUIVALENT_VARIATION',
        compatibilityLevel: 'HIGH',
        ownerAthleteId: 'athlete-1',
      }),
    ).toBe(false);
  });

  it('preserves candidate order from the parsed payload (no re-sort)', () => {
    const raw = [
      { relationshipId: 'r3', targetExerciseDefinitionId: 't3', targetCanonicalName: 'C', relationshipType: 'OTHER', compatibilityLevel: 'MODERATE' },
      { relationshipId: 'r1', targetExerciseDefinitionId: 't1', targetCanonicalName: 'A', relationshipType: 'OTHER', compatibilityLevel: 'HIGH' },
      { relationshipId: 'r2', targetExerciseDefinitionId: 't2', targetCanonicalName: 'B', relationshipType: 'OTHER', compatibilityLevel: 'HIGH' },
    ];
    const parsed = substitutionCandidatesSchema.parse(raw);
    expect(parsed.map((c) => c.relationshipId)).toEqual(['r3', 'r1', 'r2']);
  });
});

describe('compatibility result schema', () => {
  it('parses compatible result with no missing equipment', () => {
    const parsed = compatibilityResultSchema.parse({
      exerciseDefinitionId: 'def-1',
      trainingEnvironmentId: 'env-1',
      trainingEnvironmentName: 'Home gym',
      compatible: true,
      requiredEquipment: ['BARBELL'],
      availableEquipment: ['BARBELL', 'BENCH'],
      missingRequiredEquipment: [],
    });
    expect(parsed.compatible).toBe(true);
    expect(parsed.missingRequiredEquipment).toEqual([]);
  });

  it('surfaces missing equipment only from the API payload', () => {
    const parsed = compatibilityResultSchema.parse({
      exerciseDefinitionId: 'def-1',
      trainingEnvironmentId: 'env-1',
      trainingEnvironmentName: 'Hotel room',
      compatible: false,
      requiredEquipment: ['BARBELL', 'SQUAT_RACK'],
      availableEquipment: [],
      missingRequiredEquipment: ['BARBELL', 'SQUAT_RACK'],
    });
    expect(parsed.compatible).toBe(false);
    expect(parsed.missingRequiredEquipment).toEqual(['BARBELL', 'SQUAT_RACK']);
  });
});
