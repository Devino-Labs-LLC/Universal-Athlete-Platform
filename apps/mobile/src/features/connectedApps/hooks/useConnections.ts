import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import {
  beginConnect,
  confirmConnection,
  disconnectConnection,
  listConnections,
  requestConnectionSync,
} from '@/src/features/connectedApps/api/connectionsApi';
import type { HealthProviderKey } from '@/src/features/connectedApps/models/connection';
import { stopQueueForConnection } from '@/src/features/connectedApps/queue/evidenceUploadQueue';
import { runAppleHealthConnectFlow } from '@/src/features/connectedApps/services/appleHealthConnectFlow';
import { runHealthConnectConnectFlow } from '@/src/features/connectedApps/services/healthConnectConnectFlow';
import {
  connectionsInvalidationTarget,
  connectionsListQueryBase,
} from '@uap/connected-apps-contracts';

export function useConnectionsList() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    ...connectionsListQueryBase(status === 'AUTHENTICATED'),
    queryFn: () => listConnections(apiClient),
  });
}

function useRefreshConnections() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries(connectionsInvalidationTarget());
  };
}

export function useBeginConnectMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: (input: { requestId: string; provider: HealthProviderKey }) =>
      beginConnect(apiClient, input),
    onSuccess: refresh,
  });
}

export function useConfirmConnectionMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: (connectionId: string) => confirmConnection(apiClient, connectionId),
    onSuccess: refresh,
  });
}

export function useDisconnectConnectionMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: async (input: { connectionId: string; requestId: string }) => {
      await stopQueueForConnection(input.connectionId);
      return disconnectConnection(apiClient, input.connectionId, input.requestId);
    },
    onSuccess: refresh,
  });
}

export function useRequestConnectionSyncMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: (input: { connectionId: string; requestId: string }) =>
      requestConnectionSync(apiClient, input.connectionId, input.requestId),
    onSuccess: refresh,
  });
}

export function useAppleHealthConnectMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: () => runAppleHealthConnectFlow(apiClient),
    onSuccess: refresh,
  });
}

export function useHealthConnectConnectMutation() {
  const { apiClient } = useAuthSession();
  const refresh = useRefreshConnections();
  return useMutation({
    mutationFn: () => runHealthConnectConnectFlow(apiClient),
    onSuccess: refresh,
  });
}
