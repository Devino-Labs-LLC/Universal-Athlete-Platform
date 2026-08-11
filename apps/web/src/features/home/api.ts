import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  BOOTSTRAP_PATH,
  EXPECTED_CLIENT_CONTRACT_VERSION,
  TODAY_PATH,
  type TodayDashboard,
  todayDashboardSchema,
  type TrainingClientBootstrap,
  trainingClientBootstrapSchema,
} from '@/features/home/schemas';

export { EXPECTED_CLIENT_CONTRACT_VERSION };

export async function fetchTrainingBootstrap(
  client: ApiClient,
): Promise<TrainingClientBootstrap> {
  const response = await client.axios.get(BOOTSTRAP_PATH);
  return trainingClientBootstrapSchema.parse(response.data);
}

export async function fetchTodayDashboard(
  client: ApiClient,
  date?: DateOnly,
): Promise<TodayDashboard> {
  const response = await client.axios.get(TODAY_PATH, {
    params: date ? { date } : undefined,
  });
  return todayDashboardSchema.parse(response.data);
}
