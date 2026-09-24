import { ApiError } from '@/core/api/errors';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { OrganizationBillingPage } from '@/features/coach/pages/OrganizationBillingPage';
import { BillingCheckoutCancelPage, BillingCheckoutSuccessPage } from '@/features/coach/pages/BillingCheckoutReturnPages';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

const createCheckout = vi.fn();
const fetchCapacity = vi.fn();
const fetchStatus = vi.fn();
const createPortal = vi.fn();
const changePlan = vi.fn();

vi.mock('@/features/coach/api/billingApi', () => ({
  createOrganizationCheckoutSession: (...args: unknown[]) => createCheckout(...args),
  fetchOrganizationCapacity: (...args: unknown[]) => fetchCapacity(...args),
  fetchOrganizationBillingStatus: (...args: unknown[]) => fetchStatus(...args),
  createOrganizationPortalSession: (...args: unknown[]) => createPortal(...args),
  changeOrganizationPlan: (...args: unknown[]) => changePlan(...args),
  cancelOrganizationRenewal: vi.fn(),
  reactivateOrganizationSubscription: vi.fn(),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'owner@example.com', status: 'ACTIVE' },
    status: 'AUTHENTICATED',
    apiClient: { axios: {} },
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ organizationId: 'org-1' }),
  };
});

describe('Organization billing acquisition', () => {
  beforeEach(() => {
    vi.stubGlobal('location', { ...window.location, assign: vi.fn() });
    createCheckout.mockReset();
    createCheckout.mockResolvedValue({ checkoutUrl: 'https://checkout.stripe.test/cs_test' });
    fetchCapacity.mockReset();
    fetchCapacity.mockResolvedValue({
      activeAthleteCount: 23,
      bandCapacity: 25,
      remainingCapacity: 2,
      atCapacity: false,
      overCapacity: false,
    });
    fetchStatus.mockReset();
    fetchStatus.mockRejectedValue(new ApiError('missing', { category: 'NOT_FOUND', status: 404 }));
    createPortal.mockReset();
    changePlan.mockReset();
    changePlan.mockResolvedValue({
      subscriptionId: 'sub-1',
      planKey: 'ORG_BAND_75',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shows locked prices, trial copy, and tax-exclusive posture', async () => {
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByText('23 of 25 active athletes')).toBeInTheDocument();
    expect(screen.getAllByText(/14-day/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/plus applicable taxes/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: 'Start Checkout' })).toBeEnabled();

    await userEvent.click(screen.getByRole('button', { name: 'Start Checkout' }));
    expect(createCheckout).toHaveBeenCalledWith(
      { axios: {} },
      'org-1',
      expect.objectContaining({
        planKey: 'ORG_BAND_25',
        cadence: 'MONTHLY',
      }),
    );
  });

  it('submits the selected annual plan and surfaces checkout failure', async () => {
    createCheckout.mockRejectedValue(new Error('Unable to start checkout.'));
    renderWithProviders(<OrganizationBillingPage />);

    await screen.findByRole('button', { name: 'Start Checkout' });
    await userEvent.selectOptions(screen.getByRole('combobox'), 'ORG_BAND_75');
    await userEvent.click(screen.getByRole('radio', { name: /Annual/i }));
    await userEvent.click(screen.getByRole('button', { name: 'Start Checkout' }));

    expect(createCheckout).toHaveBeenCalledWith(
      { axios: {} },
      'org-1',
      expect.objectContaining({
        planKey: 'ORG_BAND_75',
        cadence: 'ANNUAL',
      }),
    );
    expect(await screen.findByText('Unable to start checkout.')).toBeInTheDocument();
  });

  it('success return does not claim the subscription is activated', () => {
    renderWithProviders(<BillingCheckoutSuccessPage />);
    expect(screen.getByText(/Payment setup received/i)).toBeInTheDocument();
    expect(screen.queryByText(/Subscription activated/i)).not.toBeInTheDocument();
  });

  it('cancel return does not claim a subscription change', () => {
    renderWithProviders(<BillingCheckoutCancelPage />);
    expect(screen.getByText(/Checkout was not completed/i)).toBeInTheDocument();
    expect(screen.queryByText(/Subscription activated/i)).not.toBeInTheDocument();
  });

  it('shows payment management and hides checkout while a plan is active', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-1',
      planKey: 'ORG_BAND_75',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
    });
    createPortal.mockResolvedValue({ url: 'https://billing.stripe.test/session' });
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByRole('button', { name: 'Manage payment method and invoices' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change plan' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel renewal' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Start Checkout' })).not.toBeInTheDocument();
    expect(screen.queryByText(/Manage subscription/i)).not.toBeInTheDocument();
    expect(screen.getByRole('combobox')).toHaveValue('ORG_BAND_75');
    expect(screen.getByRole('radio', { name: /Monthly/i })).toBeChecked();

    await userEvent.click(screen.getByRole('button', { name: 'Change plan' }));
    expect(changePlan).toHaveBeenCalledWith(
      { axios: {} },
      'org-1',
      'sub-1',
      expect.objectContaining({
        targetPlanKey: 'ORG_BAND_75',
        targetCadence: 'MONTHLY',
      }),
    );
  });

  it('offers reactivate without plan changes while cancellation is scheduled', async () => {
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-1',
      planKey: 'ORG_BAND_75',
      cadence: 'ANNUAL',
      lifecycleState: 'CANCEL_AT_PERIOD_END',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
    });
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByRole('button', { name: 'Reactivate' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Change plan' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancel renewal' })).not.toBeInTheDocument();
  });

  it('keeps a same-band cadence change available when usage already exceeds the band', async () => {
    fetchCapacity.mockResolvedValue({
      activeAthleteCount: 26,
      bandCapacity: 25,
      remainingCapacity: 0,
      atCapacity: true,
      overCapacity: true,
    });
    fetchStatus.mockResolvedValue({
      subscriptionId: 'sub-1',
      planKey: 'ORG_BAND_25',
      cadence: 'MONTHLY',
      lifecycleState: 'ACTIVE',
      trialEndsAt: null,
      currentPeriodEndsAt: '2026-10-01T00:00:00Z',
    });
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByRole('combobox')).toHaveValue('ORG_BAND_25');
    expect(screen.getByRole('option', { name: /Starter/i })).toBeEnabled();
    expect(screen.getByRole('button', { name: 'Change plan' })).toBeEnabled();
    await userEvent.click(screen.getByRole('radio', { name: /Annual/i }));
    await userEvent.click(screen.getByRole('button', { name: 'Change plan' }));
    expect(changePlan).toHaveBeenCalledWith(
      { axios: {} },
      'org-1',
      'sub-1',
      expect.objectContaining({
        targetPlanKey: 'ORG_BAND_25',
        targetCadence: 'ANNUAL',
      }),
    );
  });

  it('selects a fitting checkout band and disables checkout when none fits', async () => {
    fetchCapacity.mockResolvedValue({
      activeAthleteCount: 40,
      bandCapacity: null,
      remainingCapacity: null,
      atCapacity: false,
      overCapacity: false,
    });
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByRole('combobox')).toHaveValue('ORG_BAND_75');
    expect(screen.getByRole('option', { name: /Starter/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Start Checkout' })).toBeEnabled();
  });

  it('disables checkout when every self-service band is undersized', async () => {
    fetchCapacity.mockResolvedValue({
      activeAthleteCount: 300,
      bandCapacity: null,
      remainingCapacity: null,
      atCapacity: false,
      overCapacity: true,
    });
    renderWithProviders(<OrganizationBillingPage />);

    expect(await screen.findByRole('button', { name: 'Start Checkout' })).toBeDisabled();
  });
});
