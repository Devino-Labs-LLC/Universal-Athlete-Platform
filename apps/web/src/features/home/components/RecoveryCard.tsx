import { useNavigate } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import type { TodayDashboard } from '@/features/home/schemas';

interface RecoveryCardProps {
  recovery: TodayDashboard['recovery'];
}

export function RecoveryCard({ recovery }: RecoveryCardProps) {
  const navigate = useNavigate();
  const checkInLabel = recovery.checkInPresent ? 'Update check-in' : 'Check in';

  return (
    <HomeCard title="Recovery">
      <Badge tone={recovery.checkInPresent ? 'success' : 'muted'}>
        {recovery.checkInPresent ? 'Checked in' : 'No check-in'}
      </Badge>
      <p className="emptyHint">
        {recovery.checkInPresent ? 'Check-in completed for today.' : 'No recovery check-in yet.'}
      </p>
      {recovery.fatigue != null ? <p className="emptyHint">Fatigue: {recovery.fatigue}</p> : null}
      <Button onClick={() => navigate('/app/recovery/check-in')}>{checkInLabel}</Button>
      <Button variant="secondary" onClick={() => navigate('/app/recovery')}>
        View recovery
      </Button>
    </HomeCard>
  );
}
