import type { ApiClient } from '@/src/core/api/apiClient';
import {
  isAvailable,
  readSamplesForLastNDays,
  requestAuthorization,
} from '@/src/features/connectedApps/adapters/iosHealthKit';
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

export type AppleHealthConnectOutcome = {
  connection: ConnectionView | null;
  status: 'connected' | 'permission_denied' | 'unavailable' | 'failed';
  message: string;
  evidenceQueued: number;
  evidenceUploaded: number;
  syncRun: SyncRunView | null;
  partialPermissions: boolean;
};

type HealthKitBridge = {
  isAvailable: typeof isAvailable;
  requestAuthorization: typeof requestAuthorization;
  readSamplesForLastNDays: typeof readSamplesForLastNDays;
};

const defaultBridge: HealthKitBridge = {
  isAvailable,
  requestAuthorization,
  readSamplesForLastNDays,
};

/**
 * C1 iOS connect path: beginConnect → OS auth → confirmConnected → read → queue → drain.
 * Optional POST /sync may still return NO_ADAPTER; evidence-batch is the ingest path.
 */
export async function runAppleHealthConnectFlow(
  client: ApiClient,
  options: {
    backfillDays?: number;
    healthKit?: HealthKitBridge;
  } = {},
): Promise<AppleHealthConnectOutcome> {
  const bridge = options.healthKit ?? defaultBridge;
  const backfillDays = options.backfillDays ?? DEFAULT_BACKFILL_DAYS;

  const available = await bridge.isAvailable();
  if (!available) {
    return {
      connection: null,
      status: 'unavailable',
      message:
        'Apple HealthKit is not available in this build or on this device. An iOS development build with HealthKit is required.',
      evidenceQueued: 0,
      evidenceUploaded: 0,
      syncRun: null,
      partialPermissions: false,
    };
  }

  const pending = await beginConnect(client, {
    requestId: newRequestId(),
    provider: 'APPLE_HEALTHKIT',
  });

  const auth = await bridge.requestAuthorization();
  if (!auth.granted) {
    return {
      connection: pending,
      status: 'permission_denied',
      message:
        auth.message ||
        'Apple Health permission was not granted. Enable Sleep, Heart, HRV, Activity, and Workouts in Settings → Health → Sharing, then try again.',
      evidenceQueued: 0,
      evidenceUploaded: 0,
      syncRun: null,
      partialPermissions: true,
    };
  }

  const connection = await confirmConnection(client, pending.connectionId);

  const read = await bridge.readSamplesForLastNDays(backfillDays);
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

  const partial = read.partial || read.items.length === 0;
  const messageParts = [
    'Apple Health is connected.',
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
