import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import {
  clearPendingAccountCheckout,
  readPendingAccountCheckout,
} from '@/features/billing/api/accountBillingApi';
import { useSyncAccountSubscriptionMutation } from '@/features/billing/hooks/useAccountBilling';
import { accountBillingErrorMessage } from '@/features/billing/models/errors';
import styles from '@/features/billing/pages/AccountBillingPage.module.scss';

function resolveCheckoutIds(searchParams: URLSearchParams): {
  subscriptionId: string | null;
  checkoutSessionId: string | null;
} {
  const pending = readPendingAccountCheckout();
  const checkoutSessionId =
    searchParams.get('session_id')?.trim() ||
    searchParams.get('checkoutSessionId')?.trim() ||
    pending?.checkoutSessionId ||
    null;
  const subscriptionId =
    searchParams.get('subscriptionId')?.trim() ||
    searchParams.get('subscription_id')?.trim() ||
    pending?.subscriptionId ||
    null;
  return { subscriptionId, checkoutSessionId };
}

export function AccountBillingCheckoutSuccessPage() {
  const [searchParams] = useSearchParams();
  const { mutate, isPending, isSuccess, isError, error } = useSyncAccountSubscriptionMutation();
  const started = useRef(false);
  const [missingParams, setMissingParams] = useState(false);
  const { subscriptionId, checkoutSessionId } = resolveCheckoutIds(searchParams);

  useEffect(() => {
    if (started.current) {
      return;
    }
    started.current = true;

    if (!subscriptionId || !checkoutSessionId) {
      setMissingParams(true);
      return;
    }

    mutate(
      { subscriptionId, checkoutSessionId },
      {
        onSettled: () => {
          clearPendingAccountCheckout();
        },
      },
    );
  }, [checkoutSessionId, mutate, subscriptionId]);

  return (
    <Page
      title="Checkout received"
      description="Payment setup received. We’re confirming your subscription."
    >
      <div className={styles.stack}>
        {isPending ? <LoadingView message="Confirming subscription…" /> : null}

        {missingParams ? (
          <p className={styles.meta}>
            Checkout returned without enough information to confirm billing. Open Premium billing
            to check your current status.
          </p>
        ) : null}

        {isSuccess ? (
          <p className={styles.meta}>
            Confirmation request completed. Premium access updates after the provider confirms the
            subscription. This page does not activate billing by itself.
          </p>
        ) : null}

        {isError ? (
          <ErrorView
            message={accountBillingErrorMessage(
              error,
              'Unable to confirm checkout. Open Premium billing to check status.',
            )}
          />
        ) : null}

        <p>
          <Link to="/app/billing" className={styles.inlineLink}>
            Return to Premium billing
          </Link>
        </p>
        <p>
          <Link to="/app/profile" className={styles.inlineLink}>
            Back to profile
          </Link>
        </p>
      </div>
    </Page>
  );
}

export function AccountBillingCheckoutCancelPage() {
  useEffect(() => {
    clearPendingAccountCheckout();
  }, []);

  return (
    <Page title="Checkout canceled" description="Checkout was not completed.">
      <div className={styles.stack}>
        <p className={styles.meta}>No subscription change was made.</p>
        <p>
          <Link to="/app/billing" className={styles.inlineLink}>
            Return to Premium billing
          </Link>
        </p>
        <p>
          <Link to="/app/profile" className={styles.inlineLink}>
            Back to profile
          </Link>
        </p>
      </div>
    </Page>
  );
}
