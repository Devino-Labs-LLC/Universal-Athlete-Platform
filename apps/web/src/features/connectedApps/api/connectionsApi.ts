import type { ApiClient } from '@/core/api/apiClient';

import {
  connectionListSchema,
  connectionViewSchema,
  syncRunViewSchema,
  type ConnectionView,
  type HealthProviderKey,
  type SyncRunView,
} from '@/features/connectedApps/models/connection';

const CONNECTIONS_PATH = '/api/v1/integrations/connections';

export async function listConnections(client: ApiClient): Promise<ConnectionView[]> {
  const response = await client.axios.get(CONNECTIONS_PATH);
  return connectionListSchema.parse(response.data);
}

export async function beginConnect(
  client: ApiClient,
  input: { requestId: string; provider: HealthProviderKey },
): Promise<ConnectionView> {
  const response = await client.axios.post(CONNECTIONS_PATH, {
    requestId: input.requestId,
    provider: input.provider,
  });
  return connectionViewSchema.parse(response.data);
}

export async function confirmConnection(
  client: ApiClient,
  connectionId: string,
): Promise<ConnectionView> {
  const response = await client.axios.post(`${CONNECTIONS_PATH}/${connectionId}/confirm`);
  return connectionViewSchema.parse(response.data);
}

export async function disconnectConnection(
  client: ApiClient,
  connectionId: string,
  requestId: string,
): Promise<ConnectionView> {
  const response = await client.axios.post(`${CONNECTIONS_PATH}/${connectionId}/disconnect`, {
    requestId,
  });
  return connectionViewSchema.parse(response.data);
}

export async function requestConnectionSync(
  client: ApiClient,
  connectionId: string,
  requestId: string,
): Promise<SyncRunView> {
  const response = await client.axios.post(`${CONNECTIONS_PATH}/${connectionId}/sync`, {
    requestId,
  });
  return syncRunViewSchema.parse(response.data);
}
