import { useLocalSearchParams } from 'expo-router';

import { MarketplaceListingScreen } from '@/src/features/marketplace/screens/MarketplaceListingScreen';

export default function MarketplaceListingRoute() {
  const { listingId } = useLocalSearchParams<{ listingId: string }>();
  return <MarketplaceListingScreen listingId={listingId ?? ''} />;
}
