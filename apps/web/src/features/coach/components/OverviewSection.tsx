import type { ReactNode } from 'react';

import { Badge } from '@/core/components/Badge';
import { consentScopeInfo } from '@/features/consent/models/scopes';
import type { CoachOverviewSectionStatus } from '@/features/coach/models/schemas';
import { coachSectionStatusMessage } from '@/features/coach/models/sectionCopy';
import styles from '@/features/coach/pages/CoachPages.module.scss';

interface OverviewSectionProps {
  title: string;
  status: CoachOverviewSectionStatus;
  children?: ReactNode;
}

export function OverviewSection({ title, status, children }: OverviewSectionProps) {
  const tone = status === 'AVAILABLE' ? 'success' : status === 'NOT_SHARED' ? 'muted' : 'neutral';

  return (
    <section className={styles.section} aria-label={title}>
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>{title}</h2>
        <Badge tone={tone}>{coachSectionStatusMessage(status)}</Badge>
      </div>
      {status === 'AVAILABLE' ? (
        <div className={styles.sectionBody}>{children}</div>
      ) : (
        <p className={styles.sectionStatusCopy}>{coachSectionStatusMessage(status)}</p>
      )}
    </section>
  );
}

export function RatingRow({
  label,
  rating,
}: {
  label: string;
  rating: { value: number; label: string } | null | undefined;
}) {
  if (!rating) {
    return (
      <div className={styles.stat}>
        <span className={styles.statLabel}>{label}</span>
        <span className={styles.statValue}>—</span>
      </div>
    );
  }
  return (
    <div className={styles.stat}>
      <span className={styles.statLabel}>{label}</span>
      <span className={styles.statValue}>
        {rating.label} ({rating.value})
      </span>
    </div>
  );
}

export function ScopeChips({ scopes }: { scopes: string[] }) {
  if (scopes.length === 0) {
    return <p className={styles.meta}>No effective scopes for this membership.</p>;
  }
  return (
    <div className={styles.scopeChips} aria-label="Effective scopes">
      {scopes.map((scope) => (
        <Badge key={scope} tone="info">
          {consentScopeInfo(scope).label}
        </Badge>
      ))}
    </div>
  );
}
