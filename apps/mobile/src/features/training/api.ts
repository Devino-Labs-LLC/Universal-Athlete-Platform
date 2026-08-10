import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  TrainingClientBootstrap,
  trainingClientBootstrapSchema,
  TrainingTodayDashboard,
  trainingTodayDashboardSchema,
} from '@/src/features/training/schemas';

const BOOTSTRAP_PATH = '/api/v1/training/client/bootstrap';
const TODAY_PATH = '/api/v1/training/client/today';

export async function fetchTrainingBootstrap(
  client: ApiClient,
): Promise<TrainingClientBootstrap> {
  const response = await client.axios.get(BOOTSTRAP_PATH);
  return trainingClientBootstrapSchema.parse(response.data);
}

export async function fetchTrainingToday(
  client: ApiClient,
  date?: DateOnly,
): Promise<TrainingTodayDashboard> {
  const response = await client.axios.get(TODAY_PATH, {
    params: date ? { date } : undefined,
  });
  return trainingTodayDashboardSchema.parse(response.data);
}

export const EXPECTED_CLIENT_CONTRACT_VERSION = 'V1';
