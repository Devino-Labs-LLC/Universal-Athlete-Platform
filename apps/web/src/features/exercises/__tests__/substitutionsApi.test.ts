import { describe, expect, it, vi } from 'vitest';

import {
  createSubstitutionRelationship,
  deleteSubstitutionRelationship,
  fetchSubstitutionCandidates,
  fetchSubstitutionRelationship,
  updateSubstitutionRelationship,
} from '@/features/exercises/api/substitutionsApi';

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

describe('substitutionsApi', () => {
  it('creates a substitution relationship under the source exercise', async () => {
    const client = makeClient();
    const request = {
      targetExerciseDefinitionId: 'target-1',
      relationshipType: 'EQUIVALENT_VARIATION' as const,
      compatibilityLevel: 'HIGH' as const,
    };
    client.axios.post.mockResolvedValue({
      data: { id: 'rel-1', targetExerciseDefinitionId: 'target-1', relationshipType: 'EQUIVALENT_VARIATION', compatibilityLevel: 'HIGH' },
    });
    await createSubstitutionRelationship(client as never, 'source-1', request);
    expect(client.axios.post).toHaveBeenCalledWith(
      '/api/v1/training/exercise-definitions/source-1/substitutions',
      request,
    );
  });

  it('requests candidates with an equipment filter only', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchSubstitutionCandidates(client as never, 'source-1', { equipment: ['BARBELL'] });
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/exercise-definitions/source-1/substitution-candidates',
      { params: { equipment: ['BARBELL'], trainingEnvironmentId: undefined } },
    );
  });

  it('requests candidates with an environment filter only, dropping equipment', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [] });
    await fetchSubstitutionCandidates(client as never, 'source-1', {
      equipment: ['BARBELL'],
      trainingEnvironmentId: 'env-1',
    });
    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/exercise-definitions/source-1/substitution-candidates',
      { params: { equipment: undefined, trainingEnvironmentId: 'env-1' } },
    );
  });

  it('preserves the exact server order when parsing candidates', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: [
        { relationshipId: 'r3', targetExerciseDefinitionId: 't3', targetCanonicalName: 'C', relationshipType: 'OTHER', compatibilityLevel: 'HIGH' },
        { relationshipId: 'r1', targetExerciseDefinitionId: 't1', targetCanonicalName: 'A', relationshipType: 'OTHER', compatibilityLevel: 'HIGH' },
      ],
    });
    const candidates = await fetchSubstitutionCandidates(client as never, 'source-1');
    expect(candidates.map((c) => c.relationshipId)).toEqual(['r3', 'r1']);
  });

  it('fetches a relationship by id', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: { id: 'rel-1', targetExerciseDefinitionId: 'target-1', relationshipType: 'OTHER', compatibilityLevel: 'HIGH' },
    });
    await fetchSubstitutionRelationship(client as never, 'rel-1');
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/training/exercise-substitution-relationships/rel-1');
  });

  it('updates a relationship with the full required payload (not PatchValue)', async () => {
    const client = makeClient();
    const request = {
      relationshipType: 'PROGRESSION' as const,
      compatibilityLevel: 'MODERATE' as const,
    };
    client.axios.patch.mockResolvedValue({
      data: { id: 'rel-1', targetExerciseDefinitionId: 'target-1', ...request },
    });
    await updateSubstitutionRelationship(client as never, 'rel-1', request);
    expect(client.axios.patch).toHaveBeenCalledWith(
      '/api/v1/training/exercise-substitution-relationships/rel-1',
      request,
    );
  });

  it('deletes a relationship', async () => {
    const client = makeClient();
    client.axios.delete.mockResolvedValue({ status: 204 });
    await deleteSubstitutionRelationship(client as never, 'rel-1');
    expect(client.axios.delete).toHaveBeenCalledWith('/api/v1/training/exercise-substitution-relationships/rel-1');
  });
});
