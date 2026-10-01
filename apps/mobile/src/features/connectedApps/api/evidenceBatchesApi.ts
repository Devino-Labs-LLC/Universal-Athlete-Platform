import type { ApiClient } from '@/src/core/api/apiClient';

import {
  evidenceBatchResultSchema,
  type EvidenceBatchItem,
  type EvidenceBatchResult,
} from '@/src/features/connectedApps/models/evidenceBatch';

const CONNECTIONS_PATH = '/api/v1/integrations/connections';

export async function uploadEvidenceBatch(
  client: ApiClient,
  connectionId: string,
  input: { requestId: string; items: EvidenceBatchItem[] },
): Promise<EvidenceBatchResult> {
  const response = await client.axios.post(`${CONNECTIONS_PATH}/${connectionId}/evidence-batches`, {
    requestId: input.requestId,
    items: input.items,
  });
  return evidenceBatchResultSchema.parse(response.data);
}
