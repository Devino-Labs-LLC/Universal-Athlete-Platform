import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { Page } from '@/core/components/Page';
import {
  createOrganizationCheckoutSession,
  fetchOrganizationCapacity,
} from '@/features/coach/api/billingApi';
import {
  organizationCatalog,
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

export function OrganizationBillingPage() {
  const { organizationId } = useParams<{ organizationId: string }>();
  const { apiClient } = useAuthSession();
  const [planKey, setPlanKey] = useState<(typeof organizationCatalog)[number]['planKey']>('ORG_BAND_25');
  const [cadence, setCadence] = useState<'MONTHLY' | 'ANNUAL'>('MONTHLY');
  const [error, setError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);
  const [capacity, setCapacity] = useState<OrganizationCapacitySnapshot | null>(null);

  useEffect(() => {
    if (!organizationId) {
      return;
    }
    let cancelled = false;
    void fetchOrganizationCapacity(apiClient, organizationId)
      .then((snapshot) => {
        if (!cancelled) {
          setCapacity(snapshot);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCapacity(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [apiClient, organizationId]);

  const selected = organizationCatalog.find((tier) => tier.planKey === planKey) ?? organizationCatalog[0];
  const price = cadence === 'MONTHLY' ? selected.monthlyUsd : selected.annualUsd;

  return (
    <Page
      title="Organization billing"
      description="Start a 14-day Organization trial. Prices are shown before applicable taxes."
    >
      {capacity ? <p className={styles.meta}>{ownerUsageCopy(capacity)}</p> : null}
      <form
        className={styles.pickerForm}
        onSubmit={(event) => {
          event.preventDefault();
          if (!organizationId || submitting) {
            return;
          }
          setSubmitting(true);
          setError(null);
          void createOrganizationCheckoutSession(apiClient, organizationId, {
            requestId: crypto.randomUUID(),
            planKey,
            cadence,
          })
            .then((result) => {
              window.location.assign(result.checkoutUrl);
            })
            .catch((cause) => {
              setError(cause);
              setSubmitting(false);
            });
        }}
      >
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
              <option key={tier.planKey} value={tier.planKey}>
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
        {error ? <ErrorView message={coachErrorMessage(error, 'Unable to start checkout.')} /> : null}
        <div className={styles.formActions}>
          <Button type="submit" disabled={submitting || !organizationId}>
            {submitting ? 'Starting checkout…' : 'Start Checkout'}
          </Button>
          <Link to="/coach" className={styles.inlineLink}>
            Back to coach home
          </Link>
        </div>
      </form>
    </Page>
  );
}
