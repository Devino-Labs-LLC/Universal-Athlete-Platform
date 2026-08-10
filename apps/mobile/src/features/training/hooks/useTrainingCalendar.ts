import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { DateOnly } from '@/src/core/date/dateOnly';
import { fetchTrainingCalendar } from '@/src/features/training/api/calendarApi';
import { CalendarQueryFilters, trainingKeys } from '@/src/features/training/models/queryKeys';

export function useTrainingCalendar(
  scheduledFrom: DateOnly,
  scheduledTo: DateOnly,
  filters?: CalendarQueryFilters,
) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: trainingKeys.calendar(scheduledFrom, scheduledTo, filters),
    queryFn: () => fetchTrainingCalendar(apiClient, scheduledFrom, scheduledTo, filters),
    enabled: status === 'AUTHENTICATED' && Boolean(scheduledFrom) && Boolean(scheduledTo),
    staleTime: 30_000,
    retry: 1,
  });
}
