import { Outlet } from 'react-router-dom';

import { OnboardingStepper } from '@/features/onboarding/components/OnboardingStepper';

import styles from '@/features/onboarding/layout/OnboardingLayout.module.scss';

export function OnboardingLayout() {
  return (
    <div className={styles.layout}>
      <div className={styles.container}>
        <OnboardingStepper />
        <main className={styles.main}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
