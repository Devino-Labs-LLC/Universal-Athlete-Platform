import { describe, expect, it, vi } from 'vitest';

import { fetchExerciseDefinition } from '@/features/exercises/api/exerciseDefinitionsApi';

function makeClient(data: unknown) {
  return { axios: { get: vi.fn().mockResolvedValue({ data }) } };
}

const validMetadata = {
  category: 'STRENGTH',
  metricMode: 'WEIGHT_AND_REPETITIONS',
  primaryMovementPattern: 'SQUAT',
  secondaryMovementPatterns: [],
  primaryMuscleGroups: [],
  secondaryMuscleGroups: [],
  requiredEquipment: [],
  optionalEquipment: [],
  laterality: 'BILATERAL',
  kineticChainType: 'CLOSED_CHAIN',
  impactLevel: 'LOW_IMPACT',
  difficulty: 'INTERMEDIATE',
};

// Representative schema (exerciseDefinitionSchema, wired through fetchExerciseDefinition).
// A malformed/partial API payload must fail closed (throw) rather than the UI
// silently rendering an incomplete or wrongly-typed object.
describe('RC16 — malformed API payloads fail closed via Zod parsing', () => {
  it('throws instead of returning a half-shaped exercise definition when required fields are missing', async () => {
    const client = makeClient({ scope: 'SYSTEM', canonicalName: 'Back squat' }); // missing id, metadata, active

    await expect(fetchExerciseDefinition(client as never, 'def-1')).rejects.toThrow();
  });

  it('throws when a field has the wrong runtime type (active as a string instead of boolean)', async () => {
    const client = makeClient({
      id: 'def-1',
      scope: 'SYSTEM',
      canonicalName: 'Back squat',
      metadata: validMetadata,
      active: 'yes',
    });

    await expect(fetchExerciseDefinition(client as never, 'def-1')).rejects.toThrow();
  });
});
