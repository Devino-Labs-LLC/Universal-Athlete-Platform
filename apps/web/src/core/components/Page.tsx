import type { PropsWithChildren, ReactNode } from 'react';

import styles from '@/core/components/Page.module.scss';

interface PageProps extends PropsWithChildren {
  title: string;
  description?: string;
  actions?: ReactNode;
  /** Wider content rail for dense operational surfaces (builder, calendar). */
  width?: 'default' | 'wide';
  /** Extra inset for pages rendered outside AppShell. */
  padded?: boolean;
}

export function Page({
  title,
  description,
  actions,
  children,
  width = 'default',
  padded = false,
}: PageProps) {
  return (
    <section
      className={[
        styles.page,
        width === 'wide' ? styles.wide : '',
        padded ? styles.padded : '',
      ]
        .filter(Boolean)
        .join(' ')}
      aria-labelledby="page-title"
    >
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
