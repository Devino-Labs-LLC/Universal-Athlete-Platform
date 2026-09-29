export const accountBillingQueryKeys = {
  all: ['accountBilling'] as const,
  status: () => [...accountBillingQueryKeys.all, 'status'] as const,
};
