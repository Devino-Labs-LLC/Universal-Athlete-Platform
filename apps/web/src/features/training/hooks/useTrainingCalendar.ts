import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import { fetchTrainingCalendar } from '@/features/training/api/calendarApi';
import type { CalendarQueryFilters } from '@/features/training/models/queryKeys';
import { trainingKeys } from '@/features/training/models/queryKeys';

export function useTrainingCalendar(
  from: DateOnly,
  to: DateOnly,
  filters?: CalendarQueryFilters,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: trainingKeys.calendar(from, to, filters),
    queryFn: () => fetchTrainingCalendar(apiClient, from, to, filters),
    enabled: status === 'AUTHENTICATED',
  });
}
