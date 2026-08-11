import { ApiClient } from '@/src/core/api/apiClient';
import {
  DailyReadinessAssessment,
  dailyReadinessAssessmentSchema,
} from '@/src/features/recovery/models/recoverySchemas';

const BASE_PATH = '/api/v1/training/readiness/assessments';

export async function fetchReadinessAssessment(
  client: ApiClient,
  assessmentId: string,
): Promise<DailyReadinessAssessment> {
  const response = await client.axios.get(`${BASE_PATH}/${assessmentId}`);
  return dailyReadinessAssessmentSchema.parse(response.data);
}
