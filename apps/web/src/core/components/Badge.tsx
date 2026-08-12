import type { PropsWithChildren } from 'react';

import styles from '@/core/components/Badge.module.scss';

export type BadgeTone = 'neutral' | 'accent' | 'muted' | 'success' | 'warning' | 'danger' | 'info';

interface BadgeProps extends PropsWithChildren {
  tone?: BadgeTone;
}

export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return <span className={[styles.badge, styles[tone]].join(' ')}>{children}</span>;
}
