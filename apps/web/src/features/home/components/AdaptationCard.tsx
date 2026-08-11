import { useNavigate } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import type { TodayDashboard } from '@/features/home/schemas';

interface AdaptationCardProps {
  adaptation: TodayDashboard['adaptation'];
}

export function AdaptationCard({ adaptation }: AdaptationCardProps) {
  const navigate = useNavigate();

  if (!adaptation?.activeProposalPresent) {
    return (
      <HomeCard title="Adaptation">
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>No active adaptation proposal.</p>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Adaptation">
      {adaptation.status ? (
        <p style={{ margin: 0, fontWeight: 600 }}>{formatEnumLabel(adaptation.status)}</p>
      ) : null}
      {adaptation.unresolvedCount != null ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Unresolved items: {adaptation.unresolvedCount}
        </p>
      ) : null}
      <Button variant="secondary" onClick={() => navigate('/app/training')}>
        Review Adaptation
      </Button>
    </HomeCard>
  );
}
