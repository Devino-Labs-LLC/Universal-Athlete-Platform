import { ApiError } from '@/src/core/api/errors';
import {
  __resetEvidenceQueueStateForTests,
  __setEvidenceQueueStoreForTests,
  drainEvidenceUploadQueue,
  enqueueEvidenceBatch,
  listQueuedEvidenceBatches,
  stopQueueForConnection,
} from '@/src/features/connectedApps/queue/evidenceUploadQueue';

const memory = new Map<string, string>();

const memoryStore = {
  getItem: jest.fn(async (key: string) => memory.get(key) ?? null),
  setItem: jest.fn(async (key: string, value: string) => {
    memory.set(key, value);
  }),
  removeItem: jest.fn(async (key: string) => {
    memory.delete(key);
  }),
};

jest.mock('@react-native-community/netinfo', () => ({
  __esModule: true,
  default: {
    fetch: jest.fn(async () => ({ isConnected: true })),
    addEventListener: jest.fn(() => jest.fn()),
  },
}));

jest.mock('@/src/features/connectedApps/api/evidenceBatchesApi', () => ({
  uploadEvidenceBatch: jest.fn(),
}));

const { uploadEvidenceBatch } = jest.requireMock(
  '@/src/features/connectedApps/api/evidenceBatchesApi',
);
const NetInfo = jest.requireMock('@react-native-community/netinfo').default;

const sampleItem = {
  externalRecordId: 'hk-1',
  signalFamily: 'SLEEP' as const,
  signalType: 'DURATION',
  valueNumeric: 420,
  unitCode: 'MINUTE',
  observedAt: '2026-09-29T06:00:00.000Z',
  provenanceClass: 'CLIENT_DEVICE' as const,
};

describe('evidenceUploadQueue', () => {
  beforeEach(() => {
    memory.clear();
    jest.clearAllMocks();
    __resetEvidenceQueueStateForTests();
    __setEvidenceQueueStoreForTests(memoryStore);
    NetInfo.fetch.mockResolvedValue({ isConnected: true });
  });

  afterEach(() => {
    __setEvidenceQueueStoreForTests(null);
  });

  it('enqueues idempotently by requestId', async () => {
    const requestId = '11111111-2222-4333-8444-555555555555';
    await enqueueEvidenceBatch({
      requestId,
      connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      items: [sampleItem],
    });
    await enqueueEvidenceBatch({
      requestId,
      connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      items: [{ ...sampleItem, valueNumeric: 400 }],
    });

    const queue = await listQueuedEvidenceBatches();
    expect(queue).toHaveLength(1);
    expect(queue[0]?.requestId).toBe(requestId);
    expect(queue[0]?.items[0]?.valueNumeric).toBe(400);
  });

  it('drains when online and removes successful uploads', async () => {
    uploadEvidenceBatch.mockResolvedValue({
      requestId: '11111111-2222-4333-8444-555555555555',
      syncRunId: '11111111-2222-4333-8444-555555555555',
      acceptedCount: 1,
      rejectedCount: 0,
      replayed: false,
    });

    await enqueueEvidenceBatch({
      requestId: '11111111-2222-4333-8444-555555555555',
      connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      items: [sampleItem],
    });

    const result = await drainEvidenceUploadQueue({ axios: {} } as never);
    expect(result.uploaded).toBe(1);
    expect(result.remaining).toBe(0);
    expect(uploadEvidenceBatch).toHaveBeenCalledTimes(1);
    expect(await listQueuedEvidenceBatches()).toHaveLength(0);
  });

  it('keeps entries on network failure and skips drain when offline', async () => {
    uploadEvidenceBatch.mockRejectedValue(
      new ApiError('offline', { category: 'network' }),
    );
    await enqueueEvidenceBatch({
      requestId: '11111111-2222-4333-8444-555555555555',
      connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      items: [sampleItem],
    });

    const failed = await drainEvidenceUploadQueue({ axios: {} } as never);
    expect(failed.uploaded).toBe(0);
    expect(failed.remaining).toBe(1);
    expect((await listQueuedEvidenceBatches())[0]?.attempts).toBe(1);

    NetInfo.fetch.mockResolvedValue({ isConnected: false });
    const offline = await drainEvidenceUploadQueue({ axios: {} } as never);
    expect(offline.uploaded).toBe(0);
    expect(offline.remaining).toBe(1);
    expect(uploadEvidenceBatch).toHaveBeenCalledTimes(1);
  });

  it('stopQueueForConnection drops pending batches for that connection', async () => {
    await enqueueEvidenceBatch({
      requestId: '11111111-2222-4333-8444-555555555555',
      connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      items: [sampleItem],
    });
    await enqueueEvidenceBatch({
      requestId: '22222222-3333-4444-8555-666666666666',
      connectionId: 'bbbbbbbb-cccc-4ddd-8eee-ffffffffffff',
      items: [sampleItem],
    });

    await stopQueueForConnection('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee');
    const queue = await listQueuedEvidenceBatches();
    expect(queue).toHaveLength(1);
    expect(queue[0]?.connectionId).toBe('bbbbbbbb-cccc-4ddd-8eee-ffffffffffff');
  });
});
