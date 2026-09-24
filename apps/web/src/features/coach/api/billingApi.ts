import type { ApiClient } from '@/core/api/apiClient';
import { z } from 'zod';
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

export async function createOrganizationPortalSession(
  client: ApiClient,
  organizationId: string,
): Promise<{ url: string }> {
  const response = await client.axios.post(
    `/api/v1/billing/organizations/${organizationId}/portal-sessions`,
  );
  return z.object({ url: z.string().url() }).parse(response.data);
}

export async function changeOrganizationPlan(
  client: ApiClient,
  organizationId: string,
  subscriptionId: string,
  input: { requestId: string; targetPlanKey: string; targetCadence: string },
): Promise<OrganizationBillingStatus> {
  const response = await client.axios.post(
    `/api/v1/billing/organizations/${organizationId}/subscriptions/${subscriptionId}/plan-changes`,
    input,
  );
  return organizationBillingStatusSchema.parse(response.data);
}

export async function cancelOrganizationRenewal(
  client: ApiClient,
  organizationId: string,
  subscriptionId: string,
  requestId: string,
): Promise<OrganizationBillingStatus> {
  const response = await client.axios.post(
    `/api/v1/billing/organizations/${organizationId}/subscriptions/${subscriptionId}/cancel`,
    { requestId },
  );
  return organizationBillingStatusSchema.parse(response.data);
}

export async function reactivateOrganizationSubscription(
  client: ApiClient,
  organizationId: string,
  subscriptionId: string,
  requestId: string,
): Promise<OrganizationBillingStatus> {
  const response = await client.axios.post(
    `/api/v1/billing/organizations/${organizationId}/subscriptions/${subscriptionId}/reactivate`,
    { requestId },
  );
  return organizationBillingStatusSchema.parse(response.data);
}
