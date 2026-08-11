import { describe, expect, it, vi } from 'vitest';

import {
  archiveExerciseDefinition,
  createExerciseDefinition,
  fetchExerciseDefinition,
  fetchExerciseDefinitions,
  updateExerciseDefinition,
} from '@/features/exercises/api/exerciseDefinitionsApi';

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

function makeClient() {
  return {
    axios: {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
    },
  };
}

describe('exerciseDefinitionsApi', () => {
  it('sends all supported list filters as query params', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });

    await fetchExerciseDefinitions(client as never, {
      name: 'squat',
      scope: 'SYSTEM',
      category: 'STRENGTH',
      metricMode: 'REPETITIONS',
      movementPattern: 'SQUAT',
      muscleGroup: 'QUADRICEPS',
      equipment: 'BARBELL',
      laterality: 'BILATERAL',
      impactLevel: 'LOW_IMPACT',
      difficulty: 'BEGINNER',
      page: 2,
      size: 10,
    });

    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/exercise-definitions', {
      params: {
        name: 'squat',
        scope: 'SYSTEM',
        category: 'STRENGTH',
        metricMode: 'REPETITIONS',
        movementPattern: 'SQUAT',
        muscleGroup: 'QUADRICEPS',
        equipment: 'BARBELL',
        laterality: 'BILATERAL',
        impactLevel: 'LOW_IMPACT',
        difficulty: 'BEGINNER',
        page: 2,
        size: 10,
      },
    });
  });

  it('defaults page/size when filters are omitted', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { definitions: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
    });
    await fetchExerciseDefinitions(client as never);
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/exercise-definitions',
      expect.objectContaining({ params: expect.objectContaining({ page: 0, size: 20 }) }),
    );
  });

  it('parses the list page envelope', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: {
        definitions: [
          { id: 'def-1', scope: 'SYSTEM', canonicalName: 'Back squat', metadata: validMetadata, active: true },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    const page = await fetchExerciseDefinitions(client as never);
    expect(page.definitions).toHaveLength(1);
    expect(page.totalPages).toBe(1);
  });

  it('fetches a single definition by id', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { id: 'def-1', scope: 'SYSTEM', canonicalName: 'Back squat', metadata: validMetadata, active: true },
    });
    const definition = await fetchExerciseDefinition(client as never, 'def-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/exercise-definitions/def-1');
    expect(definition.id).toBe('def-1');
  });

  it('posts the create payload as-is', async () => {
    const client = makeClient();
    const request = { canonicalName: 'Goblet squat', metadata: validMetadata };
    client.axios.post.mockResolvedValue({
      data: { id: 'def-2', scope: 'ATHLETE_CUSTOM', canonicalName: 'Goblet squat', metadata: validMetadata, active: true },
    });
    await createExerciseDefinition(client as never, request as never);
    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/training/exercise-definitions', request);
  });

  it('patches with a bare PatchValue body (no wrapping)', async () => {
    const client = makeClient();
    client.axios.patch.mockResolvedValue({
      data: { id: 'def-1', scope: 'ATHLETE_CUSTOM', canonicalName: 'New name', metadata: validMetadata, active: true },
    });
    await updateExerciseDefinition(client as never, 'def-1', { canonicalName: 'New name' });
    expect(client.axios.patch).toHaveBeenCalledWith('/api/v1/training/exercise-definitions/def-1', {
      canonicalName: 'New name',
    });
  });

  it('archives via DELETE and returns void', async () => {
    const client = makeClient();
    client.axios.delete.mockResolvedValue({ status: 204 });
    await expect(archiveExerciseDefinition(client as never, 'def-1')).resolves.toBeUndefined();
    expect(client.axios.delete).toHaveBeenCalledWith('/api/v1/training/exercise-definitions/def-1');
  });
});
