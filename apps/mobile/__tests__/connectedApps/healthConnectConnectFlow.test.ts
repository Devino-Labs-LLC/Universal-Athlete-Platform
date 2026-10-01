import {
  beginConnect,
  confirmConnection,
  requestConnectionSync,
} from '@/src/features/connectedApps/api/connectionsApi';
import { runHealthConnectConnectFlow } from '@/src/features/connectedApps/services/healthConnectConnectFlow';
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
  provider: 'HEALTH_CONNECT' as const,
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

describe('runHealthConnectConnectFlow', () => {
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
    const healthConnect = {
      checkAvailability: jest.fn().mockResolvedValue({ status: 'available' }),
      requestAuthorization: jest.fn().mockResolvedValue({
        granted: true,
        partial: false,
        grantedRecordTypes: [
          'SleepSession',
          'RestingHeartRate',
          'HeartRateVariabilityRmssd',
          'Steps',
          'ActiveCaloriesBurned',
          'ExerciseSession',
        ],
        deniedRecordTypes: [],
        dialogCompleted: true,
      }),
      readSamplesForLastNDays: jest.fn().mockResolvedValue({
        items: [
          {
            externalRecordId: 'hc-1',
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
      openHealthConnectInstallOrSettings: jest.fn(),
    };

    const outcome = await runHealthConnectConnectFlow({ axios: {} } as never, {
      healthConnect,
    });

    expect(beginConnect).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ provider: 'HEALTH_CONNECT' }),
    );
    expect(healthConnect.requestAuthorization).toHaveBeenCalled();
    expect(confirmConnection).toHaveBeenCalledWith(expect.anything(), pending.connectionId);
    expect(enqueueEvidenceBatch).toHaveBeenCalled();
    expect(drainEvidenceUploadQueue).toHaveBeenCalled();
    expect(requestConnectionSync).toHaveBeenCalled();
    expect(outcome.status).toBe('connected');
    expect(outcome.evidenceUploaded).toBe(1);
    expect(outcome.syncRun?.errorCode).toBe('OS_HUB_UPLOAD_ONLY');
    expect(outcome.message).toMatch(/upload-only/i);
  });

  it('stops before confirm when OS permission is denied', async () => {
    const healthConnect = {
      checkAvailability: jest.fn().mockResolvedValue({ status: 'available' }),
      requestAuthorization: jest.fn().mockResolvedValue({
        granted: false,
        partial: false,
        grantedRecordTypes: [],
        deniedRecordTypes: ['SleepSession'],
        dialogCompleted: true,
        message: 'denied',
      }),
      readSamplesForLastNDays: jest.fn(),
      openHealthConnectInstallOrSettings: jest.fn(),
    };

    const outcome = await runHealthConnectConnectFlow({ axios: {} } as never, {
      healthConnect,
    });

    expect(confirmConnection).not.toHaveBeenCalled();
    expect(enqueueEvidenceBatch).not.toHaveBeenCalled();
    expect(outcome.status).toBe('permission_denied');
    expect(outcome.connection?.lifecycleState).toBe('PENDING');
  });

  it('reports unavailable and offers install when HC SDK is missing', async () => {
    const healthConnect = {
      checkAvailability: jest.fn().mockResolvedValue({
        status: 'unavailable',
        reason: 'sdk_unavailable',
        message: 'Health Connect is not installed on this device.',
      }),
      requestAuthorization: jest.fn(),
      readSamplesForLastNDays: jest.fn(),
      openHealthConnectInstallOrSettings: jest.fn(),
    };

    const outcome = await runHealthConnectConnectFlow({ axios: {} } as never, {
      healthConnect,
    });

    expect(outcome.status).toBe('unavailable');
    expect(beginConnect).not.toHaveBeenCalled();
    expect(healthConnect.openHealthConnectInstallOrSettings).toHaveBeenCalledWith(
      'sdk_unavailable',
    );
  });

  it('marks partial permissions when some types are denied but connect succeeds', async () => {
    const healthConnect = {
      checkAvailability: jest.fn().mockResolvedValue({ status: 'available' }),
      requestAuthorization: jest.fn().mockResolvedValue({
        granted: true,
        partial: true,
        grantedRecordTypes: ['SleepSession', 'Steps'],
        deniedRecordTypes: ['RestingHeartRate', 'HeartRateVariabilityRmssd'],
        dialogCompleted: true,
        message: 'Some Health Connect types were not granted.',
      }),
      readSamplesForLastNDays: jest.fn().mockResolvedValue({
        items: [
          {
            externalRecordId: 'hc-sleep',
            signalFamily: 'SLEEP',
            signalType: 'DURATION',
            valueNumeric: 360,
            unitCode: 'MINUTE',
            observedAt: '2026-09-29T06:00:00.000Z',
          },
        ],
        rawCount: 1,
        partial: true,
        deniedOrEmptyFamilies: ['HEART', 'HRV'],
      }),
      openHealthConnectInstallOrSettings: jest.fn(),
    };

    const outcome = await runHealthConnectConnectFlow({ axios: {} } as never, {
      healthConnect,
    });

    expect(outcome.status).toBe('connected');
    expect(outcome.partialPermissions).toBe(true);
    expect(healthConnect.readSamplesForLastNDays).toHaveBeenCalledWith(
      expect.any(Number),
      ['SleepSession', 'Steps'],
    );
  });
});
