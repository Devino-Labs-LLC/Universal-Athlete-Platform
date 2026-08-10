import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  CalendarEntry,
  calendarEntriesSchema,
} from '@/src/features/training/models/browseSchemas';
import { CalendarQueryFilters } from '@/src/features/training/models/queryKeys';

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
