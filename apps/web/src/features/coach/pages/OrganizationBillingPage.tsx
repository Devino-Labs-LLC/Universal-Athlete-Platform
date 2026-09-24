import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { isApiError } from '@/core/api/errors';
import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { Page } from '@/core/components/Page';
import {
  cancelOrganizationRenewal,
  changeOrganizationPlan,
  createOrganizationCheckoutSession,
  createOrganizationPortalSession,
  fetchOrganizationBillingStatus,
  fetchOrganizationCapacity,
  reactivateOrganizationSubscription,
} from '@/features/coach/api/billingApi';
import {
  organizationCatalog,
  type OrganizationBillingStatus,
  type OrganizationCapacitySnapshot,
} from '@/features/coach/models/billingCatalog';
import { coachErrorMessage } from '@/features/coach/models/errors';
import styles from '@/features/coach/pages/CoachPages.module.scss';

function ownerUsageCopy(snapshot: OrganizationCapacitySnapshot): string {
  if (snapshot.bandCapacity == null) {
    return `${snapshot.activeAthleteCount} active athletes. No current Organization plan band applies.`;
  }
  return `${snapshot.activeAthleteCount} of ${snapshot.bandCapacity} active athletes`;
}

function planLabel(planKey: string): string {
  return organizationCatalog.find((tier) => tier.planKey === planKey)?.name ?? planKey;
}

function capacityConflictCopy(
  snapshot: OrganizationCapacitySnapshot,
  planKey: string,
): string {
  const tier = organizationCatalog.find((item) => item.planKey === planKey);
  if (!tier) {
    return 'The selected plan does not cover the current active athletes.';
  }
  return `Your organization currently has ${snapshot.activeAthleteCount} active athletes. Reduce active usage to ${tier.capacity} or fewer before selecting ${tier.name}.`;
}

function isMissingSubscription(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND';
}

type OrganizationPlanKey = (typeof organizationCatalog)[number]['planKey'];

function isOrganizationPlanKey(value: string): value is OrganizationPlanKey {
  return organizationCatalog.some((tier) => tier.planKey === value);
}

function planFits(planKey: OrganizationPlanKey, snapshot: OrganizationCapacitySnapshot | null): boolean {
  if (snapshot == null) {
    return true;
  }
  const tier = organizationCatalog.find((item) => item.planKey === planKey);
  return tier != null && snapshot.activeAthleteCount <= tier.capacity;
}

function smallestFittingPlan(snapshot: OrganizationCapacitySnapshot): OrganizationPlanKey | null {
  return organizationCatalog.find((tier) => snapshot.activeAthleteCount <= tier.capacity)?.planKey ?? null;
}

export function OrganizationBillingPage() {
  const { organizationId } = useParams<{ organizationId: string }>();
  const { apiClient } = useAuthSession();
  const [planKey, setPlanKey] = useState<(typeof organizationCatalog)[number]['planKey']>('ORG_BAND_25');
  const [cadence, setCadence] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [loadError, setLoadError] = useState<unknown>(null);
  const [actionError, setActionError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [capacity, setCapacity] = useState<OrganizationCapacitySnapshot | null>(null);
  const [subscription, setSubscription] = useState<OrganizationBillingStatus | null>(null);
  const [loaded, setLoaded] = useState(false);
  const seededOrganizationId = useRef<string | null>(null);

  useEffect(() => {
    if (!organizationId) {
      return;
    }
    let cancelled = false;
    void Promise.allSettled([
      fetchOrganizationCapacity(apiClient, organizationId),
      fetchOrganizationBillingStatus(apiClient, organizationId),
    ]).then(([capacityResult, statusResult]) => {
      if (cancelled) {
        return;
      }
      const snapshot = capacityResult.status === 'fulfilled' ? capacityResult.value : null;
      setCapacity(snapshot);
      let nextPlan: OrganizationPlanKey = 'ORG_BAND_25';
      let nextCadence: 'MONTHLY' | 'ANNUAL' = 'MONTHLY';
      if (statusResult.status === 'fulfilled') {
        setSubscription(statusResult.value);
        setLoadError(null);
        if (isOrganizationPlanKey(statusResult.value.planKey)) {
          nextPlan = statusResult.value.planKey;
        }
        if (statusResult.value.cadence === 'MONTHLY' || statusResult.value.cadence === 'ANNUAL') {
          nextCadence = statusResult.value.cadence;
        }
      } else if (isMissingSubscription(statusResult.reason)) {
        setSubscription(null);
        setLoadError(null);
        if (snapshot != null && !planFits(nextPlan, snapshot)) {
          nextPlan = smallestFittingPlan(snapshot) ?? nextPlan;
        }
      } else {
        setSubscription(null);
        setLoadError(statusResult.reason);
      }
      if (seededOrganizationId.current !== organizationId) {
        setPlanKey(nextPlan);
        setCadence(nextCadence);
        seededOrganizationId.current = organizationId;
      }
      setLoaded(true);
    });
    return () => {
      cancelled = true;
    };
  }, [apiClient, organizationId]);

  const selected = organizationCatalog.find((tier) => tier.planKey === planKey) ?? organizationCatalog[0];
  const price = cadence === 'MONTHLY' ? selected.monthlyUsd : selected.annualUsd;
  const lifecycle = subscription?.lifecycleState;
  const manageable = lifecycle === 'ACTIVE' || lifecycle === 'TRIALING';
  const showCheckout = loaded && (subscription == null || lifecycle === 'EXPIRED') && !isApiError(loadError);
  const showPortal = subscription != null && lifecycle !== 'PENDING';
  const cadenceOnly = manageable && subscription != null && planKey === subscription.planKey;
  const selectionFits = cadenceOnly || planFits(planKey, capacity);

  function report(cause: unknown, targetPlan?: string) {
    if (
      isApiError(cause) &&
      cause.code === 'ORGANIZATION_PLAN_CAPACITY_CONFLICT' &&
      capacity &&
      targetPlan
    ) {
      setActionError(new Error(capacityConflictCopy(capacity, targetPlan)));
      return;
    }
    setActionError(cause);
  }

  const visibleError = actionError ?? loadError;

  return (
    <Page
      title="Organization billing"
      description="Prices are shown before applicable taxes."
    >
      {capacity ? <p className={styles.meta}>{ownerUsageCopy(capacity)}</p> : null}
      {subscription ? (
        <section className={styles.pickerForm}>
          <p className={styles.meta}>
            {planLabel(subscription.planKey)} · {subscription.cadence === 'ANNUAL' ? 'Annual' : 'Monthly'} ·{' '}
            {subscription.lifecycleState}
          </p>
          {subscription.trialEndsAt ? (
            <p className={styles.meta}>Trial ends {subscription.trialEndsAt}</p>
          ) : null}
          {subscription.currentPeriodEndsAt ? (
            <p className={styles.meta}>Current period ends {subscription.currentPeriodEndsAt}</p>
          ) : null}
          {lifecycle === 'PENDING' ? <p className={styles.meta}>Checkout is already in progress.</p> : null}
          {showPortal ? (
            <Button
              type="button"
              disabled={submitting || !organizationId}
              onClick={() => {
                if (!organizationId || submitting) {
                  return;
                }
                setSubmitting(true);
                setActionError(null);
                void createOrganizationPortalSession(apiClient, organizationId)
                  .then((result) => {
                    window.location.assign(result.url);
                  })
                  .catch((cause) => {
                    report(cause);
                    setSubmitting(false);
                  });
              }}
            >
              Manage payment method and invoices
            </Button>
          ) : null}
          {manageable ? (
            <form
              onSubmit={(event) => {
                event.preventDefault();
                if (!organizationId || !subscription || submitting || !selectionFits) {
                  return;
                }
                setSubmitting(true);
                setActionError(null);
                void changeOrganizationPlan(apiClient, organizationId, subscription.subscriptionId, {
                  requestId: crypto.randomUUID(),
                  targetPlanKey: planKey,
                  targetCadence: cadence,
                })
                  .then((updated) => {
                    setSubscription(updated);
                    setSubmitting(false);
                  })
                  .catch((cause) => {
                    report(cause, planKey);
                    setSubmitting(false);
                  });
              }}
            >
              <label className={styles.field}>
                <span className={styles.fieldLabel}>Change plan</span>
                <select
                  className={styles.select}
                  value={planKey}
                  onChange={(event) =>
                    setPlanKey(event.target.value as (typeof organizationCatalog)[number]['planKey'])
                  }
                >
                  {organizationCatalog.map((tier) => (
                    <option
                      key={tier.planKey}
                      value={tier.planKey}
                      disabled={
                        capacity != null &&
                        capacity.activeAthleteCount > tier.capacity &&
                        tier.planKey !== subscription.planKey
                      }
                    >
                      {tier.name} — ${tier.monthlyUsd}/mo or ${tier.annualUsd}/yr
                    </option>
                  ))}
                </select>
              </label>
              <fieldset className={styles.field}>
                <legend className={styles.fieldLabel}>Billing cadence</legend>
                <label>
                  <input
                    type="radio"
                    name="manage-cadence"
                    checked={cadence === 'MONTHLY'}
                    onChange={() => setCadence('MONTHLY')}
                  />{' '}
                  Monthly
                </label>
                <label>
                  <input
                    type="radio"
                    name="manage-cadence"
                    checked={cadence === 'ANNUAL'}
                    onChange={() => setCadence('ANNUAL')}
                  />{' '}
                  Annual
                </label>
              </fieldset>
              <p className={styles.meta}>
                ${price} / {cadence === 'MONTHLY' ? 'month' : 'year'} plus applicable taxes.
              </p>
              <Button type="submit" disabled={submitting || !selectionFits}>
                {submitting ? 'Updating plan…' : 'Change plan'}
              </Button>
            </form>
          ) : null}
          {manageable && !confirmCancel ? (
            <Button type="button" disabled={submitting} onClick={() => setConfirmCancel(true)}>
              Cancel renewal
            </Button>
          ) : null}
          {manageable && confirmCancel && organizationId && subscription ? (
            <Button
              type="button"
              disabled={submitting}
              onClick={() => {
                setSubmitting(true);
                setActionError(null);
                void cancelOrganizationRenewal(
                  apiClient,
                  organizationId,
                  subscription.subscriptionId,
                  crypto.randomUUID(),
                )
                  .then((updated) => {
                    setSubscription(updated);
                    setConfirmCancel(false);
                    setSubmitting(false);
                  })
                  .catch((cause) => {
                    report(cause);
                    setSubmitting(false);
                  });
              }}
            >
              Confirm cancel renewal
            </Button>
          ) : null}
          {lifecycle === 'CANCEL_AT_PERIOD_END' && organizationId && subscription ? (
            <Button
              type="button"
              disabled={submitting}
              onClick={() => {
                setSubmitting(true);
                setActionError(null);
                void reactivateOrganizationSubscription(
                  apiClient,
                  organizationId,
                  subscription.subscriptionId,
                  crypto.randomUUID(),
                )
                  .then((updated) => {
                    setSubscription(updated);
                    setSubmitting(false);
                  })
                  .catch((cause) => {
                    report(cause);
                    setSubmitting(false);
                  });
              }}
            >
              Reactivate
            </Button>
          ) : null}
        </section>
      ) : null}
      {showCheckout ? (
        <form
          className={styles.pickerForm}
          onSubmit={(event) => {
            event.preventDefault();
            if (!organizationId || submitting || !selectionFits) {
              return;
            }
            setSubmitting(true);
            setActionError(null);
            void createOrganizationCheckoutSession(apiClient, organizationId, {
              requestId: crypto.randomUUID(),
              planKey,
              cadence,
            })
              .then((result) => {
                window.location.assign(result.checkoutUrl);
              })
              .catch((cause) => {
                report(cause, planKey);
                setSubmitting(false);
              });
          }}
        >
          <p className={styles.meta}>Start a 14-day Organization trial.</p>
          <label className={styles.field}>
            <span className={styles.fieldLabel}>Plan</span>
            <select
              className={styles.select}
              value={planKey}
              onChange={(event) =>
                setPlanKey(event.target.value as (typeof organizationCatalog)[number]['planKey'])
              }
            >
              {organizationCatalog.map((tier) => (
                <option
                  key={tier.planKey}
                  value={tier.planKey}
                  disabled={capacity != null && capacity.activeAthleteCount > tier.capacity}
                >
                  {tier.name} — up to {tier.capacity} athletes
                </option>
              ))}
            </select>
          </label>
          <fieldset className={styles.field}>
            <legend className={styles.fieldLabel}>Billing cadence</legend>
            <label>
              <input
                type="radio"
                name="cadence"
                checked={cadence === 'MONTHLY'}
                onChange={() => setCadence('MONTHLY')}
              />{' '}
              Monthly
            </label>
            <label>
              <input
                type="radio"
                name="cadence"
                checked={cadence === 'ANNUAL'}
                onChange={() => setCadence('ANNUAL')}
              />{' '}
              Annual
            </label>
          </fieldset>
          <p className={styles.meta}>
            ${price} / {cadence === 'MONTHLY' ? 'month' : 'year'} plus applicable taxes. 14-day trial.
            Payment method required up front.
          </p>
          <div className={styles.formActions}>
            <Button type="submit" disabled={submitting || !organizationId || !selectionFits}>
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
              : coachErrorMessage(visibleError, 'Unable to update billing.')
          }
        />
      ) : null}
      <Link to="/coach" className={styles.inlineLink}>
        Back to coach home
      </Link>
    </Page>
  );
}
