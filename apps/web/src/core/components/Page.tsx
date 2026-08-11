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
        <div className={styles.headerRow}>
          <h1 id="page-title" className={styles.pageTitle}>
            {title}
          </h1>
          {actions ? <div className={styles.pageActions}>{actions}</div> : null}
        </div>
        {description ? <p className={styles.pageDescription}>{description}</p> : null}
      </header>
      <div className={styles.pageContent}>{children}</div>
    </section>
  );
}
