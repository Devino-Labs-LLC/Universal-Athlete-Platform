/**
 * Client default mirrors server `uap.integrations.backfill-days` (ADR-051 / D4 = B).
 * Prefer the server value when a bootstrap/config surface exposes it; until then keep in sync.
 */
export const DEFAULT_BACKFILL_DAYS = 30;

export const EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY = 'uap:connectedApps:evidenceUploadQueue:v1';

export const HEALTHKIT_SOURCE_APP = 'Apple Health';
