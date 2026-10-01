import type { ApiClient } from '@/src/core/api/apiClient';
import {
  beginConnectWithAxios,
  confirmConnectionWithAxios,
  disconnectConnectionWithAxios,
  listConnectionsWithAxios,
  requestConnectionSyncWithAxios,
  type HealthProviderKey,
} from '@uap/connected-apps-contracts';

/** Mobile keeps named functions (structurally distinct from web destructuring re-export). */
export const listConnections = (client: ApiClient) => listConnectionsWithAxios(client.axios);

export const beginConnect = (
  client: ApiClient,
  input: { requestId: string; provider: HealthProviderKey },
) => beginConnectWithAxios(client.axios, input);

export const confirmConnection = (client: ApiClient, connectionId: string) =>
  confirmConnectionWithAxios(client.axios, connectionId);

export const disconnectConnection = (
  client: ApiClient,
  connectionId: string,
  requestId: string,
) => disconnectConnectionWithAxios(client.axios, connectionId, requestId);

export const requestConnectionSync = (
  client: ApiClient,
  connectionId: string,
  requestId: string,
) => requestConnectionSyncWithAxios(client.axios, connectionId, requestId);
