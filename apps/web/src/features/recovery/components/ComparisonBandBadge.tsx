import { Badge } from '@/core/components/Badge';
import { baselineSufficiencyLabel, comparisonBandLabel } from '@/features/recovery/models/labels';
import { comparisonBandBadgeTone } from '@/features/recovery/utils/readinessVisual';

interface ComparisonBandBadgeProps {
  band: string | null | undefined;
}

export function ComparisonBandBadge({ band }: ComparisonBandBadgeProps) {
  return <Badge tone={comparisonBandBadgeTone(band)}>{comparisonBandLabel(band)}</Badge>;
}

interface SufficiencyBadgeProps {
  sufficiency: string | null | undefined;
}

export function SufficiencyBadge({ sufficiency }: SufficiencyBadgeProps) {
  const tone = sufficiency === 'SUFFICIENT' ? 'success' : sufficiency === 'LIMITED' ? 'warning' : 'muted';
  return <Badge tone={tone}>{baselineSufficiencyLabel(sufficiency)}</Badge>;
}
