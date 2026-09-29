import type { ApiClient } from '@/src/core/api/apiClient';

import {
  accountBillingStatusSchema,
  type AccountBillingStatus,
} from '@/src/features/billing/models/accountBilling';

export async function fetchAccountBillingStatus(
  client: ApiClient,
): Promise<AccountBillingStatus> {
  const response = await client.axios.get('/api/v1/billing/account');
  return accountBillingStatusSchema.parse(response.data);
}
