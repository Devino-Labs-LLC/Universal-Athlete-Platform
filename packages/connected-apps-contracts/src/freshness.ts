/**
 * Provisional product freshness labels (PO D12 = B).
 * Not clinical. Thresholds are centrally owned placeholders for UX honesty.
 */
export type SyncFreshnessClass = 'fresh' | 'stale' | 'unavailable';

/** Hours after which successful sync is labeled stale (provisional). */
export const SYNC_STALE_AFTER_HOURS = 36;

export function classifySyncFreshness(
  lastSuccessfulSyncAt: string | null | undefined,
  nowMs: number = Date.now(),
): SyncFreshnessClass {
  if (lastSuccessfulSyncAt == null || lastSuccessfulSyncAt.trim() === '') {
    return 'unavailable';
  }
  const parsed = Date.parse(lastSuccessfulSyncAt);
  if (Number.isNaN(parsed)) {
    return 'unavailable';
  }
  const ageMs = nowMs - parsed;
  if (ageMs < 0) {
    return 'fresh';
  }
  const ageHours = ageMs / (1000 * 60 * 60);
  return ageHours <= SYNC_STALE_AFTER_HOURS ? 'fresh' : 'stale';
}

export function syncFreshnessLabel(freshness: SyncFreshnessClass): string {
  switch (freshness) {
    case 'fresh':
      return 'Recent';
    case 'stale':
      return 'Stale (provisional)';
    case 'unavailable':
      return 'Unavailable';
    default: {
      const _exhaustive: never = freshness;
      return _exhaustive;
    }
  }
}
