import { QueryClient } from '@tanstack/react-query';

import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import {
  invalidateOccurrenceQueries,
  invalidateSetValueUpdate,
} from '@/src/features/training/execution/models/invalidation';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

describe('execution invalidation helpers', () => {
  it('invalidates occurrence-scoped keys on start', async () => {
    const queryClient = new QueryClient();
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    await invalidateOccurrenceQueries(queryClient, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: trainingKeys.launch('plan-1', 'day-1', 'occ-1'),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['training', 'calendar'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['training', 'overview'] });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: todayQueryKeys.all });
  });

  it('invalidates sets and occurrence on set value update', async () => {
    const queryClient = new QueryClient();
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    await invalidateSetValueUpdate(queryClient, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
      executionId: 'exec-1',
    });

    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: trainingKeys.sets('plan-1', 'day-1', 'occ-1', 'exec-1'),
    });
    expect(invalidateSpy).toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
  });
});
