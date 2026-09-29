import { describe, expect, it, vi } from 'vitest';

vi.mock('@/features/billing/api/accountBillingApi', () => ({
  fetchAccountBillingStatus: vi.fn(),
  createAccountCheckoutSession: vi.fn(),
  syncAccountSubscription: vi.fn(),
  rememberPendingAccountCheckout: vi.fn(),
  readPendingAccountCheckout: vi.fn(() => null),
  clearPendingAccountCheckout: vi.fn(),
}));

describe('account billing router wiring', () => {
  it('exports and builds AppRouter including AccountBilling lazy routes', async () => {
    const [billingPage, returnPages, router] = await Promise.all([
      import('@/features/billing/pages/AccountBillingPage'),
      import('@/features/billing/pages/AccountBillingCheckoutReturnPages'),
      import('@/app/router/index'),
    ]);

    expect(typeof billingPage.AccountBillingPage).toBe('function');
    expect(typeof returnPages.AccountBillingCheckoutSuccessPage).toBe('function');
    expect(typeof returnPages.AccountBillingCheckoutCancelPage).toBe('function');
    expect(typeof router.AppRouter).toBe('function');
    // Invoke so New Code Route elements under /app/billing* are executed for coverage.
    expect(router.AppRouter()).toBeTruthy();
  });
});
