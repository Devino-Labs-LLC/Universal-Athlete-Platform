import { ApiError } from '@/core/api/errors';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  AccountBillingCheckoutCancelPage,
  AccountBillingCheckoutSuccessPage,
} from '@/features/billing/pages/AccountBillingCheckoutReturnPages';
import { AccountBillingPage } from '@/features/billing/pages/AccountBillingPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const fetchStatus = vi.fn();
const createCheckout = vi.fn();
const syncSubscription = vi.fn();
const createPortal = vi.fn();
const cancelRenewal = vi.fn();
const reactivateSubscription = vi.fn();
const rememberPending = vi.fn();
const readPending = vi.fn();
const clearPending = vi.fn();

vi.mock('@/features/billing/api/accountBillingApi', () => ({
  fetchAccountBillingStatus: (...args: unknown[]) => fetchStatus(...args),
  createAccountCheckoutSession: (...args: unknown[]) => createCheckout(...args),
  syncAccountSubscription: (...args: unknown[]) => syncSubscription(...args),
  createAccountPortalSession: (...args: unknown[]) => createPortal(...args),
  cancelAccountRenewal: (...args: unknown[]) => cancelRenewal(...args),
  reactivateAccountSubscription: (...args: unknown[]) => reactivateSubscription(...args),
  rememberPendingAccountCheckout: (...args: unknown[]) => rememberPending(...args),
  readPendingAccountCheckout: (...args: unknown[]) => readPending(...args),
  clearPendingAccountCheckout: (...args: unknown[]) => clearPending(...args),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'athlete@example.com', status: 'ACTIVE' },
    status: 'AUTHENTICATED',
    apiClient: { axios: {} },
  }),
}));

const activeStatus = {
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

describe('Account billing page', () => {
  beforeEach(() => {
    vi.stubGlobal('location', { ...window.location, assign: vi.fn() });
    fetchStatus.mockReset();
    fetchStatus.mockRejectedValue(
      new ApiError('Account billing was not found', {
        category: 'NOT_FOUND',
        status: 404,
        code: 'ACCOUNT_NOT_FOUND',
      }),
    );
    createCheckout.mockReset();
    createCheckout.mockResolvedValue({
      subscriptionId: '11111111-2222-3333-4444-555555555555',
      checkoutSessionId: 'cs_test',
      checkoutUrl: 'https://checkout.stripe.test/cs_test',
    });
    syncSubscription.mockReset();
    syncSubscription.mockResolvedValue({
      ...activeStatus,
      subscriptionId: '11111111-2222-3333-4444-555555555555',
    });
    createPortal.mockReset();
    createPortal.mockResolvedValue({
      url: 'https://billing.stripe.test/portal/account',
    });
    cancelRenewal.mockReset();
    cancelRenewal.mockResolvedValue({
      ...activeStatus,
      lifecycleState: 'CANCEL_AT_PERIOD_END',
    });
    reactivateSubscription.mockReset();
    reactivateSubscription.mockResolvedValue(activeStatus);
    rememberPending.mockReset();
    readPending.mockReset();
    readPending.mockReturnValue(null);
    clearPending.mockReset();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shows locked prices, no-trial copy, and starts checkout', async () => {
    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByRole('button', { name: 'Start Checkout' })).toBeEnabled();
    expect(screen.getAllByText(/plus applicable taxes/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/No trial/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/\$9\.99/).length).toBeGreaterThanOrEqual(1);

    await userEvent.click(screen.getByRole('radio', { name: /Annual/i }));
    expect(screen.getAllByText(/\$99\.99/).length).toBeGreaterThanOrEqual(1);

    await userEvent.click(screen.getByRole('button', { name: 'Start Checkout' }));

    await waitFor(() => {
      expect(createCheckout).toHaveBeenCalledWith(
        { axios: {} },
        expect.objectContaining({
          planKey: 'INDIVIDUAL_PREMIUM',
          cadence: 'ANNUAL',
        }),
      );
    });
    expect(rememberPending).toHaveBeenCalledWith({
      subscriptionId: '11111111-2222-3333-4444-555555555555',
      checkoutSessionId: 'cs_test',
    });
    expect(window.location.assign).toHaveBeenCalledWith('https://checkout.stripe.test/cs_test');
  });

  it('surfaces checkout failure without navigating away', async () => {
    createCheckout.mockRejectedValue(new Error('Unable to start checkout.'));
    renderWithProviders(<AccountBillingPage />);

    await screen.findByRole('button', { name: 'Start Checkout' });
    await userEvent.click(screen.getByRole('button', { name: 'Start Checkout' }));

    expect(await screen.findByText('Unable to start checkout.')).toBeInTheDocument();
    expect(window.location.assign).not.toHaveBeenCalled();
  });

  it('shows locked pricing summary on the authenticated billing page', async () => {
    renderWithProviders(<AccountBillingPage />);

    expect(
      await screen.findByRole('region', { name: /Individual Premium pricing/i }),
    ).toBeInTheDocument();
    expect(screen.getByText(/\$9\.99 \/ month/i)).toBeInTheDocument();
    expect(screen.getByText(/\$99\.99 \/ year/i)).toBeInTheDocument();
  });

  it('shows active subscription with portal and cancel actions', async () => {
    fetchStatus.mockResolvedValue(activeStatus);

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Your Premium subscription is active/i)).toBeInTheDocument();
    expect(screen.getByText(/Premium origin: Stripe/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Manage payment method and invoices' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel renewal' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reactivate' })).not.toBeInTheDocument();
  });

  it('routes App Store origin to store management without Stripe portal actions', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      provider: 'APPLE_APP_STORE',
      managementChannel: 'APPLE_APP_STORE',
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Premium origin: App Store/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Manage in App Store' })).toHaveAttribute(
      'href',
      'https://apps.apple.com/account/subscriptions',
    );
    expect(
      screen.queryByRole('button', { name: 'Manage payment method and invoices' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
  });

  it('routes Google Play origin to store management without Stripe portal actions', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      provider: 'GOOGLE_PLAY',
      managementChannel: 'GOOGLE_PLAY',
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Premium origin: Google Play/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Manage in Google Play' })).toHaveAttribute(
      'href',
      'https://play.google.com/store/account/subscriptions',
    );
    expect(
      screen.queryByRole('button', { name: 'Manage payment method and invoices' }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
  });

  it('opens the customer portal from an active subscription', async () => {
    fetchStatus.mockResolvedValue(activeStatus);
    renderWithProviders(<AccountBillingPage />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Manage payment method and invoices' }),
    );

    await waitFor(() => {
      expect(createPortal).toHaveBeenCalledWith({ axios: {} });
    });
    expect(window.location.assign).toHaveBeenCalledWith(
      'https://billing.stripe.test/portal/account',
    );
  });

  it('confirms cancel renewal and updates status', async () => {
    fetchStatus.mockResolvedValue(activeStatus);
    renderWithProviders(<AccountBillingPage />);

    await userEvent.click(await screen.findByRole('button', { name: 'Cancel renewal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm cancel renewal' }));

    await waitFor(() => {
      expect(cancelRenewal).toHaveBeenCalledWith(
        { axios: {} },
        'sub-1',
        expect.any(String),
      );
    });
    expect(await screen.findByRole('button', { name: 'Reactivate' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
  });

  it('reactivates a subscription scheduled to end', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      lifecycleState: 'CANCEL_AT_PERIOD_END',
    });
    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Renewal is scheduled to end/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Reactivate' }));

    await waitFor(() => {
      expect(reactivateSubscription).toHaveBeenCalledWith(
        { axios: {} },
        'sub-1',
        expect.any(String),
      );
    });
    expect(await screen.findByText(/Your Premium subscription is active/i)).toBeInTheDocument();
  });

  it('surfaces manage action conflicts without navigating away', async () => {
    fetchStatus.mockResolvedValue(activeStatus);
    cancelRenewal.mockRejectedValue(
      new ApiError('This subscription cannot be changed in its current state.', {
        category: 'CONFLICT',
        status: 409,
        code: 'BILLING_LIFECYCLE_CONFLICT',
      }),
    );
    renderWithProviders(<AccountBillingPage />);

    await userEvent.click(await screen.findByRole('button', { name: 'Cancel renewal' }));
    await userEvent.click(screen.getByRole('button', { name: 'Confirm cancel renewal' }));

    expect(
      await screen.findByText('This subscription cannot be changed in its current state.'),
    ).toBeInTheDocument();
    expect(window.location.assign).not.toHaveBeenCalled();
  });

  it('shows portal without cancel or reactivate during grace', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      subscriptionId: 'sub-grace',
      cadence: 'ANNUAL',
      lifecycleState: 'GRACE_PERIOD',
      graceEndsAt: '2026-09-20T12:00:00Z',
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Payment needs attention/i)).toBeInTheDocument();
    expect(screen.getByText(/Access continues until/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Manage payment method and invoices' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reactivate' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
  });

  it('degrades gracefully when Stripe billing routes are unavailable', async () => {
    fetchStatus.mockRejectedValue(
      new ApiError('Not Found', { category: 'NOT_FOUND', status: 404 }),
    );

    renderWithProviders(<AccountBillingPage />);

    expect(
      await screen.findByText(/Individual Premium checkout is not available right now/i),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Manage payment method and invoices' }),
    ).not.toBeInTheDocument();
  });

  it('maps conflict errors from checkout', async () => {
    createCheckout.mockRejectedValue(
      new ApiError('Checkout is already in progress.', {
        category: 'CONFLICT',
        status: 409,
        code: 'BILLING_CHECKOUT_IN_PROGRESS',
      }),
    );

    renderWithProviders(<AccountBillingPage />);
    await screen.findByRole('button', { name: 'Start Checkout' });
    await userEvent.click(screen.getByRole('button', { name: 'Start Checkout' }));

    expect(await screen.findByText('Checkout is already in progress.')).toBeInTheDocument();
  });

  it('shows pending checkout state without offering a new checkout', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      subscriptionId: 'sub-pending',
      lifecycleState: 'PENDING',
      currentPeriodEndsAt: null,
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Checkout is already in progress/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Manage payment method and invoices' }),
    ).not.toBeInTheDocument();
  });

  it('shows past-due attention with portal only', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      subscriptionId: 'sub-past-due',
      lifecycleState: 'PAST_DUE',
    });

    renderWithProviders(<AccountBillingPage />);
    expect(await screen.findByText(/Billing needs attention/i)).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Manage payment method and invoices' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reactivate' })).not.toBeInTheDocument();
  });

  it('shows past-due store origin with App Store manage link only', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      provider: 'APPLE_APP_STORE',
      managementChannel: 'APPLE_APP_STORE',
      lifecycleState: 'PAST_DUE',
    });

    renderWithProviders(<AccountBillingPage />);
    expect(
      await screen.findByText(/Manage this subscription in the store where it was purchased/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Manage in App Store' })).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Manage payment method and invoices' }),
    ).not.toBeInTheDocument();
  });

  it('allows checkout again after an expired subscription', async () => {
    fetchStatus.mockResolvedValue({
      ...activeStatus,
      subscriptionId: 'sub-expired',
      lifecycleState: 'EXPIRED',
      currentPeriodEndsAt: null,
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByRole('button', { name: 'Start Checkout' })).toBeEnabled();
    expect(screen.getByText(/EXPIRED/i)).toBeInTheDocument();
  });

  it('surfaces non-missing load failures', async () => {
    fetchStatus.mockRejectedValue(
      new ApiError('temporary outage', {
        category: 'SERVER',
        status: 500,
        code: 'BILLING_PROVIDER_UNAVAILABLE',
      }),
    );

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText('Billing is temporarily unavailable. Try again.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
  });

  it('syncs on success return using query params', async () => {
    renderWithProviders(<AccountBillingCheckoutSuccessPage />, {
      initialEntries: [
        '/app/billing/success?subscriptionId=11111111-2222-3333-4444-555555555555&session_id=cs_test_1',
      ],
    });

    await waitFor(() => {
      expect(syncSubscription).toHaveBeenCalledWith(
        { axios: {} },
        '11111111-2222-3333-4444-555555555555',
        'cs_test_1',
      );
    });
    expect(await screen.findByText(/Confirmation request completed/i)).toBeInTheDocument();
    expect(screen.queryByText(/Subscription activated/i)).not.toBeInTheDocument();
    expect(clearPending).toHaveBeenCalled();
  });

  it('shows missing-params guidance when success return lacks ids', async () => {
    renderWithProviders(<AccountBillingCheckoutSuccessPage />, {
      initialEntries: ['/app/billing/success'],
    });

    expect(
      await screen.findByText(/Checkout returned without enough information/i),
    ).toBeInTheDocument();
    expect(syncSubscription).not.toHaveBeenCalled();
  });

  it('uses pending checkout storage when query params are absent', async () => {
    readPending.mockReturnValue({
      subscriptionId: '11111111-2222-3333-4444-555555555555',
      checkoutSessionId: 'cs_from_storage',
    });

    renderWithProviders(<AccountBillingCheckoutSuccessPage />, {
      initialEntries: ['/app/billing/success'],
    });

    await waitFor(() => {
      expect(syncSubscription).toHaveBeenCalledWith(
        { axios: {} },
        '11111111-2222-3333-4444-555555555555',
        'cs_from_storage',
      );
    });
  });

  it('surfaces sync failure on success return', async () => {
    syncSubscription.mockRejectedValue(
      new ApiError('Billing provider request failed', {
        category: 'SERVER',
        status: 502,
        code: 'BILLING_PROVIDER_UNAVAILABLE',
      }),
    );

    renderWithProviders(<AccountBillingCheckoutSuccessPage />, {
      initialEntries: [
        '/app/billing/success?subscriptionId=11111111-2222-3333-4444-555555555555&session_id=cs_test_1',
      ],
    });

    expect(await screen.findByText('Billing is temporarily unavailable. Try again.')).toBeInTheDocument();
  });

  it('cancel return does not claim a subscription change', () => {
    renderWithProviders(<AccountBillingCheckoutCancelPage />);
    expect(screen.getByText(/Checkout was not completed/i)).toBeInTheDocument();
    expect(screen.getByText(/No subscription change was made/i)).toBeInTheDocument();
    expect(screen.queryByText(/Subscription activated/i)).not.toBeInTheDocument();
    expect(clearPending).toHaveBeenCalled();
  });
});
