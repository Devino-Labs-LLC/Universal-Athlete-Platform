import { Suspense } from 'react';
import { Outlet } from 'react-router-dom';

import { LoadingView } from '@/core/components/LoadingView';
import { OnboardingStepper } from '@/features/onboarding/components/OnboardingStepper';

import styles from '@/features/onboarding/layout/OnboardingLayout.module.scss';

export function OnboardingLayout() {
  return (
    <div className={styles.layout}>
      <div className={styles.container}>
        <OnboardingStepper />
        <main className={styles.main}>
          <Suspense fallback={<LoadingView message="Loading onboarding step…" />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  );
}
