import { describe, expect, it, vi } from 'vitest';

import { fetchCompatibility } from '@/features/exercises/api/compatibilityApi';

describe('compatibilityApi', () => {
  it('requests the environment-compatibility endpoint for the given exercise and environment', async () => {
    const client = {
      axios: {
        get: vi.fn().mockResolvedValue({
          data: {
            exerciseDefinitionId: 'def-1',
            trainingEnvironmentId: 'env-1',
            trainingEnvironmentName: 'Home gym',
            compatible: false,
            requiredEquipment: ['BARBELL'],
            availableEquipment: [],
            missingRequiredEquipment: ['BARBELL'],
          },
        }),
      },
    };

    const result = await fetchCompatibility(client as never, 'def-1', 'env-1');

    expect(client.axios.get).toHaveBeenCalledWith(
      '/api/v1/training/exercise-definitions/def-1/environment-compatibility/env-1',
    );
    expect(result.compatible).toBe(false);
    expect(result.missingRequiredEquipment).toEqual(['BARBELL']);
  });
});
