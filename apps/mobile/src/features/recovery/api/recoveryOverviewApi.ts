import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  RecoveryOverview,
  recoveryOverviewSchema,
 TrendDays } from '@/src/features/recovery/models/recoverySchemas';

const OVERVIEW_PATH = '/api/v1/training/client/recovery-overview';

export async function fetchRecoveryOverview(
  client: ApiClient,
  date: DateOnly,
  trendDays: TrendDays = 7,
): Promise<RecoveryOverview> {
  const response = await client.axios.get(OVERVIEW_PATH, {
    params: { date, trendDays },
  });
  return recoveryOverviewSchema.parse(response.data);
}
