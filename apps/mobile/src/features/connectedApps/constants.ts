/**
 * Client default mirrors server `uap.integrations.backfill-days` (ADR-051 / D4 = B).
 * Prefer the server value when a bootstrap/config surface exposes it; until then keep in sync.
 */
export const DEFAULT_BACKFILL_DAYS = 30;

export const EVIDENCE_UPLOAD_QUEUE_STORAGE_KEY = 'uap:connectedApps:evidenceUploadQueue:v1';

export const HEALTHKIT_SOURCE_APP = 'Apple Health';

export const HEALTH_CONNECT_SOURCE_APP = 'Health Connect';

/** Health Connect Play provider package (Android). */
export const HEALTH_CONNECT_PROVIDER_PACKAGE = 'com.google.android.apps.healthdata';

/**
 * Without READ_HEALTH_DATA_HISTORY, Health Connect limits readable history to ~30 days.
 * C2 stays within this limit and does not request the extended-history special permission.
 */
export const HEALTH_CONNECT_MAX_HISTORY_DAYS = 30;
