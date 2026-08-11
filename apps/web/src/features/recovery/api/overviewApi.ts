import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import { type RecoveryOverview, recoveryOverviewSchema, type TrendDays } from '@/features/recovery/models/schemas';

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
