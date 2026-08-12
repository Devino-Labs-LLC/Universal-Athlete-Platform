import { useParams } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { RecommendationAdjustmentsList } from '@/features/recovery/components/RecommendationAdjustmentsList';
import { useRecommendation } from '@/features/recovery/hooks/useRecommendations';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import {
  readinessBandLabel,
  readinessDimensionLabel,
  recommendationActionLabel,
  recommendationStatusLabel,
} from '@/features/recovery/models/labels';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { readinessBandBadgeTone } from '@/features/recovery/utils/readinessVisual';

export function GuidanceDetailPage() {
  const { recommendationId } = useParams<{ recommendationId: string }>();
  const recommendationQuery = useRecommendation(recommendationId);

  if (recommendationQuery.isLoading) {
    return <LoadingView message="Loading training guidance…" />;
  }

  if (recommendationQuery.isError) {
    return (
      <ErrorView
        message={recoveryErrorMessage(recommendationQuery.error)}
        onRetry={() => recommendationQuery.refetch()}
      />
    );
  }

  const recommendation = recommendationQuery.data!;

  return (
    <Page title="Training guidance" description={`Guidance for ${recommendation.stateDate}.`} width="wide">
      <div className={surfaces.hub}>
        <section className={surfaces.panel} aria-labelledby="guidance-action-heading">
          <p className={surfaces.eyebrow} id="guidance-action-heading">
            Guidance
          </p>
          <h2 className={surfaces.heroTitle}>{recommendationActionLabel(recommendation.overallAction)}</h2>
          <div className={surfaces.metaRow}>
            <Badge tone="info">{recommendationStatusLabel(recommendation.recommendationStatus)}</Badge>
            {recommendation.readinessBand ? (
              <Badge tone={readinessBandBadgeTone(recommendation.readinessBand)}>
                {readinessBandLabel(recommendation.readinessBand)}
              </Badge>
            ) : null}
          </div>
          {recommendation.limitingDimensions && recommendation.limitingDimensions.length > 0 ? (
            <p className={surfaces.metaText}>
              Limiting dimensions: {recommendation.limitingDimensions.map(readinessDimensionLabel).join(', ')}
            </p>
          ) : null}
        </section>

        <section className={surfaces.panel} aria-labelledby="adjustments-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="adjustments-heading">
              Suggested adjustments
            </h2>
          </div>
          <RecommendationAdjustmentsList adjustments={recommendation.adjustments} />
        </section>

        {recommendation.scheduledOccurrences && recommendation.scheduledOccurrences.length > 0 ? (
          <section className={surfaces.panel} aria-labelledby="sessions-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="sessions-heading">
                Scheduled sessions today
              </h2>
            </div>
            <ul className={surfaces.sessionList}>
              {recommendation.scheduledOccurrences.map((occurrence) => (
                <li key={occurrence.occurrenceId} className={surfaces.trendRow}>
                  <span className={surfaces.trendName}>
                    {occurrence.plannedEnvironmentNameSnapshot ?? 'Session'} — {occurrence.occurrenceStatus}
                  </span>
                  <Badge tone={occurrence.modifiable ? 'info' : 'muted'}>
                    {occurrence.modifiable ? 'Modifiable' : '(not modifiable)'}
                  </Badge>
                </li>
              ))}
            </ul>
          </section>
        ) : null}
      </div>
    </Page>
  );
}
