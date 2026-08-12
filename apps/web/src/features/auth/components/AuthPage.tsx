import type { PropsWithChildren } from 'react';

import styles from '@/features/auth/components/AuthPage.module.scss';

interface AuthPageProps extends PropsWithChildren {
  title: string;
  description?: string;
}

/**
 * Centered public authentication shell (not AppShell).
 * Desktop: viewport-centered card ~480px; stacks naturally on small screens.
 */
export function AuthPage({ title, description, children }: AuthPageProps) {
  return (
    <div className={styles.viewport}>
      <div className={styles.brandRow}>
        <span className={styles.brandMark}>UAP</span>
        <span className={styles.brandName}>Universal Athlete</span>
      </div>
      <section className={styles.card} aria-labelledby="auth-page-title">
        <header className={styles.header}>
          <h1 id="auth-page-title" className={styles.title}>
            {title}
          </h1>
          {description ? <p className={styles.description}>{description}</p> : null}
        </header>
        <div className={styles.content}>{children}</div>
      </section>
    </div>
  );
}
