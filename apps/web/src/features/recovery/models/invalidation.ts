import type { QueryClient } from '@tanstack/react-query';

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

export function invalidateAthleteStateQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.athleteStates() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.readinesses() });
  void queryClient.invalidateQueries({ queryKey: recoveryKeys.recommendations() });
}
