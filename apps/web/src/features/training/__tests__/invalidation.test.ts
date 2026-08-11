import { QueryClient } from '@tanstack/react-query';

import {
  invalidateDayQueries,
  invalidateExerciseQueries,
  invalidateOccurrenceQueries,
  invalidatePlanQueries,
} from '@/features/training/models/invalidation';
import { trainingKeys } from '@/features/training/models/queryKeys';

describe('training invalidation', () => {
  it('invalidates plan queries without occurrence detail keys', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidatePlanQueries(client, 'plan-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.plan('plan-1') });
    expect(spy).not.toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
  });

  it('invalidates day queries scoped to plan', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateDayQueries(client, 'plan-1', 'day-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.days('plan-1') });
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.day('plan-1', 'day-1') });
  });

  it('invalidates exercise queries only', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateExerciseQueries(client, 'plan-1', 'day-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.exercises('plan-1', 'day-1') });
  });

  it('invalidates occurrence list and detail', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateOccurrenceQueries(client, 'plan-1', 'day-1', 'occ-1');
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.occurrences('plan-1', 'day-1') });
    expect(spy).toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
  });
});
