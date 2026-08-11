import { describe, expect, it } from 'vitest';

import { exerciseKeys, TRAINING_EXERCISE_DEFINITIONS_PREFIX } from '@/features/exercises/models/queryKeys';

describe('exerciseKeys', () => {
  it('produces stable list keys for identical filters', () => {
    const a = exerciseKeys.list({ name: 'squat', page: 0, size: 20 });
    const b = exerciseKeys.list({ name: 'squat', page: 0, size: 20 });
    expect(a).toEqual(b);
  });

  it('produces distinct list keys for different filters', () => {
    const a = exerciseKeys.list({ name: 'squat' });
    const b = exerciseKeys.list({ name: 'deadlift' });
    expect(a).not.toEqual(b);
  });

  it('defaults page and size when omitted', () => {
    expect(exerciseKeys.list()).toEqual(exerciseKeys.list({ page: 0, size: 20 }));
  });

  it('scopes detail keys under the exercise namespace', () => {
    expect(exerciseKeys.detail('def-1')).toEqual([...exerciseKeys.details(), 'def-1']);
  });

  it('sorts equipment filters for candidates so order does not affect cache identity', () => {
    const a = exerciseKeys.candidates('source-1', { equipment: ['BARBELL', 'BENCH'] });
    const b = exerciseKeys.candidates('source-1', { equipment: ['BENCH', 'BARBELL'] });
    expect(a).toEqual(b);
  });

  it('candidate keys differ between equipment and environment filters', () => {
    const equipment = exerciseKeys.candidates('source-1', { equipment: ['BARBELL'] });
    const environment = exerciseKeys.candidates('source-1', { trainingEnvironmentId: 'env-1' });
    expect(equipment).not.toEqual(environment);
  });

  it('exposes the W3 training prefix used for chooser invalidation', () => {
    expect(TRAINING_EXERCISE_DEFINITIONS_PREFIX).toEqual(['training', 'exerciseDefinitions']);
  });

  it('scopes compatibility keys per exercise and environment', () => {
    expect(exerciseKeys.compatibility('def-1', 'env-1')).toEqual([
      ...exerciseKeys.compatibilityFor('def-1'),
      'env-1',
    ]);
  });
});
