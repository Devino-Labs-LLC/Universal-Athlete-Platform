import { SponsorCard } from '@/src/features/sponsorship/components/SponsorCard';
import { usePlacement } from '@/src/features/sponsorship/hooks/usePlacement';
import type { PlacementSurface } from '@/src/features/sponsorship/api/sponsorshipApi';

export function SponsorPlacementSlot({ surface }: { surface: PlacementSurface }) {
  const placement = usePlacement(surface);
  if (placement.isLoading || placement.isError || !placement.data) {
    return null;
  }
  return <SponsorCard placement={placement.data} onVisit={() => placement.recordClick()} />;
}
