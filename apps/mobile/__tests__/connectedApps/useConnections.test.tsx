import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react-native';
import type { PropsWithChildren } from 'react';

import {
  useAppleHealthConnectMutation,
  useBeginConnectMutation,
  useConnectionsList,
  useDisconnectConnectionMutation,
  useHealthConnectConnectMutation,
  useRequestConnectionSyncMutation,
} from '@/src/features/connectedApps/hooks/useConnections';
import { connectedAppsQueryKeys } from '@/src/features/connectedApps/models/queryKeys';

const mockListConnections = jest.fn();
const mockBeginConnect = jest.fn();
const mockDisconnectConnection = jest.fn();
const mockRequestConnectionSync = jest.fn();
const mockStopQueueForConnection = jest.fn();
const mockRunAppleHealthConnectFlow = jest.fn();
const mockRunHealthConnectConnectFlow = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

jest.mock('@/src/features/connectedApps/api/connectionsApi', () => ({
  listConnections: (...args: unknown[]) => mockListConnections(...args),
  beginConnect: (...args: unknown[]) => mockBeginConnect(...args),
  confirmConnection: jest.fn(),
  disconnectConnection: (...args: unknown[]) => mockDisconnectConnection(...args),
  requestConnectionSync: (...args: unknown[]) => mockRequestConnectionSync(...args),
}));

jest.mock('@/src/features/connectedApps/queue/evidenceUploadQueue', () => ({
  stopQueueForConnection: (...args: unknown[]) => mockStopQueueForConnection(...args),
}));

jest.mock('@/src/features/connectedApps/services/appleHealthConnectFlow', () => ({
  runAppleHealthConnectFlow: (...args: unknown[]) => mockRunAppleHealthConnectFlow(...args),
}));

jest.mock('@/src/features/connectedApps/services/healthConnectConnectFlow', () => ({
  runHealthConnectConnectFlow: (...args: unknown[]) => mockRunHealthConnectConnectFlow(...args),
}));

const sampleConnection = {
  connectionId: '11111111-2222-4333-8444-555555555555',
  provider: 'APPLE_HEALTHKIT' as const,
  lifecycleState: 'CONNECTED' as const,
  processConsentGranted: true,
  processConsentGrantedAt: '2026-09-01T12:00:00Z',
  connectedAt: '2026-09-01T12:00:00Z',
  disconnectedAt: null,
  lastSuccessfulSyncAt: null,
  lastAttemptedSyncAt: null,
};

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }
  return { Wrapper, queryClient };
}

describe('useConnections hooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockListConnections.mockResolvedValue([sampleConnection]);
    mockBeginConnect.mockResolvedValue({ ...sampleConnection, lifecycleState: 'PENDING' });
    mockDisconnectConnection.mockResolvedValue({
      ...sampleConnection,
      lifecycleState: 'DISCONNECTED',
    });
    mockRequestConnectionSync.mockResolvedValue({
      syncRunId: '99999999-8888-4777-a666-555555555555',
      connectionId: sampleConnection.connectionId,
      status: 'FAILED',
      requestedAt: '2026-09-30T09:00:00Z',
      startedAt: null,
      finishedAt: '2026-09-30T09:00:01Z',
      errorCode: 'OS_HUB_UPLOAD_ONLY',
      recordsAccepted: 0,
      recordsRejected: 0,
    });
    mockStopQueueForConnection.mockResolvedValue(undefined);
    mockRunAppleHealthConnectFlow.mockResolvedValue(sampleConnection);
    mockRunHealthConnectConnectFlow.mockResolvedValue({
      ...sampleConnection,
      provider: 'HEALTH_CONNECT',
    });
  });

  it('loads connections when authenticated', async () => {
    const { Wrapper, queryClient } = createWrapper();
    const { result } = await renderHook(() => useConnectionsList(), { wrapper: Wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockListConnections).toHaveBeenCalled();
    expect(queryClient.getQueryData(connectedAppsQueryKeys.connections())).toEqual([
      sampleConnection,
    ]);
  });

  it('begin connect invalidates the connections query', async () => {
    const { Wrapper, queryClient } = createWrapper();
    const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');
    const { result } = await renderHook(() => useBeginConnectMutation(), { wrapper: Wrapper });

    await act(async () => {
      await result.current.mutateAsync({
        requestId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
        provider: 'HEALTH_CONNECT',
      });
    });

    expect(mockBeginConnect).toHaveBeenCalled();
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: connectedAppsQueryKeys.connections(),
      });
    });
  });

  it('disconnect stops the evidence queue before API call', async () => {
    const { Wrapper } = createWrapper();
    const { result } = await renderHook(() => useDisconnectConnectionMutation(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await result.current.mutateAsync({
        connectionId: sampleConnection.connectionId,
        requestId: 'bbbbbbbb-cccc-4ddd-aeee-ffffffffffff',
      });
    });

    expect(mockStopQueueForConnection).toHaveBeenCalledWith(sampleConnection.connectionId);
    expect(mockDisconnectConnection).toHaveBeenCalled();
  });

  it('request sync and OS hub connect mutations run their flows', async () => {
    const { Wrapper } = createWrapper();
    const { result: syncResult } = await renderHook(() => useRequestConnectionSyncMutation(), {
      wrapper: Wrapper,
    });
    const { result: appleResult } = await renderHook(() => useAppleHealthConnectMutation(), {
      wrapper: Wrapper,
    });
    const { result: hcResult } = await renderHook(() => useHealthConnectConnectMutation(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await syncResult.current.mutateAsync({
        connectionId: sampleConnection.connectionId,
        requestId: 'cccccccc-dddd-4eee-8fff-000000000000',
      });
    });
    await act(async () => {
      await appleResult.current.mutateAsync();
    });
    await act(async () => {
      await hcResult.current.mutateAsync();
    });

    expect(mockRequestConnectionSync).toHaveBeenCalled();
    expect(mockRunAppleHealthConnectFlow).toHaveBeenCalled();
    expect(mockRunHealthConnectConnectFlow).toHaveBeenCalled();
  });
});
