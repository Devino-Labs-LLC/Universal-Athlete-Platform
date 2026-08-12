import { Link } from 'react-router-dom';

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
        <p className="emptyHint">No training recommendation yet.</p>
        <Link to="/app/recovery">View recovery</Link>
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
      {recommendation.recommendationId ? (
        <Link to={`/app/recovery/guidance/${recommendation.recommendationId}`}>View details</Link>
      ) : null}
    </HomeCard>
  );
}
