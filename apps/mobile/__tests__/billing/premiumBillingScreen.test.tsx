import { fireEvent, render, screen, waitFor } from '@testing-library/react-native';
import { Alert, Linking, Platform } from 'react-native';

import { ThemeProvider } from '@/src/app/theme/ThemeProvider';
import { PremiumBillingScreen } from '@/src/features/billing/screens/PremiumBillingScreen';
import { setStoreRestoreCollector } from '@/src/features/billing/store/storeRestoreBridge';

const mockRefetch = jest.fn();
const mockRestoreMutate = jest.fn();

jest.mock('@/src/features/billing/hooks/useAccountBilling', () => ({
  useAccountBillingStatus: jest.fn(),
  useRestorePremiumMutation: jest.fn(),
  isAccountBillingUnavailable: jest.requireActual(
    '@/src/features/billing/models/errors',
  ).isAccountBillingUnavailable,
}));

const { useAccountBillingStatus, useRestorePremiumMutation } = jest.requireMock(
  '@/src/features/billing/hooks/useAccountBilling',
);

const activeStripe = {
  subscriptionId: 'sub-1',
  provider: 'STRIPE' as const,
  managementChannel: 'STRIPE_CUSTOMER_PORTAL' as const,
  planKey: 'INDIVIDUAL_PREMIUM' as const,
  cadence: 'MONTHLY' as const,
  lifecycleState: 'ACTIVE' as const,
  trialEndsAt: null,
  currentPeriodEndsAt: '2026-10-01T00:00:00Z',
  graceEndsAt: null,
};

function renderScreen() {
  render(
    <ThemeProvider>
      <PremiumBillingScreen />
    </ThemeProvider>,
  );
}

describe('PremiumBillingScreen', () => {
  const originalOs = Platform.OS;

  beforeEach(() => {
    jest.clearAllMocks();
    setStoreRestoreCollector(null);
    jest.spyOn(Alert, 'alert').mockImplementation(() => undefined);
    jest.spyOn(Linking, 'openURL').mockResolvedValue(undefined as never);
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'ios' });

    useAccountBillingStatus.mockReturnValue({
      data: null,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });
    useRestorePremiumMutation.mockReturnValue({
      mutate: (...args: unknown[]) => mockRestoreMutate(...args),
      isPending: false,
    });
  });

  afterEach(() => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => originalOs });
    setStoreRestoreCollector(null);
    jest.restoreAllMocks();
  });

  it('shows locked pricing summary when no subscription', () => {
    renderScreen();

    expect(screen.getByTestId('premium-billing-screen')).toBeTruthy();
    expect(screen.getByText(/\$9\.99/)).toBeTruthy();
    expect(screen.getByText(/\$99\.99/)).toBeTruthy();
    expect(screen.getByText(/No active Premium/i)).toBeTruthy();
    expect(screen.getByTestId('premium-restore')).toBeTruthy();
  });

  it('shows Stripe origin without store manage link', () => {
    useAccountBillingStatus.mockReturnValue({
      data: activeStripe,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });

    renderScreen();

    expect(screen.getByText('Stripe')).toBeTruthy();
    expect(screen.getByText(/Your Premium subscription is active/i)).toBeTruthy();
    expect(screen.getByText(/purchased on the web \(Stripe\)/i)).toBeTruthy();
    expect(screen.queryByTestId('premium-manage-store')).toBeNull();
  });

  it('opens App Store management URL for Apple origin', async () => {
    useAccountBillingStatus.mockReturnValue({
      data: {
        ...activeStripe,
        provider: 'APPLE_APP_STORE',
        managementChannel: 'APPLE_APP_STORE',
      },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });

    renderScreen();

    expect(screen.getByText('App Store')).toBeTruthy();
    fireEvent.press(screen.getByTestId('premium-manage-store'));

    await waitFor(() => {
      expect(Linking.openURL).toHaveBeenCalledWith(
        'https://apps.apple.com/account/subscriptions',
      );
    });
  });

  it('opens Google Play management URL for Play origin', async () => {
    Object.defineProperty(Platform, 'OS', { configurable: true, get: () => 'android' });
    useAccountBillingStatus.mockReturnValue({
      data: {
        ...activeStripe,
        provider: 'GOOGLE_PLAY',
        managementChannel: 'GOOGLE_PLAY',
      },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });

    renderScreen();
    fireEvent.press(screen.getByTestId('premium-manage-store'));

    await waitFor(() => {
      expect(Linking.openURL).toHaveBeenCalledWith(
        'https://play.google.com/store/account/subscriptions',
      );
    });
  });

  it('shows grace copy for store-origin grace period', () => {
    useAccountBillingStatus.mockReturnValue({
      data: {
        ...activeStripe,
        provider: 'APPLE_APP_STORE',
        managementChannel: 'APPLE_APP_STORE',
        lifecycleState: 'GRACE_PERIOD',
        graceEndsAt: '2026-09-20T12:00:00Z',
      },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refetch: mockRefetch,
    });

    renderScreen();

    expect(screen.getByText(/Payment needs attention/i)).toBeTruthy();
    expect(screen.getByText(/Access continues until/i)).toBeTruthy();
    expect(screen.getByTestId('premium-manage-store')).toBeTruthy();
  });

  it('invokes restore mutation from the restore entry point', () => {
    renderScreen();
    fireEvent.press(screen.getByTestId('premium-restore'));
    expect(mockRestoreMutate).toHaveBeenCalled();
  });
});
