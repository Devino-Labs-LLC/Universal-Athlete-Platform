import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import {
  invalidateCompatibilityQueries,
  invalidateExerciseDefinitionQueries,
  invalidateSubstitutionQueries,
} from '@/features/exercises/models/invalidation';
import { exerciseKeys, TRAINING_EXERCISE_DEFINITIONS_PREFIX } from '@/features/exercises/models/queryKeys';

describe('exercise invalidation', () => {
  it('invalidates the exercise list, detail, and the W3 training prefix on mutation', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateExerciseDefinitionQueries(client, 'def-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.lists() });
    expect(spy).toHaveBeenCalledWith({ queryKey: TRAINING_EXERCISE_DEFINITIONS_PREFIX });
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.detail('def-1') });
  });

  it('skips the detail invalidation when no id is provided', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateExerciseDefinitionQueries(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.lists() });
    expect(spy).not.toHaveBeenCalledWith({ queryKey: exerciseKeys.detail('def-1') });
  });

  it('invalidates substitution candidates and relationship detail', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateSubstitutionQueries(client, 'source-1', 'rel-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.candidatesFor('source-1') });
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.relationship('rel-1') });
  });

  it('invalidates compatibility queries for the exercise', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateCompatibilityQueries(client, 'def-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: exerciseKeys.compatibilityFor('def-1') });
  });
});
