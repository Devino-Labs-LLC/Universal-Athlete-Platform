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
import { connectedAppsQueryKeys } from '@/src/features/connectedApps/models/queryKeys';

export function useConnectionsList() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: connectedAppsQueryKeys.connections(),
    enabled: status === 'AUTHENTICATED',
    retry: false,
    queryFn: () => listConnections(apiClient),
  });
}

export function useBeginConnectMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { requestId: string; provider: HealthProviderKey }) =>
      beginConnect(apiClient, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.connections() });
    },
  });
}

export function useConfirmConnectionMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (connectionId: string) => confirmConnection(apiClient, connectionId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.connections() });
    },
  });
}

export function useDisconnectConnectionMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { connectionId: string; requestId: string }) =>
      disconnectConnection(apiClient, input.connectionId, input.requestId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.connections() });
    },
  });
}

export function useRequestConnectionSyncMutation() {
  const { apiClient } = useAuthSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: { connectionId: string; requestId: string }) =>
      requestConnectionSync(apiClient, input.connectionId, input.requestId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.connections() });
    },
  });
}
