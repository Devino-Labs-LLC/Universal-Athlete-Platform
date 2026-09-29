/**
 * Collects a platform store restore payload for server validate/restore.
 * Native StoreKit / Play Billing modules are not bundled in H1 — inject a collector
 * in tests or when a later slice wires IAP. No secrets live here.
 */
export type StoreRestorePayload =
  | { platform: 'ios'; signedTransactionInfo: string }
  | { platform: 'android'; purchaseToken: string; productId: string };

export type StoreRestoreCollector = () => Promise<StoreRestorePayload | null>;

let restoreCollector: StoreRestoreCollector | null = null;

export function setStoreRestoreCollector(collector: StoreRestoreCollector | null): void {
  restoreCollector = collector;
}

export async function collectStoreRestorePayload(): Promise<StoreRestorePayload | null> {
  if (!restoreCollector) {
    return null;
  }
  return restoreCollector();
}
