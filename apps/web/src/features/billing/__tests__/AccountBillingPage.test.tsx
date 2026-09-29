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
const rememberPending = vi.fn();
const readPending = vi.fn();
const clearPending = vi.fn();

vi.mock('@/features/billing/api/accountBillingApi', () => ({
  fetchAccountBillingStatus: (...args: unknown[]) => fetchStatus(...args),
  createAccountCheckoutSession: (...args: unknown[]) => createCheckout(...args),
  syncAccountSubscription: (...args: unknown[]) => syncSubscription(...args),
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
      subscriptionId: '11111111-2222-3333-4444-555555555555',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });
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
    expect(screen.getByText(/No trial/i)).toBeInTheDocument();
    expect(screen.getByText(/\$9\.99/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('radio', { name: /Annual/i }));
    expect(screen.getByText(/\$99\.99/)).toBeInTheDocument();

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

  it('shows active subscription and hides checkout', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-1',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Individual Premium/i)).toBeInTheDocument();
    expect(screen.getByText(/Your Premium subscription is active/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
    expect(screen.queryByText(/Cancel renewal/i)).not.toBeInTheDocument();
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
      subscriptionId: 'sub-pending',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'PENDING',
      trialEndsAt: null,
      currentPeriodEndsAt: null,
      graceEndsAt: null,
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Checkout is already in progress/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
  });

  it('shows grace-period attention copy', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-grace',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'ANNUAL',
      lifecycleState: 'GRACE_PERIOD',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: '2026-09-20T12:00:00Z',
    });

    renderWithProviders(<AccountBillingPage />);

    expect(await screen.findByText(/Payment needs attention/i)).toBeInTheDocument();
    expect(screen.getByText(/Access continues until/i)).toBeInTheDocument();
  });

  it('shows past-due and cancel-at-period-end attention copy', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-past-due',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'PAST_DUE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });

    const { unmount } = renderWithProviders(<AccountBillingPage />);
    expect(await screen.findByText(/Billing needs attention/i)).toBeInTheDocument();
    unmount();

    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-cancel',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'CANCEL_AT_PERIOD_END',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
      graceEndsAt: null,
    });
    renderWithProviders(<AccountBillingPage />);
    expect(await screen.findByText(/Renewal is scheduled to end/i)).toBeInTheDocument();
  });

  it('allows checkout again after an expired subscription', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-expired',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
      lifecycleState: 'EXPIRED',
      trialEndsAt: null,
      currentPeriodEndsAt: null,
      graceEndsAt: null,
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
