import { QueryClient } from '@tanstack/react-query';

import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';
import {
  invalidateAfterApplyAdaptation,
  invalidateAfterDirectSubstitution,
  invalidateAfterProposalMutation,
} from '@/src/features/adaptation/models/invalidation';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

describe('adaptation invalidation', () => {
  it('invalidates proposal, launch, and today after proposal mutation', async () => {
    const client = new QueryClient();
    const spy = jest.spyOn(client, 'invalidateQueries');

    await invalidateAfterProposalMutation(client, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
      proposalId: 'prop-1',
    });

    expect(spy).toHaveBeenCalledWith({ queryKey: adaptationKeys.proposal('prop-1') });
    expect(spy).toHaveBeenCalledWith({ queryKey: trainingKeys.launch('plan-1', 'day-1', 'occ-1') });
    expect(spy).toHaveBeenCalledWith({ queryKey: todayQueryKeys.all });
  });

  it('invalidates occurrence and overview after apply', async () => {
    const client = new QueryClient();
    const spy = jest.spyOn(client, 'invalidateQueries');

    await invalidateAfterApplyAdaptation(client, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
      proposalId: 'prop-1',
    });

    expect(spy).toHaveBeenCalledWith({
      queryKey: trainingKeys.occurrence('plan-1', 'day-1', 'occ-1'),
    });
    expect(spy).toHaveBeenCalledWith({ queryKey: ['training', 'overview'] });
  });

  it('invalidates substitution history after direct substitution', async () => {
    const client = new QueryClient();
    const spy = jest.spyOn(client, 'invalidateQueries');

    await invalidateAfterDirectSubstitution(client, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
      executionId: 'exec-1',
    });

    expect(spy).toHaveBeenCalledWith({
      queryKey: adaptationKeys.substitutionHistory('plan-1', 'day-1', 'occ-1', 'exec-1'),
    });
  });
});
