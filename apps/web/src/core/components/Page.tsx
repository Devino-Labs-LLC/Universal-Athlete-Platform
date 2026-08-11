import type { PropsWithChildren, ReactNode } from 'react';

import styles from '@/core/components/Page.module.scss';

interface PageProps extends PropsWithChildren {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function Page({ title, description, actions, children }: PageProps) {
  return (
    <section className={styles.page} aria-labelledby="page-title">
      <header>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem' }}>
          <h1 id="page-title" className={styles.pageTitle}>
            {title}
          </h1>
          {actions}
        </div>
        {description ? <p className={styles.pageDescription}>{description}</p> : null}
      </header>
      <div className={styles.pageContent}>{children}</div>
    </section>
  );
}
