import type { SponsorshipPlacement } from '@/features/sponsorship/api/sponsorshipApi';
import styles from '@/features/sponsorship/components/SponsorCard.module.scss';

interface SponsorCardProps {
  placement: SponsorshipPlacement;
  onVisit?: () => void;
}

export function SponsorCard({ placement, onVisit }: SponsorCardProps) {
  return (
    <aside className={styles.card} data-testid="sponsor-card" aria-label="Sponsored partner content">
      <p className={styles.label}>{placement.label}</p>
      <p className={styles.sponsor}>{placement.sponsorDisplayName}</p>
      <p className={styles.headline}>{placement.headline}</p>
      <p className={styles.body}>{placement.body}</p>
      {placement.creativeUrl ? (
        <img
          className={styles.creative}
          src={placement.creativeUrl}
          alt=""
          onError={(event) => {
            event.currentTarget.style.display = 'none';
          }}
        />
      ) : null}
      <a
        className={styles.cta}
        href={placement.destinationUrl}
        target="_blank"
        rel="noopener noreferrer"
        onClick={() => onVisit?.()}
      >
        {placement.ctaLabel}
      </a>
    </aside>
  );
}
