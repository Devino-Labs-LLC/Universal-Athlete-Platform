import { QueryClient } from '@tanstack/react-query';

import { athleteQueryKeys } from '@/src/features/profile/queryKeys';

describe('logout cache clearing', () => {
  it('clears athlete-specific cached data between accounts', () => {
    const queryClient = new QueryClient();

    queryClient.setQueryData(athleteQueryKeys.profile(), {
      id: 'athlete-a',
      firstName: 'Athlete',
      lastName: 'A',
    });
    queryClient.setQueryData(athleteQueryKeys.sports(), [{ id: 'sport-a' }]);
    queryClient.setQueryData(athleteQueryKeys.goals(), [{ id: 'goal-a' }]);

    queryClient.clear();

    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(athleteQueryKeys.sports())).toBeUndefined();
    expect(queryClient.getQueryData(athleteQueryKeys.goals())).toBeUndefined();
  });
});
