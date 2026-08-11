import type { PropsWithChildren, ReactNode } from 'react';

import styles from '@/features/home/components/HomeCard.module.scss';

interface HomeCardProps extends PropsWithChildren {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  className?: string;
}

export function HomeCard({ title, subtitle, actions, className, children }: HomeCardProps) {
  return (
    <article className={[styles.card, className].filter(Boolean).join(' ')}>
      <header className={styles.header}>
        <div>
          <h2 className={styles.title}>{title}</h2>
          {subtitle ? <p className={styles.subtitle}>{subtitle}</p> : null}
        </div>
        {actions}
      </header>
      <div className={styles.body}>{children}</div>
    </article>
  );
}
