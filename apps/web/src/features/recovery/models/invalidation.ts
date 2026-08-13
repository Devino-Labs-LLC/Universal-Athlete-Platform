import type { QueryClient } from '@tanstack/react-query';

import { trainingClientKeys } from '@/features/home/queryKeys';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

export function invalidateRecoveryCheckInQueries(queryClient: QueryClient, checkInId?: string): void {
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.checkIns() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.histories() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.overviews() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.analytics() });
  if (checkInId) {
    void queryClient.invalidateQueries({ queryKey: recoveryKeys.checkIn(checkInId) });
    void queryClient.invalidateQueries({ queryKey: recoveryKeys.checkInRevisions(checkInId) });
  }
}

/** Recovery + Home/Today only. Athlete-state/readiness/guidance stay explicitly generated. */
export function invalidateAfterCheckInMutation(queryClient: QueryClient, checkInId?: string): void {
  invalidateRecoveryCheckInQueries(queryClient, checkInId);
  void queryClient.invalidateQueries({ queryKey: trainingClientKeys.all });
}

export function invalidateAthleteStateQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.athleteStates() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.readinesses() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.recommendations() });
}
