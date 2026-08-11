import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  AthleteRecoveryHistory,
  athleteRecoveryHistorySchema,
} from '@/src/features/recovery/models/recoverySchemas';

const BASE_PATH = '/api/v1/training/recovery-check-ins/history';

export async function fetchRecoveryHistory(
  client: ApiClient,
  startDate: DateOnly,
  endDate: DateOnly,
  includeTrainingLoad = true,
): Promise<AthleteRecoveryHistory> {
  const response = await client.axios.get(BASE_PATH, {
    params: { startDate, endDate, includeTrainingLoad },
  });
  return athleteRecoveryHistorySchema.parse(response.data);
}
