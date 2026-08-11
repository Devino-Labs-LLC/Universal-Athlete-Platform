import { QueryClient } from '@tanstack/react-query';

import { performanceKeys } from '@/src/features/performance/models/performanceKeys';
import { invalidatePerformanceQueries } from '@/src/features/performance/models/invalidation';
import { invalidateOccurrenceTerminal } from '@/src/features/training/execution/models/invalidation';

describe('performance invalidation', () => {
  it('invalidates performance query prefix', async () => {
    const queryClient = new QueryClient();
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    await invalidatePerformanceQueries(queryClient);

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: performanceKeys.all });
  });

  it('extends occurrence terminal invalidation with performance keys', async () => {
    const queryClient = new QueryClient();
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

    await invalidateOccurrenceTerminal(queryClient, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
    });

    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: performanceKeys.all });
  });
});
