import type { ApiClient } from '@/core/api/apiClient';
import {
  beginConnectWithAxios,
  confirmConnectionWithAxios,
  disconnectConnectionWithAxios,
  listConnectionsWithAxios,
  requestConnectionSyncWithAxios,
  type ConnectionView,
  type HealthProviderKey,
  type SyncRunView,
} from '@uap/connected-apps-contracts';

export const listConnections = (client: ApiClient): Promise<ConnectionView[]> =>
  listConnectionsWithAxios(client.axios);

export const beginConnect = (
  client: ApiClient,
  input: { requestId: string; provider: HealthProviderKey },
): Promise<ConnectionView> => beginConnectWithAxios(client.axios, input);

export const confirmConnection = (
  client: ApiClient,
  connectionId: string,
): Promise<ConnectionView> => confirmConnectionWithAxios(client.axios, connectionId);

export const disconnectConnection = (
  client: ApiClient,
  connectionId: string,
  requestId: string,
): Promise<ConnectionView> =>
  disconnectConnectionWithAxios(client.axios, connectionId, requestId);

export const requestConnectionSync = (
  client: ApiClient,
  connectionId: string,
  requestId: string,
): Promise<SyncRunView> =>
  requestConnectionSyncWithAxios(client.axios, connectionId, requestId);
