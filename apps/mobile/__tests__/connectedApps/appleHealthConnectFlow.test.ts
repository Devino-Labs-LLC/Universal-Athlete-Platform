import {
  beginConnect,
  confirmConnection,
  requestConnectionSync,
} from '@/src/features/connectedApps/api/connectionsApi';
import { runAppleHealthConnectFlow } from '@/src/features/connectedApps/services/appleHealthConnectFlow';
import {
  drainEvidenceUploadQueue,
  enqueueEvidenceBatch,
} from '@/src/features/connectedApps/queue/evidenceUploadQueue';

jest.mock('@/src/features/connectedApps/api/connectionsApi', () => ({
  beginConnect: jest.fn(),
  confirmConnection: jest.fn(),
  requestConnectionSync: jest.fn(),
}));

jest.mock('@/src/features/connectedApps/queue/evidenceUploadQueue', () => ({
  enqueueEvidenceBatch: jest.fn(),
  drainEvidenceUploadQueue: jest.fn(),
}));

const pending = {
  connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
  provider: 'APPLE_HEALTHKIT' as const,
  lifecycleState: 'PENDING' as const,
  processConsentGranted: false,
  processConsentGrantedAt: null,
  connectedAt: null,
  disconnectedAt: null,
  lastSuccessfulSyncAt: null,
  lastAttemptedSyncAt: null,
};

const connected = {
  ...pending,
  lifecycleState: 'CONNECTED' as const,
  processConsentGranted: true,
  processConsentGrantedAt: '2026-09-30T12:00:00Z',
  connectedAt: '2026-09-30T12:00:00Z',
};

describe('runAppleHealthConnectFlow', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (beginConnect as jest.Mock).mockResolvedValue(pending);
    (confirmConnection as jest.Mock).mockResolvedValue(connected);
    (requestConnectionSync as jest.Mock).mockResolvedValue({
      syncRunId: '99999999-aaaa-4bbb-8ccc-dddddddddddd',
      connectionId: connected.connectionId,
      status: 'FAILED',
      requestedAt: '2026-09-30T14:00:00Z',
      startedAt: '2026-09-30T14:00:00Z',
      finishedAt: '2026-09-30T14:00:01Z',
      errorCode: 'OS_HUB_UPLOAD_ONLY',
      recordsAccepted: 0,
      recordsRejected: 0,
    });
    (enqueueEvidenceBatch as jest.Mock).mockResolvedValue([]);
    (drainEvidenceUploadQueue as jest.Mock).mockResolvedValue({
      uploaded: 1,
      remaining: 0,
      skippedStopped: 0,
    });
  });

  it('runs begin → auth → confirm → enqueue → drain', async () => {
    const healthKit = {
      isAvailable: jest.fn().mockResolvedValue(true),
      requestAuthorization: jest.fn().mockResolvedValue({ granted: true, dialogCompleted: true }),
      readSamplesForLastNDays: jest.fn().mockResolvedValue({
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
        rawCount: 1,
        partial: false,
        deniedOrEmptyFamilies: [],
      }),
    };

    const outcome = await runAppleHealthConnectFlow({ axios: {} } as never, { healthKit });

    expect(beginConnect).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ provider: 'APPLE_HEALTHKIT' }),
    );
    expect(healthKit.requestAuthorization).toHaveBeenCalled();
    expect(confirmConnection).toHaveBeenCalledWith(expect.anything(), pending.connectionId);
    expect(enqueueEvidenceBatch).toHaveBeenCalled();
    expect(drainEvidenceUploadQueue).toHaveBeenCalled();
    expect(requestConnectionSync).toHaveBeenCalled();
    expect(outcome.status).toBe('connected');
    expect(outcome.evidenceUploaded).toBe(1);
    expect(outcome.syncRun?.errorCode).toBe('OS_HUB_UPLOAD_ONLY');
    expect(outcome.message).toMatch(/upload-only/i);
  });

  it('keeps connect success when optional sync transport fails', async () => {
    (requestConnectionSync as jest.Mock).mockRejectedValue(new Error('network'));
    const healthKit = {
      isAvailable: jest.fn().mockResolvedValue(true),
      requestAuthorization: jest.fn().mockResolvedValue({ granted: true, dialogCompleted: true }),
      readSamplesForLastNDays: jest.fn().mockResolvedValue({
        items: [],
        rawCount: 0,
        partial: true,
        deniedOrEmptyFamilies: ['SLEEP'],
        message: 'No samples in window.',
      }),
    };

    const outcome = await runAppleHealthConnectFlow({ axios: {} } as never, { healthKit });

    expect(outcome.status).toBe('connected');
    expect(outcome.syncRun).toBeNull();
    expect(outcome.message).toMatch(/Apple Health is connected/i);
  });

  it('stops before confirm when OS permission is denied', async () => {
    const healthKit = {
      isAvailable: jest.fn().mockResolvedValue(true),
      requestAuthorization: jest.fn().mockResolvedValue({
        granted: false,
        dialogCompleted: true,
        message: 'denied',
      }),
      readSamplesForLastNDays: jest.fn(),
    };

    const outcome = await runAppleHealthConnectFlow({ axios: {} } as never, { healthKit });

    expect(confirmConnection).not.toHaveBeenCalled();
    expect(enqueueEvidenceBatch).not.toHaveBeenCalled();
    expect(outcome.status).toBe('permission_denied');
    expect(outcome.connection?.lifecycleState).toBe('PENDING');
  });

  it('reports unavailable when HealthKit is missing', async () => {
    const outcome = await runAppleHealthConnectFlow({ axios: {} } as never, {
      healthKit: {
        isAvailable: jest.fn().mockResolvedValue(false),
        requestAuthorization: jest.fn(),
        readSamplesForLastNDays: jest.fn(),
      },
    });
    expect(outcome.status).toBe('unavailable');
    expect(beginConnect).not.toHaveBeenCalled();
  });
});
