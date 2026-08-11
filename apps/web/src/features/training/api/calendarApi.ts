import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import type { CalendarQueryFilters } from '@/features/training/models/queryKeys';
import { type CalendarEntry, calendarEntriesSchema } from '@/features/training/models/schemas';

const CALENDAR_PATH = '/api/v1/training/calendar';

export async function fetchTrainingCalendar(
  client: ApiClient,
  scheduledFrom: DateOnly,
  scheduledTo: DateOnly,
  filters?: CalendarQueryFilters,
): Promise<CalendarEntry[]> {
  const response = await client.axios.get(CALENDAR_PATH, {
    params: {
      scheduledFrom,
      scheduledTo,
      status: filters?.status,
      trainingPlanId: filters?.trainingPlanId,
    },
  });
  return calendarEntriesSchema.parse(response.data);
}
