import { SponsorCard } from '@/features/sponsorship/components/SponsorCard';
import { usePlacement } from '@/features/sponsorship/hooks/useSponsorship';
import type { PlacementSurface } from '@/features/sponsorship/api/sponsorshipApi';

interface SponsorPlacementSlotProps {
  surface: PlacementSurface;
  teamId?: string;
}

export function SponsorPlacementSlot({ surface, teamId }: SponsorPlacementSlotProps) {
  const placement = usePlacement(surface, teamId);
  if (placement.isLoading || placement.isError || !placement.data) {
    return null;
  }
  return <SponsorCard placement={placement.data} onVisit={() => placement.recordClick()} />;
}
