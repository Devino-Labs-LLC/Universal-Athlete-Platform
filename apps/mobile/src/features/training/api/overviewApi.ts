import { ApiClient } from '@/src/core/api/apiClient';
import { DateOnly } from '@/src/core/date/dateOnly';
import {
  TrainingOverview,
  trainingOverviewSchema,
} from '@/src/features/training/models/browseSchemas';

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
