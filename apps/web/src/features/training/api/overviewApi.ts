import type { ApiClient } from '@/core/api/apiClient';
import type { DateOnly } from '@/core/date/dateOnly';
import { type TrainingOverview, trainingOverviewSchema } from '@/features/training/models/schemas';

const OVERVIEW_PATH = '/api/v1/training/client/training-overview';

export async function fetchTrainingOverview(
  client: ApiClient,
  date?: DateOnly,
): Promise<TrainingOverview> {
  const response = await client.axios.get(OVERVIEW_PATH, {
    params: date ? { date } : undefined,
  });
  return trainingOverviewSchema.parse(response.data);
}
