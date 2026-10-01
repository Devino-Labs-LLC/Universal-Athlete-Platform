import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';

import type { ApiClient } from '@/src/core/api/apiClient';
import { isApiError } from '@/src/core/api/errors';
import { uploadEvidenceBatch } from '@/src/features/connectedApps/api/evidenceBatchesApi';
import { EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY } from '@/src/features/connectedApps/constants';
import type { EvidenceBatchItem } from '@/src/features/connectedApps/models/evidenceBatch';

export type QueuedEvidenceBatch = {
  requestId: string;
  connectionId: string;
  items: EvidenceBatchItem[];
  enqueuedAt: string;
  attempts: number;
};

type QueueStore = {
  getItem: (key: string) => Promise<string | null>;
  setItem: (key: string, value: string) => Promise<void>;
  removeItem: (key: string) => Promise<void>;
};

const defaultStore: QueueStore = AsyncStorage;

let storeOverride: QueueStore | null = null;
let draining = false;
const stoppedConnections = new Set<string>();

export function __setEvidenceQueueStoreForTests(store: QueueStore | null): void {
  storeOverride = store;
}

export function __resetEvidenceQueueStateForTests(): void {
  draining = false;
  stoppedConnections.clear();
}

function activeStore(): QueueStore {
  return storeOverride ?? defaultStore;
}

async function readQueue(): Promise<QueuedEvidenceBatch[]> {
  const raw = await activeStore().getItem(EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY);
  if (!raw) {
    return [];
  }
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.filter((entry): entry is QueuedEvidenceBatch => {
      return (
        typeof entry === 'object' &&
        entry != null &&
        typeof (entry as QueuedEvidenceBatch).requestId === 'string' &&
        typeof (entry as QueuedEvidenceBatch).connectionId === 'string' &&
        Array.isArray((entry as QueuedEvidenceBatch).items)
      );
    });
  } catch {
    return [];
  }
}

async function writeQueue(entries: QueuedEvidenceBatch[]): Promise<void> {
  if (entries.length === 0) {
    await activeStore().removeItem(EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY);
    return;
  }
  await activeStore().setItem(EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY, JSON.stringify(entries));
}

/**
 * Enqueue an evidence batch. Same requestId is idempotent (replaces payload, does not duplicate).
 */
export async function enqueueEvidenceBatch(input: {
  requestId: string;
  connectionId: string;
  items: EvidenceBatchItem[];
}): Promise<QueuedEvidenceBatch[]> {
  if (input.items.length === 0) {
    return readQueue();
  }
  const queue = await readQueue();
  const existingIndex = queue.findIndex((entry) => entry.requestId === input.requestId);
  const next: QueuedEvidenceBatch = {
    requestId: input.requestId,
    connectionId: input.connectionId,
    items: input.items,
    enqueuedAt: new Date().toISOString(),
    attempts: existingIndex >= 0 ? queue[existingIndex]!.attempts : 0,
  };
  if (existingIndex >= 0) {
    queue[existingIndex] = next;
  } else {
    queue.push(next);
  }
  stoppedConnections.delete(input.connectionId);
  await writeQueue(queue);
  return queue;
}

/** Stop draining and drop pending uploads for a disconnected connection. */
export async function stopQueueForConnection(connectionId: string): Promise<void> {
  stoppedConnections.add(connectionId);
  const queue = await readQueue();
  await writeQueue(queue.filter((entry) => entry.connectionId !== connectionId));
}

export async function listQueuedEvidenceBatches(): Promise<QueuedEvidenceBatch[]> {
  return readQueue();
}

function isRetryableUploadError(error: unknown): boolean {
  if (isApiError(error)) {
    return error.category === 'network' || error.category === 'timeout' || error.category === 'server';
  }
  return true;
}

export async function drainEvidenceUploadQueue(client: ApiClient): Promise<{
  uploaded: number;
  remaining: number;
  skippedStopped: number;
}> {
  if (draining) {
    const queue = await readQueue();
    return { uploaded: 0, remaining: queue.length, skippedStopped: 0 };
  }

  const net = await NetInfo.fetch();
  if (net.isConnected === false) {
    const queue = await readQueue();
    return { uploaded: 0, remaining: queue.length, skippedStopped: 0 };
  }

  draining = true;
  let uploaded = 0;
  let skippedStopped = 0;
  try {
    const queue = await readQueue();
    const remaining: QueuedEvidenceBatch[] = [];

    for (const entry of queue) {
      if (stoppedConnections.has(entry.connectionId)) {
        skippedStopped += 1;
        continue;
      }
      try {
        await uploadEvidenceBatch(client, entry.connectionId, {
          requestId: entry.requestId,
          items: entry.items,
        });
        uploaded += 1;
      } catch (error) {
        if (isRetryableUploadError(error)) {
          remaining.push({ ...entry, attempts: entry.attempts + 1 });
        }
        // Non-retryable client errors are dropped to avoid poison loops.
      }
    }

    await writeQueue(remaining);
    return { uploaded, remaining: remaining.length, skippedStopped };
  } finally {
    draining = false;
  }
}
