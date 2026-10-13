import { Href, router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';
import { ListingCard } from '@/src/features/marketplace/components/ListingCard';
import {
  useMarketplaceActions,
  useMarketplaceBrowse,
  useMarketplaceCategories,
} from '@/src/features/marketplace/hooks/useMarketplace';

export function MarketplaceScreen() {
  const theme = useAppTheme();
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<string | undefined>(undefined);
  const categories = useMarketplaceCategories();
  const browse = useMarketplaceBrowse({ category, q: query || undefined });
  const { recordImpression } = useMarketplaceActions();
  const impressed = useRef(new Set<string>());

  useEffect(() => {
    for (const listing of browse.data?.listings ?? []) {
      if (!impressed.current.has(listing.id)) {
        impressed.current.add(listing.id);
        void recordImpression(listing.id);
      }
    }
  }, [recordImpression, browse.data?.listings]);

  if (browse.isLoading) {
    return (
      <Screen title="Marketplace">
        <Text style={{ color: theme.colors.textSecondary }}>Loading marketplace…</Text>
      </Screen>
    );
  }
  if (browse.isError) {
    return (
      <Screen title="Marketplace">
        <Text style={{ color: theme.colors.textSecondary }}>Marketplace is not available right now.</Text>
      </Screen>
    );
  }

  const listings = browse.data?.listings ?? [];

  return (
    <Screen title="Marketplace" scroll>
      <Text style={{ color: theme.colors.textSecondary }}>
        Curated partner listings. This is not athlete intelligence.
      </Text>
      <TextInput
        accessibilityLabel="Search marketplace"
        value={search}
        onChangeText={setSearch}
        onSubmitEditing={() => setQuery(search)}
        placeholder="Search listings"
        style={{
          borderWidth: 1,
          borderColor: theme.colors.border,
          color: theme.colors.textPrimary,
          padding: 10,
          borderRadius: 8,
        }}
      />
      <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
        <Pressable onPress={() => setCategory(undefined)}>
          <Text style={{ color: theme.colors.textPrimary }}>All</Text>
        </Pressable>
        {(categories.data ?? []).map((item) => (
          <Pressable
            key={item.code}
            accessibilityLabel={`Filter ${item.name}`}
            onPress={() => setCategory(item.code)}
          >
            <Text style={{ color: theme.colors.textPrimary }}>{item.name}</Text>
          </Pressable>
        ))}
      </View>
      {listings.length === 0 ? (
        <Text style={{ color: theme.colors.textSecondary }}>No marketplace listings match these filters.</Text>
      ) : (
        listings.map((listing) => (
          <ListingCard
            key={listing.id}
            listing={listing}
            onPress={() => router.push(`/(tabs)/profile/marketplace/${listing.id}` as Href)}
          />
        ))
      )}
    </Screen>
  );
}
