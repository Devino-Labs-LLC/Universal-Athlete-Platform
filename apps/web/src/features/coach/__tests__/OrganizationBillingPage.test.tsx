import { beforeEach, describe, expect, it, vi } from 'vitest';

import { OrganizationBillingPage } from '@/features/coach/pages/OrganizationBillingPage';
import { BillingCheckoutSuccessPage } from '@/features/coach/pages/BillingCheckoutReturnPages';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

const createCheckout = vi.fn();

vi.mock('@/features/coach/api/billingApi', () => ({
  createOrganizationCheckoutSession: (...args: unknown[]) => createCheckout(...args),
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
    createCheckout.mockReset();
    createCheckout.mockResolvedValue({ checkoutUrl: 'https://checkout.stripe.test/cs_test' });
  });

  it('shows locked prices, trial copy, and tax-exclusive posture', async () => {
    renderWithProviders(<OrganizationBillingPage />);

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

  it('success return does not claim the subscription is activated', () => {
    renderWithProviders(<BillingCheckoutSuccessPage />);
    expect(screen.getByText(/Payment setup received/i)).toBeInTheDocument();
    expect(screen.queryByText(/Subscription activated/i)).not.toBeInTheDocument();
  });
});
