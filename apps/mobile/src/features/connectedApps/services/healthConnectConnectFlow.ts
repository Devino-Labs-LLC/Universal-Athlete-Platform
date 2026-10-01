import type { ApiClient } from '@/src/core/api/apiClient';
import {
  checkAvailability,
  openHealthConnectInstallOrSettings,
  readSamplesForLastNDays,
  requestAuthorization,
} from '@/src/features/connectedApps/adapters/androidHealthConnect';
import {
  beginConnect,
  confirmConnection,
  requestConnectionSync,
} from '@/src/features/connectedApps/api/connectionsApi';
import { DEFAULT_BACKFILL_DAYS } from '@/src/features/connectedApps/constants';
import type { ConnectionView, SyncRunView } from '@/src/features/connectedApps/models/connection';
import { newRequestId } from '@/src/features/connectedApps/models/providers';
import {
  drainEvidenceUploadQueue,
  enqueueEvidenceBatch,
} from '@/src/features/connectedApps/queue/evidenceUploadQueue';
import { osHubNonFatalSyncMessage } from '@uap/connected-apps-contracts';

export type HealthConnectConnectOutcome = {
  connection: ConnectionView | null;
  status: 'connected' | 'permission_denied' | 'unavailable' | 'failed';
  message: string;
  evidenceQueued: number;
  evidenceUploaded: number;
  syncRun: SyncRunView | null;
  partialPermissions: boolean;
  availabilityReason?:
    | 'not_android'
    | 'module_missing'
    | 'sdk_unavailable'
    | 'update_required'
    | 'init_failed';
};

type HealthConnectBridge = {
  checkAvailability: typeof checkAvailability;
  requestAuthorization: typeof requestAuthorization;
  readSamplesForLastNDays: typeof readSamplesForLastNDays;
  openHealthConnectInstallOrSettings: typeof openHealthConnectInstallOrSettings;
};

const defaultBridge: HealthConnectBridge = {
  checkAvailability,
  requestAuthorization,
  readSamplesForLastNDays,
  openHealthConnectInstallOrSettings,
};

/**
 * C2 Android connect path: beginConnect → OS auth → confirmConnected → read → queue → drain.
 * Optional POST /sync may still return OS_HUB_UPLOAD_ONLY; evidence-batch is the ingest path.
 */
export async function runHealthConnectConnectFlow(
  client: ApiClient,
  options: {
    backfillDays?: number;
    healthConnect?: HealthConnectBridge;
  } = {},
): Promise<HealthConnectConnectOutcome> {
  const bridge = options.healthConnect ?? defaultBridge;
  const backfillDays = options.backfillDays ?? DEFAULT_BACKFILL_DAYS;

  const availability = await bridge.checkAvailability();
  if (availability.status !== 'available') {
    if (
      availability.reason === 'sdk_unavailable' ||
      availability.reason === 'update_required'
    ) {
      void bridge.openHealthConnectInstallOrSettings(availability.reason);
    }
    return {
      connection: null,
      status: 'unavailable',
      message: availability.message,
      evidenceQueued: 0,
      evidenceUploaded: 0,
      syncRun: null,
      partialPermissions: false,
      availabilityReason: availability.reason,
    };
  }

  const pending = await beginConnect(client, {
    requestId: newRequestId(),
    provider: 'HEALTH_CONNECT',
  });

  const auth = await bridge.requestAuthorization();
  if (!auth.granted) {
    return {
      connection: pending,
      status: 'permission_denied',
      message:
        auth.message ||
        'Health Connect permission was not granted. Enable Sleep, Resting heart rate, HRV, Steps, Active calories, and Exercise in Health Connect, then try again.',
      evidenceQueued: 0,
      evidenceUploaded: 0,
      syncRun: null,
      partialPermissions: true,
    };
  }

  const connection = await confirmConnection(client, pending.connectionId);

  const read = await bridge.readSamplesForLastNDays(
    backfillDays,
    auth.grantedRecordTypes,
  );
  let evidenceQueued = 0;
  let evidenceUploaded = 0;

  if (read.items.length > 0) {
    const requestId = newRequestId();
    await enqueueEvidenceBatch({
      requestId,
      connectionId: connection.connectionId,
      items: read.items,
    });
    evidenceQueued = read.items.length;
    const drain = await drainEvidenceUploadQueue(client);
    evidenceUploaded = drain.uploaded;
  }

  let syncRun: SyncRunView | null = null;
  try {
    syncRun = await requestConnectionSync(client, connection.connectionId, newRequestId());
  } catch {
    // Optional; transport errors on sync must not undo a successful evidence upload.
    syncRun = null;
  }

  const partial = auth.partial || read.partial || read.items.length === 0;
  const messageParts = [
    'Health Connect is connected.',
    auth.message,
    read.message,
    osHubNonFatalSyncMessage(syncRun?.errorCode ?? null),
  ].filter(Boolean);

  return {
    connection,
    status: 'connected',
    message: messageParts.join(' '),
    evidenceQueued,
    evidenceUploaded,
    syncRun,
    partialPermissions: partial,
  };
}
