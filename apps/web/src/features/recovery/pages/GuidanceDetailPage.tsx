import { useParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import { RecommendationAdjustmentsList } from '@/features/recovery/components/RecommendationAdjustmentsList';
import { useRecommendation } from '@/features/recovery/hooks/useRecommendations';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import {
  readinessBandLabel,
  readinessDimensionLabel,
  recommendationActionLabel,
  recommendationStatusLabel,
} from '@/features/recovery/models/labels';

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
    <Page title="Training guidance" description={`Guidance for ${recommendation.stateDate}.`}>
      <section className="card" style={{ marginBottom: '1rem' }}>
        <p style={{ margin: 0, fontSize: 'var(--uap-font-size-lg)', fontWeight: 600 }}>
          {recommendationActionLabel(recommendation.overallAction)}
        </p>
        <p className={tableStyles.subtle}>
          Status: {recommendationStatusLabel(recommendation.recommendationStatus)}
          {recommendation.readinessBand ? ` · Readiness: ${readinessBandLabel(recommendation.readinessBand)}` : ''}
        </p>
        {recommendation.limitingDimensions && recommendation.limitingDimensions.length > 0 ? (
          <p className={tableStyles.subtle}>
            Limiting dimensions: {recommendation.limitingDimensions.map(readinessDimensionLabel).join(', ')}
          </p>
        ) : null}
      </section>

      <section className="card">
        <h2 className="cardTitle">Suggested adjustments</h2>
        <RecommendationAdjustmentsList adjustments={recommendation.adjustments} />
      </section>

      {recommendation.scheduledOccurrences && recommendation.scheduledOccurrences.length > 0 ? (
        <section className="card" style={{ marginTop: '1rem' }}>
          <h2 className="cardTitle">Scheduled sessions today</h2>
          <ul style={{ display: 'grid', gap: '0.35rem', margin: 0, paddingLeft: '1.1rem' }}>
            {recommendation.scheduledOccurrences.map((occurrence) => (
              <li key={occurrence.occurrenceId}>
                {occurrence.plannedEnvironmentNameSnapshot ?? 'Session'} — {occurrence.occurrenceStatus}
                {occurrence.modifiable ? '' : ' (not modifiable)'}
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </Page>
  );
}
