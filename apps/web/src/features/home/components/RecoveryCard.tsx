import { useNavigate } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import type { TodayDashboard } from '@/features/home/schemas';

interface RecoveryCardProps {
  recovery: TodayDashboard['recovery'];
}

export function RecoveryCard({ recovery }: RecoveryCardProps) {
  const navigate = useNavigate();

  return (
    <HomeCard title="Recovery">
      <p className="emptyHint">
        {recovery.checkInPresent ? 'Check-in completed for today.' : 'No recovery check-in yet.'}
      </p>
      {recovery.fatigue != null ? (
        <p className="emptyHint">Fatigue: {recovery.fatigue}</p>
      ) : null}
      <Button variant="secondary" onClick={() => navigate('/app/recovery')}>
        View recovery
      </Button>
    </HomeCard>
  );
}
