import { Link } from 'react-router-dom';

import { Page } from '@/core/components/Page';
import styles from '@/features/coach/pages/CoachPages.module.scss';

export function BillingCheckoutSuccessPage() {
  return (
    <Page
      title="Checkout received"
      description="Payment setup received. We’re confirming your subscription."
    >
      <p className={styles.meta}>
        This page does not activate billing by itself. Organization access updates after the provider
        confirms the subscription.
      </p>
      <p>
        <Link to="/coach" className={styles.inlineLink}>
          Return to coach home
        </Link>
      </p>
    </Page>
  );
}

export function BillingCheckoutCancelPage() {
  return (
    <Page title="Checkout canceled" description="Checkout was not completed.">
      <p>
        <Link to="/coach" className={styles.inlineLink}>
          Return to coach home
        </Link>
      </p>
    </Page>
  );
}
