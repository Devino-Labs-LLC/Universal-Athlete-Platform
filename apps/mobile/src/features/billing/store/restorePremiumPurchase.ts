import { Platform } from 'react-native';

import type { ApiClient } from '@/src/core/api/apiClient';
import { validateOrRestoreAppleTransaction } from '@/src/features/billing/api/appleBillingApi';
import { validateOrRestoreGooglePlayPurchase } from '@/src/features/billing/api/googlePlayBillingApi';
import type { AccountBillingStatus } from '@/src/features/billing/models/accountBilling';
import {
  collectStoreRestorePayload,
  type StoreRestorePayload,
} from '@/src/features/billing/store/storeRestoreBridge';

/**
 * Posts a store restore payload through the existing Apple / Google thin APIs.
 * Callers may pass an explicit payload (tests / future IAP) or rely on the injectable bridge.
 */
export async function restorePremiumPurchase(
  client: ApiClient,
  payload?: StoreRestorePayload | null,
): Promise<AccountBillingStatus> {
  const restorePayload = payload === undefined ? await collectStoreRestorePayload() : payload;
  if (!restorePayload) {
    throw new Error(
      Platform.OS === 'ios'
        ? 'No App Store purchase was available to restore on this device.'
        : Platform.OS === 'android'
          ? 'No Google Play purchase was available to restore on this device.'
          : 'Store restore is only available on iOS or Android.',
    );
  }
  if (restorePayload.platform === 'ios') {
    return validateOrRestoreAppleTransaction(client, restorePayload.signedTransactionInfo);
  }
  return validateOrRestoreGooglePlayPurchase(
    client,
    restorePayload.purchaseToken,
    restorePayload.productId,
  );
}
