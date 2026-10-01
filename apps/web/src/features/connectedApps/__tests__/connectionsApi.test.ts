import { afterEach, describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import {
  beginConnect,
  confirmConnection,
  disconnectConnection,
  listConnections,
  requestConnectionSync,
} from '@/features/connectedApps/api/connectionsApi';

function clientWith(axios: { post?: unknown; get?: unknown }): ApiClient {
  return { axios } as ApiClient;
}

const sampleConnection = {
  connectionId: '11111111-2222-4333-8444-555555555555',
  provider: 'APPLE_HEALTHKIT' as const,
  lifecycleState: 'CONNECTED' as const,
  processConsentGranted: true,
  processConsentGrantedAt: '2026-09-01T12:00:00Z',
  connectedAt: '2026-09-01T12:00:00Z',
  disconnectedAt: null,
  lastSuccessfulSyncAt: '2026-09-30T08:00:00Z',
  lastAttemptedSyncAt: '2026-09-30T08:00:00Z',
};

describe('connectionsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('lists connections from the integrations API', async () => {
    const get = vi.fn().mockResolvedValue({ data: [sampleConnection] });
    const result = await listConnections(clientWith({ get }));
    expect(get).toHaveBeenCalledWith('/api/v1/integrations/connections');
    expect(result).toHaveLength(1);
    expect(result[0]?.provider).toBe('APPLE_HEALTHKIT');
  });

  it('begins connect with provider and requestId', async () => {
    const post = vi.fn().mockResolvedValue({
      data: { ...sampleConnection, lifecycleState: 'PENDING' },
    });
    const result = await beginConnect(clientWith({ post }), {
      requestId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      provider: 'HEALTH_CONNECT',
    });
    expect(post).toHaveBeenCalledWith('/api/v1/integrations/connections', {
      requestId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      provider: 'HEALTH_CONNECT',
    });
    expect(result.lifecycleState).toBe('PENDING');
  });

  it('confirms, disconnects, and syncs by connection id', async () => {
    const post = vi
      .fn()
      .mockResolvedValueOnce({ data: sampleConnection })
      .mockResolvedValueOnce({
        data: { ...sampleConnection, lifecycleState: 'DISCONNECTED' },
      })
      .mockResolvedValueOnce({
        data: {
          syncRunId: '99999999-8888-4777-a666-555555555555',
          connectionId: sampleConnection.connectionId,
          status: 'FAILED',
          requestedAt: '2026-09-30T09:00:00Z',
          startedAt: null,
          finishedAt: '2026-09-30T09:00:01Z',
          errorCode: 'INTEGRATION_SYNC_ADAPTER_UNAVAILABLE',
          recordsAccepted: 0,
          recordsRejected: 0,
        },
      });

    await confirmConnection(clientWith({ post }), sampleConnection.connectionId);
    await disconnectConnection(
      clientWith({ post }),
      sampleConnection.connectionId,
      'bbbbbbbb-cccc-4ddd-aeee-ffffffffffff',
    );
    await requestConnectionSync(
      clientWith({ post }),
      sampleConnection.connectionId,
      'cccccccc-dddd-4eee-8fff-000000000000',
    );

    expect(post).toHaveBeenNthCalledWith(
      1,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/confirm`,
    );
    expect(post).toHaveBeenNthCalledWith(
      2,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/disconnect`,
      { requestId: 'bbbbbbbb-cccc-4ddd-aeee-ffffffffffff' },
    );
    expect(post).toHaveBeenNthCalledWith(
      3,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/sync`,
      { requestId: 'cccccccc-dddd-4eee-8fff-000000000000' },
    );
  });
});
