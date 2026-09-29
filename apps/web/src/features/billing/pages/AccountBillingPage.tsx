import { useState } from 'react';
import { Link } from 'react-router-dom';

import { isApiError } from '@/core/api/errors';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  rememberPendingAccountCheckout,
} from '@/features/billing/api/accountBillingApi';
import {
  useAccountBillingStatus,
  useCancelAccountRenewalMutation,
  useCreateAccountCheckoutMutation,
  useCreateAccountPortalSessionMutation,
  useReactivateAccountSubscriptionMutation,
  isAccountBillingUnavailable,
} from '@/features/billing/hooks/useAccountBilling';
import {
  individualPremiumCatalog,
  isStripeManagedChannel,
  premiumOriginLabel,
  storeManagementLabel,
  storeManagementUrl,
} from '@/features/billing/models/accountBilling';
import { accountBillingErrorMessage } from '@/features/billing/models/errors';
import styles from '@/features/billing/pages/AccountBillingPage.module.scss';

function formatUsd(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

function formatUtc(instant: string): string {
  const parsed = new Date(instant);
  if (Number.isNaN(parsed.getTime())) {
    return instant;
  }
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
    timeZone: 'UTC',
  }).format(parsed);
}

function cadenceLabel(cadence: 'MONTHLY' | 'ANNUAL' | null): string {
  if (cadence === 'ANNUAL') {
    return 'Annual';
  }
  if (cadence === 'MONTHLY') {
    return 'Monthly';
  }
  return 'Cadence unavailable';
}

export function AccountBillingPage() {
  const statusQuery = useAccountBillingStatus();
  const checkoutMutation = useCreateAccountCheckoutMutation();
  const portalMutation = useCreateAccountPortalSessionMutation();
  const cancelMutation = useCancelAccountRenewalMutation();
  const reactivateMutation = useReactivateAccountSubscriptionMutation();
  const [cadence, setCadence] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [actionError, setActionError] = useState<unknown>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);

  const subscription = statusQuery.data ?? null;
  const lifecycle = subscription?.lifecycleState;
  const stripeManaged =
    subscription != null && isStripeManagedChannel(subscription.managementChannel);
  const storeUrl =
    subscription != null ? storeManagementUrl(subscription.managementChannel) : null;
  const billingUnavailable =
    statusQuery.isError && isAccountBillingUnavailable(statusQuery.error);
  const loadError =
    statusQuery.isError && !billingUnavailable ? statusQuery.error : null;
  const showCheckout =
    statusQuery.isSuccess && (subscription == null || lifecycle === 'EXPIRED');
  const showStripePortal =
    stripeManaged && subscription != null && lifecycle !== 'PENDING' && !billingUnavailable;
  const showStoreManage =
    !stripeManaged &&
    subscription != null &&
    storeUrl != null &&
    lifecycle !== 'PENDING' &&
    lifecycle !== 'EXPIRED' &&
    !billingUnavailable;
  const manageable =
    stripeManaged && (lifecycle === 'ACTIVE' || lifecycle === 'TRIALING');
  const price =
    cadence === 'MONTHLY'
      ? individualPremiumCatalog.monthlyUsd
      : individualPremiumCatalog.annualUsd;
  const submitting =
    checkoutMutation.isPending ||
    portalMutation.isPending ||
    cancelMutation.isPending ||
    reactivateMutation.isPending;
  const visibleError = actionError ?? loadError;

  return (
    <Page
      title="Premium billing"
      description="Prices are shown before applicable taxes."
    >
      <div className={styles.stack}>
        {statusQuery.isLoading ? <LoadingView message="Loading billing…" /> : null}

        <section className={styles.pricingSummary} aria-label="Individual Premium pricing">
          <p className={styles.meta}>
            {individualPremiumCatalog.name} — locked list prices (tax-exclusive).
          </p>
          <p className={styles.meta}>
            {formatUsd(individualPremiumCatalog.monthlyUsd)} / month ·{' '}
            {formatUsd(individualPremiumCatalog.annualUsd)} / year
          </p>
          <p className={styles.meta}>No trial. Basic athlete features stay available without Premium.</p>
        </section>

        {billingUnavailable ? (
          <p className={styles.meta}>
            Individual Premium checkout is not available right now. You can keep using Athlete
            Readiness without a Premium subscription.
          </p>
        ) : null}

        {subscription && !billingUnavailable ? (
          <section className={styles.statusPanel} aria-label="Current subscription">
            <p className={styles.meta}>
              {individualPremiumCatalog.name} · {cadenceLabel(subscription.cadence)} ·{' '}
              {subscription.lifecycleState}
            </p>
            <p className={styles.meta}>
              Premium origin: {premiumOriginLabel(subscription.provider)}
            </p>
            {subscription.currentPeriodEndsAt ? (
              <p className={styles.meta}>
                Current period ends {subscription.currentPeriodEndsAt}
              </p>
            ) : null}
            {lifecycle === 'PENDING' ? (
              <p className={styles.meta}>Checkout is already in progress.</p>
            ) : null}
            {lifecycle === 'GRACE_PERIOD' && subscription.graceEndsAt ? (
              <>
                <p className={styles.meta}>Payment needs attention.</p>
                <p className={styles.meta}>
                  Access continues until {formatUtc(subscription.graceEndsAt)} UTC.
                </p>
              </>
            ) : null}
            {lifecycle === 'PAST_DUE' ? (
              <p className={styles.meta}>
                {stripeManaged
                  ? 'Billing needs attention. Manage payment method and invoices.'
                  : 'Billing needs attention. Manage this subscription in the store where it was purchased.'}
              </p>
            ) : null}
            {lifecycle === 'CANCEL_AT_PERIOD_END' ? (
              <p className={styles.meta}>Renewal is scheduled to end.</p>
            ) : null}
            {lifecycle === 'ACTIVE' || lifecycle === 'TRIALING' ? (
              <p className={styles.meta}>Your Premium subscription is active.</p>
            ) : null}
            {showStoreManage && storeUrl ? (
              <>
                <p className={styles.meta}>
                  This Premium subscription is managed through{' '}
                  {premiumOriginLabel(subscription.provider)}. Changes to payment method, renewal,
                  or cancellation happen there — not through Stripe on the web.
                </p>
                <div className={styles.formActions}>
                  <a
                    className={styles.storeLink}
                    href={storeUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    {storeManagementLabel(subscription.managementChannel)}
                  </a>
                </div>
              </>
            ) : null}
            {showStripePortal ? (
              <div className={styles.formActions}>
                <Button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    if (submitting) {
                      return;
                    }
                    setActionError(null);
                    portalMutation.mutate(undefined, {
                      onSuccess: (result) => {
                        window.location.assign(result.url);
                      },
                      onError: (cause) => {
                        setActionError(cause);
                      },
                    });
                  }}
                >
                  {portalMutation.isPending
                    ? 'Opening portal…'
                    : 'Manage payment method and invoices'}
                </Button>
              </div>
            ) : null}
            {manageable && !confirmCancel ? (
              <div className={styles.formActions}>
                <Button
                  type="button"
                  disabled={submitting}
                  onClick={() => setConfirmCancel(true)}
                >
                  Cancel renewal
                </Button>
              </div>
            ) : null}
            {manageable && confirmCancel ? (
              <div className={styles.formActions}>
                <Button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    if (submitting || !subscription) {
                      return;
                    }
                    setActionError(null);
                    cancelMutation.mutate(
                      {
                        subscriptionId: subscription.subscriptionId,
                        requestId: crypto.randomUUID(),
                      },
                      {
                        onSuccess: () => {
                          setConfirmCancel(false);
                        },
                        onError: (cause) => {
                          setActionError(cause);
                        },
                      },
                    );
                  }}
                >
                  {cancelMutation.isPending
                    ? 'Cancelling renewal…'
                    : 'Confirm cancel renewal'}
                </Button>
              </div>
            ) : null}
            {stripeManaged && lifecycle === 'CANCEL_AT_PERIOD_END' ? (
              <div className={styles.formActions}>
                <Button
                  type="button"
                  disabled={submitting}
                  onClick={() => {
                    if (submitting || !subscription) {
                      return;
                    }
                    setActionError(null);
                    reactivateMutation.mutate(
                      {
                        subscriptionId: subscription.subscriptionId,
                        requestId: crypto.randomUUID(),
                      },
                      {
                        onError: (cause) => {
                          setActionError(cause);
                        },
                      },
                    );
                  }}
                >
                  {reactivateMutation.isPending ? 'Reactivating…' : 'Reactivate'}
                </Button>
              </div>
            ) : null}
          </section>
        ) : null}

        {showCheckout ? (
          <form
            className={styles.pickerForm}
            onSubmit={(event) => {
              event.preventDefault();
              if (submitting) {
                return;
              }
              setActionError(null);
              const requestId = crypto.randomUUID();
              checkoutMutation.mutate(
                { requestId, cadence },
                {
                  onSuccess: (result) => {
                    rememberPendingAccountCheckout({
                      subscriptionId: result.subscriptionId,
                      checkoutSessionId: result.checkoutSessionId,
                    });
                    window.location.assign(result.checkoutUrl);
                  },
                  onError: (cause) => {
                    setActionError(cause);
                  },
                },
              );
            }}
          >
            <p className={styles.meta}>
              Start Individual Premium on the web (Stripe). No trial — payment method is charged
              when checkout completes. App Store and Google Play purchases are managed in those
              stores.
            </p>
            <fieldset className={`${styles.field} ${styles.cadenceOptions}`}>
              <legend className={styles.fieldLabel}>Billing cadence</legend>
              <label className={styles.cadenceOption}>
                <input
                  type="radio"
                  name="cadence"
                  checked={cadence === 'MONTHLY'}
                  onChange={() => setCadence('MONTHLY')}
                />
                Monthly
              </label>
              <label className={styles.cadenceOption}>
                <input
                  type="radio"
                  name="cadence"
                  checked={cadence === 'ANNUAL'}
                  onChange={() => setCadence('ANNUAL')}
                />
                Annual
              </label>
            </fieldset>
            <p className={styles.meta}>
              {formatUsd(price)} / {cadence === 'MONTHLY' ? 'month' : 'year'} plus applicable
              taxes.
            </p>
            <div className={styles.formActions}>
              <Button type="submit" disabled={submitting}>
                {submitting ? 'Starting checkout…' : 'Start Checkout'}
              </Button>
            </div>
          </form>
        ) : null}

        {visibleError ? (
          <ErrorView
            message={
              visibleError instanceof Error && !isApiError(visibleError)
                ? visibleError.message
                : accountBillingErrorMessage(visibleError, 'Unable to load billing.')
            }
          />
        ) : null}

        <Link to="/app/profile" className={styles.inlineLink}>
          Back to profile
        </Link>
      </div>
    </Page>
  );
}
