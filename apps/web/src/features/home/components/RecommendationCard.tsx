import { HomeCard } from '@/features/home/components/HomeCard';
import {
  adjustmentTypeLabel,
  recommendationActionLabel,
} from '@/features/home/labels/todayLabels';
import type { TodayDashboard } from '@/features/home/schemas';

interface RecommendationCardProps {
  recommendation: TodayDashboard['recommendation'];
}

export function RecommendationCard({ recommendation }: RecommendationCardProps) {
  if (!recommendation.recommendationPresent) {
    return (
      <HomeCard title="Recommendation">
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          No training recommendation yet.
        </p>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Recommendation">
      <p style={{ margin: 0, fontSize: '1.125rem', fontWeight: 600 }}>
        {recommendationActionLabel(recommendation.overallAction)}
      </p>
      {recommendation.adjustmentTypes && recommendation.adjustmentTypes.length > 0 ? (
        <ul style={{ margin: 0, paddingLeft: '1.25rem', color: 'var(--uap-text-secondary)' }}>
          {recommendation.adjustmentTypes.map((type) => (
            <li key={type}>{adjustmentTypeLabel(type)}</li>
          ))}
        </ul>
      ) : null}
    </HomeCard>
  );
}
