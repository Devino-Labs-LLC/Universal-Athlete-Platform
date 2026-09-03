import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { HomeCard } from '@/features/home/components/HomeCard';
import {
  MISSING_INTELLIGENCE_COPY,
  missingReadinessStep,
  readinessExplanationLines,
} from '@/features/home/labels/readinessInsight';
import { readinessBandLabel } from '@/features/home/labels/todayLabels';
import type { TodayDashboard } from '@/features/home/schemas';

interface ReadinessCardProps {
  readiness: TodayDashboard['readiness'];
  checkInPresent: boolean;
  snapshotPresent: boolean;
}

export function ReadinessCard({
  readiness,
  checkInPresent,
  snapshotPresent,
}: ReadinessCardProps) {
  if (!readiness.readinessPresent) {
    const absence = missingReadinessStep({ checkInPresent, snapshotPresent });
    return (
      <HomeCard title="Readiness">
        <p className="emptyHint">{MISSING_INTELLIGENCE_COPY[absence]}</p>
        <Link to="/app/recovery">View recovery</Link>
      </HomeCard>
    );
  }

  const band = readiness.readinessBand;
  const tone = band === 'HIGH' ? 'success' : band === 'MODERATE' ? 'warning' : band === 'LOW' ? 'danger' : 'neutral';
  const explanations = readinessExplanationLines({
    readinessBand: readiness.readinessBand,
    dataSufficiency: readiness.dataSufficiency,
    limitingDimensions: readiness.limitingDimensions,
  });

  return (
    <HomeCard title="Readiness">
      <Badge tone={tone}>{readinessBandLabel(readiness.readinessBand)}</Badge>
      {readiness.readinessScore != null ? (
        <p style={{ margin: 0, fontSize: 'var(--uap-font-size-2xl)', fontWeight: 800 }}>
          {Math.round(Number(readiness.readinessScore))}
        </p>
      ) : null}
      {explanations.map((line) => (
        <p key={line} className="emptyHint">
          {line}
        </p>
      ))}
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
