import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  beginConnect,
  confirmConnection,
  disconnectConnection,
  disconnectDevice,
  listConnections,
  listDeviceQualityNotices,
  listDevices,
  requestConnectionSync,
} from '@/features/connectedApps/api/connectionsApi';
import type { HealthProviderKey } from '@/features/connectedApps/models/connection';
import {
  connectionsInvalidationTarget,
  connectionsListQueryBase,
  devicesListQueryBase,
  qualityNoticesQueryBase,
  connectedAppsQueryKeys,
} from '@uap/connected-apps-contracts';

export function useConnectionsList() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    ...connectionsListQueryBase(status === 'AUTHENTICATED'),
    queryFn: () => listConnections(apiClient),
  });
}

function useInvalidateConnections() {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries(connectionsInvalidationTarget());
    void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.devices() });
    void queryClient.invalidateQueries({ queryKey: connectedAppsQueryKeys.quality() });
  };
}

export function useBeginConnectMutation() {
  const { apiClient } = useAuthSession();
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: (input: { requestId: string; provider: HealthProviderKey }) =>
      beginConnect(apiClient, input),
    onSuccess: invalidate,
  });
}

export function useConfirmConnectionMutation() {
  const { apiClient } = useAuthSession();
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: (connectionId: string) => confirmConnection(apiClient, connectionId),
    onSuccess: invalidate,
  });
}

export function useDisconnectConnectionMutation() {
  const { apiClient } = useAuthSession();
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: (input: { connectionId: string; requestId: string }) =>
      disconnectConnection(apiClient, input.connectionId, input.requestId),
    onSuccess: invalidate,
  });
}

export function useDevicesList() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    ...devicesListQueryBase(status === 'AUTHENTICATED'),
    queryFn: () => listDevices(apiClient),
  });
}

export function useDeviceQualityNotices() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    ...qualityNoticesQueryBase(status === 'AUTHENTICATED'),
    queryFn: () => listDeviceQualityNotices(apiClient),
  });
}

export function useDisconnectDeviceMutation() {
  const { apiClient } = useAuthSession();
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: (deviceId: string) => disconnectDevice(apiClient, deviceId),
    onSuccess: invalidate,
  });
}

export function useRequestConnectionSyncMutation() {
  const { apiClient } = useAuthSession();
  const invalidate = useInvalidateConnections();
  return useMutation({
    mutationFn: (input: { connectionId: string; requestId: string }) =>
      requestConnectionSync(apiClient, input.connectionId, input.requestId),
    onSuccess: invalidate,
  });
}
