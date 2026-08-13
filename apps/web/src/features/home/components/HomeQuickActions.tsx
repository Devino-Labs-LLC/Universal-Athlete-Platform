import { useNavigate } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import type { TodayDashboard } from '@/features/home/schemas';

interface HomeQuickActionsProps {
  actions: TodayDashboard['actions'];
  onGenerateState: () => void;
  onGenerateReadiness: () => void;
  onGenerateRecommendation: () => void;
  pendingAction: 'state' | 'readiness' | 'guidance' | null;
  errorMessage?: string | null;
}

export function HomeQuickActions({
  actions,
  onGenerateState,
  onGenerateReadiness,
  onGenerateRecommendation,
  pendingAction,
  errorMessage,
}: HomeQuickActionsProps) {
  const navigate = useNavigate();

  if (!actions) {
    return null;
  }

  const canCheckIn =
    actions.canCreateRecoveryCheckIn?.allowed || actions.canUpdateRecoveryCheckIn?.allowed;

  const hasAnyAction =
    canCheckIn ||
    actions.canGenerateAthleteStateSnapshot?.allowed ||
    actions.canGenerateReadinessAssessment?.allowed ||
    actions.canGenerateTrainingRecommendation?.allowed;

  if (!hasAnyAction) {
    return null;
  }

  return (
    <HomeCard title="Quick actions">
      {canCheckIn ? (
        <Button
          variant="secondary"
          disabled={pendingAction !== null}
          onClick={() => navigate('/app/recovery/check-in')}
        >
          {actions.canCreateRecoveryCheckIn?.allowed ? 'Check in' : 'Update check-in'}
        </Button>
      ) : null}
      {actions.canGenerateAthleteStateSnapshot?.allowed ? (
        <Button
          variant="secondary"
          disabled={pendingAction !== null}
          onClick={onGenerateState}
        >
          {pendingAction === 'state' ? 'Generating…' : 'Generate Daily State'}
        </Button>
      ) : null}
      {actions.canGenerateReadinessAssessment?.allowed ? (
        <Button
          variant="secondary"
          disabled={pendingAction !== null}
          onClick={onGenerateReadiness}
        >
          {pendingAction === 'readiness' ? 'Generating…' : 'Generate Readiness'}
        </Button>
      ) : null}
      {actions.canGenerateTrainingRecommendation?.allowed ? (
        <Button
          variant="secondary"
          disabled={pendingAction !== null}
          onClick={onGenerateRecommendation}
        >
          {pendingAction === 'guidance' ? 'Generating…' : 'Generate Guidance'}
        </Button>
      ) : null}
      {errorMessage ? (
        <p className="formError" role="alert">
          {errorMessage}
        </p>
      ) : null}
    </HomeCard>
  );
}
