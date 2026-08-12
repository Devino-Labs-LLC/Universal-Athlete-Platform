import type { PropsWithChildren } from 'react';

import styles from '@/features/training/components/MetricPill.module.scss';

interface MetricPillProps extends PropsWithChildren {
  label?: string;
}

export function MetricPill({ label, children }: MetricPillProps) {
  return (
    <span className={styles.pill}>
      {label ? <span className={styles.label}>{label}</span> : null}
      <span className={styles.value}>{children}</span>
    </span>
  );
}
