import { Link } from 'react-router-dom';

import { HomeCard } from '@/features/home/components/HomeCard';
import {
  MISSING_INTELLIGENCE_COPY,
  missingRecommendationStep,
} from '@/features/home/labels/readinessInsight';
import {
  adjustmentTypeLabel,
  recommendationActionLabel,
} from '@/features/home/labels/todayLabels';
import type { TodayDashboard } from '@/features/home/schemas';

interface RecommendationCardProps {
  recommendation: TodayDashboard['recommendation'];
  checkInPresent: boolean;
  snapshotPresent: boolean;
  readinessPresent: boolean;
}

export function RecommendationCard({
  recommendation,
  checkInPresent,
  snapshotPresent,
  readinessPresent,
}: RecommendationCardProps) {
  if (!recommendation.recommendationPresent) {
    const absence = missingRecommendationStep({
      checkInPresent,
      snapshotPresent,
      readinessPresent,
    });
    return (
      <HomeCard title="Recommendation">
        <p className="emptyHint">{MISSING_INTELLIGENCE_COPY[absence]}</p>
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
