import {
  connectionListSchema,
  connectionViewSchema,
  syncRunViewSchema,
  type ConnectionView,
  type HealthProviderKey,
  type SyncRunView,
} from './connection';

export const CONNECTIONS_PATH = '/api/v1/integrations/connections';

/** Minimal JSON HTTP surface so web/mobile ApiClients stay out of this package. */
export type ConnectionsHttpClient = {
  getJson: (path: string) => Promise<unknown>;
  postJson: (path: string, body?: unknown) => Promise<unknown>;
};

/** Axios-shaped client both web and mobile ApiClient expose. */
export type AxiosLike = {
  get: (path: string) => Promise<{ data: unknown }>;
  post: (path: string, body?: unknown) => Promise<{ data: unknown }>;
};

export function connectionsHttpFromAxios(axios: AxiosLike): ConnectionsHttpClient {
  return {
    getJson: async (path) => {
      const response = await axios.get(path);
      return response.data;
    },
    postJson: async (path, body) => {
      const response =
        body === undefined ? await axios.post(path) : await axios.post(path, body);
      return response.data;
    },
  };
}

export async function listConnections(http: ConnectionsHttpClient): Promise<ConnectionView[]> {
  const data = await http.getJson(CONNECTIONS_PATH);
  return connectionListSchema.parse(data);
}

export async function beginConnect(
  http: ConnectionsHttpClient,
  input: { requestId: string; provider: HealthProviderKey },
): Promise<ConnectionView> {
  const data = await http.postJson(CONNECTIONS_PATH, {
    requestId: input.requestId,
    provider: input.provider,
  });
  return connectionViewSchema.parse(data);
}

export async function confirmConnection(
  http: ConnectionsHttpClient,
  connectionId: string,
): Promise<ConnectionView> {
  const data = await http.postJson(`${CONNECTIONS_PATH}/${connectionId}/confirm`);
  return connectionViewSchema.parse(data);
}

export async function disconnectConnection(
  http: ConnectionsHttpClient,
  connectionId: string,
  requestId: string,
): Promise<ConnectionView> {
  const data = await http.postJson(`${CONNECTIONS_PATH}/${connectionId}/disconnect`, {
    requestId,
  });
  return connectionViewSchema.parse(data);
}

export async function requestConnectionSync(
  http: ConnectionsHttpClient,
  connectionId: string,
  requestId: string,
): Promise<SyncRunView> {
  const data = await http.postJson(`${CONNECTIONS_PATH}/${connectionId}/sync`, {
    requestId,
  });
  return syncRunViewSchema.parse(data);
}

/** Axios convenience wrappers used by web/mobile ApiClient adapters. */
export function listConnectionsWithAxios(axios: AxiosLike): Promise<ConnectionView[]> {
  return listConnections(connectionsHttpFromAxios(axios));
}

export function beginConnectWithAxios(
  axios: AxiosLike,
  input: { requestId: string; provider: HealthProviderKey },
): Promise<ConnectionView> {
  return beginConnect(connectionsHttpFromAxios(axios), input);
}

export function confirmConnectionWithAxios(
  axios: AxiosLike,
  connectionId: string,
): Promise<ConnectionView> {
  return confirmConnection(connectionsHttpFromAxios(axios), connectionId);
}

export function disconnectConnectionWithAxios(
  axios: AxiosLike,
  connectionId: string,
  requestId: string,
): Promise<ConnectionView> {
  return disconnectConnection(connectionsHttpFromAxios(axios), connectionId, requestId);
}

export function requestConnectionSyncWithAxios(
  axios: AxiosLike,
  connectionId: string,
  requestId: string,
): Promise<SyncRunView> {
  return requestConnectionSync(connectionsHttpFromAxios(axios), connectionId, requestId);
}

/** Any client that exposes an Axios-like instance (web/mobile ApiClient). */
export type AxiosClientHolder = { axios: AxiosLike };

/**
 * Bind connections API methods to clients that expose `.axios`.
 * Platform modules re-export these to stay thin and structurally distinct.
 */
export function bindApiClientConnections<TClient extends AxiosClientHolder>() {
  return {
    listConnections: (client: TClient): Promise<ConnectionView[]> =>
      listConnectionsWithAxios(client.axios),
    beginConnect: (
      client: TClient,
      input: { requestId: string; provider: HealthProviderKey },
    ): Promise<ConnectionView> => beginConnectWithAxios(client.axios, input),
    confirmConnection: (client: TClient, connectionId: string): Promise<ConnectionView> =>
      confirmConnectionWithAxios(client.axios, connectionId),
    disconnectConnection: (
      client: TClient,
      connectionId: string,
      requestId: string,
    ): Promise<ConnectionView> =>
      disconnectConnectionWithAxios(client.axios, connectionId, requestId),
    requestConnectionSync: (
      client: TClient,
      connectionId: string,
      requestId: string,
    ): Promise<SyncRunView> =>
      requestConnectionSyncWithAxios(client.axios, connectionId, requestId),
  };
}
