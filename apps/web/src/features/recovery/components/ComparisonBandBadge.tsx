import { Badge } from '@/core/components/Badge';
import { baselineSufficiencyLabel, comparisonBandLabel } from '@/features/recovery/models/labels';

interface ComparisonBandBadgeProps {
  band: string | null | undefined;
}

export function ComparisonBandBadge({ band }: ComparisonBandBadgeProps) {
  const tone = band === 'INSUFFICIENT_DATA' || !band ? 'muted' : 'neutral';
  return <Badge tone={tone}>{comparisonBandLabel(band)}</Badge>;
}

interface SufficiencyBadgeProps {
  sufficiency: string | null | undefined;
}

export function SufficiencyBadge({ sufficiency }: SufficiencyBadgeProps) {
  const tone = sufficiency === 'SUFFICIENT' ? 'accent' : 'muted';
  return <Badge tone={tone}>{baselineSufficiencyLabel(sufficiency)}</Badge>;
}
