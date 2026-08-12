import { useNavigate } from 'react-router-dom';

import { Button } from '@/core/components/Button';
import { HomeCard } from '@/features/home/components/HomeCard';
import { formatEnumLabel } from '@/features/profile/enumLabels';
import type { TodayDashboard, TrainingDashboardOccurrence } from '@/features/home/schemas';

interface AdaptationCardProps {
  adaptation: TodayDashboard['adaptation'];
  /** Primary (or matching) occurrence used to deep-link when adaptation.occurrenceId matches. */
  linkedOccurrence?: TrainingDashboardOccurrence | null;
}

export function AdaptationCard({ adaptation, linkedOccurrence }: AdaptationCardProps) {
  const navigate = useNavigate();

  if (!adaptation?.activeProposalPresent) {
    return (
      <HomeCard title="Adaptation">
        <p className="emptyHint">No active adaptation proposal.</p>
      </HomeCard>
    );
  }

  const matchingOccurrence =
    linkedOccurrence &&
    adaptation.occurrenceId &&
    linkedOccurrence.occurrenceId === adaptation.occurrenceId
      ? linkedOccurrence
      : null;

  const occurrencePath = matchingOccurrence
    ? `/app/training/plans/${matchingOccurrence.trainingPlanId}/days/${matchingOccurrence.workoutDayId}/occurrences/${matchingOccurrence.occurrenceId}`
    : null;

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
      <p style={{ margin: 0, color: 'var(--uap-text-secondary)', fontSize: '0.875rem' }}>
        Live adaptation review runs on mobile. Open the related workout for context on web.
      </p>
      {occurrencePath ? (
        <Button variant="secondary" onClick={() => navigate(occurrencePath)}>
          View related workout
        </Button>
      ) : (
        <Button variant="secondary" onClick={() => navigate('/app/training/calendar')}>
          Open calendar
        </Button>
      )}
    </HomeCard>
  );
}
