import {
  beginConnect,
  confirmConnection,
  disconnectConnection,
  listConnections,
  requestConnectionSync,
} from '@/src/features/connectedApps/api/connectionsApi';
import { uploadEvidenceBatch } from '@/src/features/connectedApps/api/evidenceBatchesApi';

const sampleConnection = {
  connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
  provider: 'APPLE_HEALTHKIT' as const,
  lifecycleState: 'CONNECTED' as const,
  processConsentGranted: true,
  processConsentGrantedAt: '2026-09-30T12:00:00Z',
  connectedAt: '2026-09-30T12:00:00Z',
  disconnectedAt: null,
  lastSuccessfulSyncAt: '2026-09-30T13:00:00Z',
  lastAttemptedSyncAt: '2026-09-30T13:00:00Z',
};

describe('connectedApps connectionsApi', () => {
  it('lists GET /api/v1/integrations/connections', async () => {
    const get = jest.fn().mockResolvedValue({ data: [sampleConnection] });
    const client = { axios: { get } };

    const connections = await listConnections(client as never);

    expect(get).toHaveBeenCalledWith('/api/v1/integrations/connections');
    expect(connections).toHaveLength(1);
    expect(connections[0]?.provider).toBe('APPLE_HEALTHKIT');
  });

  it('posts begin connect, confirm, disconnect, and sync', async () => {
    const post = jest
      .fn()
      .mockResolvedValueOnce({ data: { ...sampleConnection, lifecycleState: 'PENDING' } })
      .mockResolvedValueOnce({ data: sampleConnection })
      .mockResolvedValueOnce({ data: { ...sampleConnection, lifecycleState: 'DISCONNECTED' } })
      .mockResolvedValueOnce({
        data: {
          syncRunId: '99999999-aaaa-4bbb-8ccc-dddddddddddd',
          connectionId: sampleConnection.connectionId,
          status: 'REQUESTED',
          requestedAt: '2026-09-30T14:00:00Z',
          startedAt: null,
          finishedAt: null,
          errorCode: null,
          recordsAccepted: 0,
          recordsRejected: 0,
        },
      });
    const client = { axios: { post } };
    const requestId = '11111111-2222-4333-8444-555555555555';

    await beginConnect(client as never, { requestId, provider: 'HEALTH_CONNECT' });
    await confirmConnection(client as never, sampleConnection.connectionId);
    await disconnectConnection(client as never, sampleConnection.connectionId, requestId);
    await requestConnectionSync(client as never, sampleConnection.connectionId, requestId);

    expect(post).toHaveBeenNthCalledWith(1, '/api/v1/integrations/connections', {
      requestId,
      provider: 'HEALTH_CONNECT',
    });
    expect(post).toHaveBeenNthCalledWith(
      2,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/confirm`,
    );
    expect(post).toHaveBeenNthCalledWith(
      3,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/disconnect`,
      { requestId },
    );
    expect(post).toHaveBeenNthCalledWith(
      4,
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/sync`,
      { requestId },
    );
  });

  it('posts evidence-batches', async () => {
    const post = jest.fn().mockResolvedValue({
      data: {
        requestId: '11111111-2222-4333-8444-555555555555',
        syncRunId: '11111111-2222-4333-8444-555555555555',
        acceptedCount: 1,
        rejectedCount: 0,
        replayed: false,
      },
    });
    const client = { axios: { post } };
    const requestId = '11111111-2222-4333-8444-555555555555';

    await uploadEvidenceBatch(client as never, sampleConnection.connectionId, {
      requestId,
      items: [
        {
          externalRecordId: 'hk-1',
          signalFamily: 'SLEEP',
          signalType: 'DURATION',
          valueNumeric: 420,
          unitCode: 'MINUTE',
          observedAt: '2026-09-29T06:00:00.000Z',
          provenanceClass: 'CLIENT_DEVICE',
        },
      ],
    });

    expect(post).toHaveBeenCalledWith(
      `/api/v1/integrations/connections/${sampleConnection.connectionId}/evidence-batches`,
      expect.objectContaining({ requestId }),
    );
  });
});
