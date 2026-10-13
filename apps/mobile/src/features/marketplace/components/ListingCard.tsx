import { Pressable, StyleSheet, Text } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import type { MarketplaceListing } from '@/src/features/marketplace/api/marketplaceApi';

interface ListingCardProps {
  listing: MarketplaceListing;
  onPress: () => void;
}

export function ListingCard({ listing, onPress }: ListingCardProps) {
  const theme = useAppTheme();
  return (
    <Pressable
      testID="marketplace-listing-card"
      accessibilityRole="button"
      accessibilityLabel={`Marketplace listing: ${listing.title}`}
      onPress={onPress}
      style={[styles.card, { borderColor: theme.colors.border, backgroundColor: theme.colors.surface }]}
    >
      <Text style={[styles.kicker, { color: theme.colors.textSecondary }]}>
        {listing.featured ? 'Featured partner listing' : 'Partner listing'}
      </Text>
      <Text style={[styles.partner, { color: theme.colors.textSecondary }]}>{listing.partnerDisplayName}</Text>
      <Text style={[styles.title, { color: theme.colors.textPrimary }]}>{listing.title}</Text>
      <Text style={[styles.body, { color: theme.colors.textSecondary }]}>{listing.shortDescription}</Text>
      {listing.priceDisplay ? (
        <Text style={{ color: theme.colors.textPrimary }}>{listing.priceDisplay}</Text>
      ) : null}
      {!listing.available ? <Text style={{ color: theme.colors.textSecondary }}>Unavailable</Text> : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    gap: 6,
  },
  kicker: {
    fontSize: 11,
    letterSpacing: 0.8,
    textTransform: 'uppercase',
  },
  partner: {
    fontSize: 13,
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
  },
  body: {
    fontSize: 14,
  },
});
