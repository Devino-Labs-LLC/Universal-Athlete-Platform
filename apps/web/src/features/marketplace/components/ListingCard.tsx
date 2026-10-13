import { Link } from 'react-router-dom';

import type { MarketplaceListing } from '@/features/marketplace/api/marketplaceApi';
import styles from '@/features/marketplace/components/ListingCard.module.scss';

interface ListingCardProps {
  listing: MarketplaceListing;
}

export function ListingCard({ listing }: ListingCardProps) {
  return (
    <Link
      to={`/app/marketplace/${listing.id}`}
      className={[styles.card, listing.available ? '' : styles.unavailable].filter(Boolean).join(' ')}
      data-testid="marketplace-listing-card"
      aria-label={`Marketplace listing: ${listing.title}`}
    >
      <p className={styles.kicker}>{listing.featured ? 'Featured partner listing' : 'Partner listing'}</p>
      <p className={styles.partner}>{listing.partnerDisplayName}</p>
      <p className={styles.title}>{listing.title}</p>
      <p className={styles.body}>{listing.shortDescription}</p>
      {listing.priceDisplay ? <p className={styles.meta}>{listing.priceDisplay}</p> : null}
      {listing.offerActive && listing.discountText ? <p className={styles.meta}>{listing.discountText}</p> : null}
      {!listing.available ? <p className={styles.meta}>Unavailable</p> : null}
    </Link>
  );
}
