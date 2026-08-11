import { QueryClient } from '@tanstack/react-query';

import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';
import { invalidateAfterOccurrenceEnvironmentMutation } from '@/src/features/environments/models/invalidation';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

describe('occurrence environment invalidation', () => {
  it('invalidates occurrence, launch, today, overview, and adaptation caches', async () => {
    const client = new QueryClient();
    const spy = jest.spyOn(client, 'invalidateQueries');

    await invalidateAfterOccurrenceEnvironmentMutation(client, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
    });

    expect(spy).toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
    expect(spy).toHaveBeenCalledWith({
      queryKey: trainingKeys.launch('plan-1', 'day-1', 'occ-1'),
    });
    expect(spy).toHaveBeenCalledWith({ queryKey: todayQueryKeys.all });
    expect(spy).toHaveBeenCalledWith({ queryKey: ['training', 'overview'] });
    expect(spy).toHaveBeenCalledWith({ queryKey: adaptationKeys.all });
  });
});
