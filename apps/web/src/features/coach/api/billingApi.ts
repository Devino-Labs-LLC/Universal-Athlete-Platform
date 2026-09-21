import type { ApiClient } from '@/core/api/apiClient';
import {
  checkoutSessionResponseSchema,
  organizationBillingStatusSchema,
  organizationCapacitySnapshotSchema,
  type OrganizationBillingStatus,
  type OrganizationCapacitySnapshot,
} from '@/features/coach/models/billingCatalog';

export async function createOrganizationCheckoutSession(
  client: ApiClient,
  organizationId: string,
  input: { requestId: string; planKey: string; cadence: string },
): Promise<{ checkoutUrl: string }> {
  const response = await client.axios.post(
    `/api/v1/billing/organizations/${organizationId}/checkout-sessions`,
    {
      requestId: input.requestId,
      planKey: input.planKey,
      cadence: input.cadence,
    },
  );
  return checkoutSessionResponseSchema.parse(response.data);
}

export async function fetchOrganizationBillingStatus(
  client: ApiClient,
  organizationId: string,
): Promise<OrganizationBillingStatus> {
  const response = await client.axios.get(`/api/v1/billing/organizations/${organizationId}`);
  return organizationBillingStatusSchema.parse(response.data);
}

export async function fetchOrganizationCapacity(
  client: ApiClient,
  organizationId: string,
): Promise<OrganizationCapacitySnapshot> {
  const response = await client.axios.get(`/api/v1/billing/organizations/${organizationId}/capacity`);
  return organizationCapacitySnapshotSchema.parse(response.data);
}
