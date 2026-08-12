import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
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
        <p className="emptyHint">No readiness assessment yet.</p>
        <Link to="/app/recovery">View recovery</Link>
      </HomeCard>
    );
  }

  const band = readiness.readinessBand;
  const tone = band === 'HIGH' ? 'success' : band === 'MODERATE' ? 'warning' : band === 'LOW' ? 'danger' : 'neutral';

  return (
    <HomeCard title="Readiness">
      <Badge tone={tone}>{readinessBandLabel(readiness.readinessBand)}</Badge>
      {readiness.readinessScore != null ? (
        <p style={{ margin: 0, fontSize: 'var(--uap-font-size-2xl)', fontWeight: 800 }}>
          {Math.round(Number(readiness.readinessScore))}
        </p>
      ) : null}
      {readiness.limitingDimensions && readiness.limitingDimensions.length > 0 ? (
        <p className="emptyHint">Limiting: {readiness.limitingDimensions.join(', ')}</p>
      ) : null}
      <Link
        to={
          readiness.readinessAssessmentId
            ? `/app/recovery/readiness/${readiness.readinessAssessmentId}`
            : '/app/recovery'
        }
      >
        View details
      </Link>
    </HomeCard>
  );
}
