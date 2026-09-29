import { Platform } from 'react-native';

import { restorePremiumPurchase } from '@/src/features/billing/store/restorePremiumPurchase';
import { setStoreRestoreCollector } from '@/src/features/billing/store/storeRestoreBridge';

const mockValidateApple = jest.fn();
const mockValidateGoogle = jest.fn();

jest.mock('@/src/features/billing/api/appleBillingApi', () => ({
  validateOrRestoreAppleTransaction: (...args: unknown[]) => mockValidateApple(...args),
}));

jest.mock('@/src/features/billing/api/googlePlayBillingApi', () => ({
  validateOrRestoreGooglePlayPurchase: (...args: unknown[]) => mockValidateGoogle(...args),
}));

describe('restorePremiumPurchase', () => {
  const originalOs = Platform.OS;
  const client = { axios: {} } as never;

  beforeEach(() => {
    jest.clearAllMocks();
    setStoreRestoreCollector(null);
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });
    mockValidateApple.mockResolvedValue({
      subscriptionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      provider: 'APPLE_APP_STORE',
      managementChannel: 'APPLE_APP_STORE',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });
  });

  afterEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => originalOs });
    setStoreRestoreCollector(null);
  });

  it('posts Apple restore payload through the thin API', async () => {
    setStoreRestoreCollector(async () => ({
      platform: 'ios',
      signedTransactionInfo: 'header.payload.signature',
    }));

    await restorePremiumPurchase(client);

    expect(mockValidateApple).toHaveBeenCalledWith(client, 'header.payload.signature');
    expect(mockValidateGoogle).not.toHaveBeenCalled();
  });

  it('posts Google Play restore payload through the thin API', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    mockValidateGoogle.mockResolvedValue({
      subscriptionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
      provider: 'GOOGLE_PLAY',
      managementChannel: 'GOOGLE_PLAY',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });

    await restorePremiumPurchase(client, {
      platform: 'android',
      purchaseToken: 'token-1',
      productId: 'premium.monthly',
    });

    expect(mockValidateGoogle).toHaveBeenCalledWith(client, 'token-1', 'premium.monthly');
  });

  it('fails closed when no store restore payload is available', async () => {
    await expect(restorePremiumPurchase(client)).rejects.toThrow(
      /No App Store purchase was available/i,
    );
    expect(mockValidateApple).not.toHaveBeenCalled();
  });
});
