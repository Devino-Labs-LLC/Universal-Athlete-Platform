import { Linking, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Button } from '@/src/core/components/PrimaryButton';
import { Screen } from '@/src/core/components/Screen';
import { useMarketplaceActions, useMarketplaceListing } from '@/src/features/marketplace/hooks/useMarketplace';

interface MarketplaceListingScreenProps {
  listingId: string;
}

export function MarketplaceListingScreen({ listingId }: MarketplaceListingScreenProps) {
  const theme = useAppTheme();
  const listingQuery = useMarketplaceListing(listingId);
  const actions = useMarketplaceActions();

  if (listingQuery.isLoading) {
    return (
      <Screen title="Listing">
        <Text style={{ color: theme.colors.textSecondary }}>Loading listing…</Text>
      </Screen>
    );
  }
  if (listingQuery.isError || !listingQuery.data) {
    return (
      <Screen title="Listing">
        <Text style={{ color: theme.colors.textSecondary }}>This listing is unavailable.</Text>
      </Screen>
    );
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
    await Linking.openURL(listing.destinationUrl);
  };

  return (
    <Screen title={listing.title} scroll testID="marketplace-listing-detail">
      <Text style={{ color: theme.colors.textSecondary }}>{listing.partnerDisplayName}</Text>
      <Text style={{ color: theme.colors.textPrimary }}>{listing.fullDescription}</Text>
      {listing.priceDisplay ? <Text style={{ color: theme.colors.textPrimary }}>{listing.priceDisplay}</Text> : null}
      {listing.offerActive && listing.promoCode ? (
        <Text style={{ color: theme.colors.textPrimary }}>Code: {listing.promoCode}</Text>
      ) : null}
      {!listing.available ? (
        <Text style={{ color: theme.colors.textSecondary }}>This listing is no longer available.</Text>
      ) : null}
      <Button
        variant="secondary"
        label={listing.saved ? 'Remove save' : 'Save listing'}
        onPress={() => void (listing.saved ? actions.unsave(listing.id) : actions.save(listing.id))}
      />
      {listing.destinationUrl ? (
        <Button
          label={listing.offerActive ? 'Redeem offer' : 'Visit partner'}
          onPress={() => void visit()}
        />
      ) : null}
    </Screen>
  );
}
