import { Link, useParams } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { useMarketplaceActions, useMarketplaceListing } from '@/features/marketplace/hooks/useMarketplace';
import styles from '@/features/marketplace/pages/MarketplacePage.module.scss';

export function MarketplaceListingPage() {
  const { listingId } = useParams();
  const listingQuery = useMarketplaceListing(listingId);
  const actions = useMarketplaceActions();

  if (listingQuery.isLoading) {
    return <LoadingView message="Loading listing…" />;
  }
  if (listingQuery.isError || !listingQuery.data) {
    return <ErrorView message="This listing is unavailable." />;
  }

  const listing = listingQuery.data;
  const visit = async () => {
    if (!listing.destinationUrl) {
      return;
    }
    await actions.recordClick(listing.id);
    if (listing.offerActive) {
      await actions.recordRedeem(listing.id);
    }
    window.open(listing.destinationUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <Page title={listing.title} description="Partner marketplace listing. Not a readiness recommendation.">
      <div className={styles.detail} data-testid="marketplace-listing-detail">
        <p>Partner: {listing.partnerDisplayName}</p>
        <p>
          {listing.categoryName} · {listing.listingType}
        </p>
        <p>{listing.fullDescription}</p>
        {listing.priceDisplay ? <p>{listing.priceDisplay}</p> : null}
        {listing.offerActive ? (
          <div className={styles.offer}>
            {listing.discountText ? <p>{listing.discountText}</p> : null}
            {listing.promoCode ? <p>Code: {listing.promoCode}</p> : null}
          </div>
        ) : null}
        {!listing.available ? <p>This listing is no longer available.</p> : null}
        <div className={styles.actions}>
          {listing.saved ? (
            <Button type="button" variant="secondary" onClick={() => void actions.unsave(listing.id)}>
              Remove save
            </Button>
          ) : (
            <Button type="button" variant="secondary" onClick={() => void actions.save(listing.id)}>
              Save listing
            </Button>
          )}
          {listing.destinationUrl ? (
            <Button type="button" onClick={() => void visit()}>
              {listing.offerActive ? 'Redeem offer' : 'Visit partner'}
            </Button>
          ) : null}
          <Link to="/app/marketplace">Back to marketplace</Link>
        </div>
      </div>
    </Page>
  );
}
