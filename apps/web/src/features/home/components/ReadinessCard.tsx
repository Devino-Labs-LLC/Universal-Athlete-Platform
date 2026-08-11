import { Link } from 'react-router-dom';

import { HomeCard } from '@/features/home/components/HomeCard';
import { readinessBandLabel } from '@/features/home/labels/todayLabels';
import type { TodayDashboard } from '@/features/home/schemas';

interface ReadinessCardProps {
  readiness: TodayDashboard['readiness'];
}

export function ReadinessCard({ readiness }: ReadinessCardProps) {
  if (!readiness.readinessPresent) {
    return (
      <HomeCard title="Readiness">
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>No readiness assessment yet.</p>
        <Link to="/app/recovery">View recovery</Link>
      </HomeCard>
    );
  }

  return (
    <HomeCard title="Readiness">
      <p style={{ margin: 0, fontSize: '1.25rem', fontWeight: 600 }}>
        {readinessBandLabel(readiness.readinessBand)}
      </p>
      {readiness.readinessScore != null ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Score: {readiness.readinessScore}
        </p>
      ) : null}
      {readiness.limitingDimensions && readiness.limitingDimensions.length > 0 ? (
        <p style={{ margin: 0, color: 'var(--uap-text-secondary)' }}>
          Limiting: {readiness.limitingDimensions.join(', ')}
        </p>
      ) : null}
      <Link to={readiness.readinessAssessmentId ? `/app/recovery/readiness/${readiness.readinessAssessmentId}` : '/app/recovery'}>
        View details
      </Link>
    </HomeCard>
  );
}
