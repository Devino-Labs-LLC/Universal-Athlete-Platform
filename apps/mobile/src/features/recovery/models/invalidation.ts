import { QueryClient } from '@tanstack/react-query';

import { DateOnly } from '@/src/core/date/dateOnly';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';

export async function invalidateAfterCheckInMutation(
  queryClient: QueryClient,
  date: DateOnly,
  checkInId?: string,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: recoveryKeys.overviewPrefix() }),
    queryClient.invalidateQueries({ queryKey: recoveryKeys.checkInByDate(date) }),
    checkInId
      ? queryClient.invalidateQueries({ queryKey: recoveryKeys.checkIn(checkInId) })
      : Promise.resolve(),
    queryClient.invalidateQueries({ queryKey: recoveryKeys.all }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ]);
}

export async function invalidateAfterDerivedStateMutation(
  queryClient: QueryClient,
): Promise<void> {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
    queryClient.invalidateQueries({ queryKey: recoveryKeys.overviewPrefix() }),
  ]);
}
