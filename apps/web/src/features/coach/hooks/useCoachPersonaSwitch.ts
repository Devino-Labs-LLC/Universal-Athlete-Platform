import { useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { clearCoachQueriesOnPersonaSwitch } from '@/features/coach/models/invalidation';

/**
 * Navigate between athlete and coach personas without logout.
 * Clears coach-scoped cache so prior team projections cannot flash.
 */
export function useCoachPersonaSwitch() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { account } = useAuthSession();

  const goToCoachView = useCallback(() => {
    clearCoachQueriesOnPersonaSwitch(queryClient, account?.accountId);
    navigate('/coach');
  }, [account?.accountId, navigate, queryClient]);

  const goToAthleteView = useCallback(() => {
    clearCoachQueriesOnPersonaSwitch(queryClient, account?.accountId);
    navigate('/app/home');
  }, [account?.accountId, navigate, queryClient]);

  return { goToCoachView, goToAthleteView };
}
