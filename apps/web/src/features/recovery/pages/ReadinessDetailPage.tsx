import { useParams, useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { ScoreRing } from '@/core/components/ScoreRing';
import { MetricPill } from '@/features/training/components/MetricPill';
import { ReadinessContributionsTable } from '@/features/recovery/components/ReadinessContributionsTable';
import { SufficiencyBadge } from '@/features/recovery/components/ComparisonBandBadge';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import { readinessBandLabel, readinessDimensionLabel } from '@/features/recovery/models/labels';
import { useReadinessAssessment, useReadinessComparison } from '@/features/recovery/hooks/useReadiness';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { readinessRingTone } from '@/features/recovery/utils/readinessVisual';

export function ReadinessDetailPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const [searchParams] = useSearchParams();
  const compareToId = searchParams.get('compare') ?? undefined;

  const assessmentQuery = useReadinessAssessment(assessmentId);
  const comparisonQuery = useReadinessComparison(assessmentId, compareToId);

  if (assessmentQuery.isLoading) {
    return <LoadingView message="Loading readiness assessment…" />;
  }

  if (assessmentQuery.isError) {
    return (
      <ErrorView message={recoveryErrorMessage(assessmentQuery.error)} onRetry={() => assessmentQuery.refetch()} />
    );
  }

  const assessment = assessmentQuery.data!;
  const score = assessment.readinessScore != null ? Number(assessment.readinessScore) : null;
  const hasScore = score != null && Number.isFinite(score);

  return (
    <Page
      title="Readiness assessment"
      description={`Assessment for ${assessment.stateDate}.`}
      width="wide"
    >
      <div className={surfaces.hub}>
        <section className={surfaces.panel} aria-labelledby="readiness-score-heading">
          <div className={surfaces.scoreHero}>
            <ScoreRing
              score={hasScore ? score : null}
              label="Readiness"
              tone={readinessRingTone(assessment.readinessBand)}
              size={128}
            />
            <div className={surfaces.heroCopy}>
              <p className={surfaces.eyebrow} id="readiness-score-heading">
                Readiness
              </p>
              <h2 className={surfaces.heroTitle}>{readinessBandLabel(assessment.readinessBand)}</h2>
              <div className={surfaces.metaRow}>
                <SufficiencyBadge sufficiency={assessment.dataSufficiency} />
                {hasScore ? (
                  <MetricPill label="Score">{score.toFixed(1)}</MetricPill>
                ) : (
                  <MetricPill label="Score">—</MetricPill>
                )}
              </div>
              {assessment.limitingDimensions.length > 0 ? (
                <p className={surfaces.metaText}>
                  Limiting dimensions: {assessment.limitingDimensions.map(readinessDimensionLabel).join(', ')}
                </p>
              ) : null}
              {assessment.strongestDimensions.length > 0 ? (
                <p className={surfaces.metaText}>
                  Strongest dimensions: {assessment.strongestDimensions.map(readinessDimensionLabel).join(', ')}
                </p>
              ) : null}
            </div>
          </div>
        </section>

        <section className={surfaces.panel} aria-labelledby="contributions-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="contributions-heading">
              Contributions
            </h2>
          </div>
          <ReadinessContributionsTable contributions={assessment.contributions} />
        </section>

        {compareToId ? (
          <section className={surfaces.panel} aria-labelledby="comparison-heading">
            <div className={surfaces.panelHeader}>
              <h2 className={surfaces.panelTitle} id="comparison-heading">
                Comparison
              </h2>
            </div>
            {comparisonQuery.isLoading ? <LoadingView message="Loading comparison…" /> : null}
            {comparisonQuery.isError ? (
              <ErrorView
                message={recoveryErrorMessage(comparisonQuery.error)}
                onRetry={() => comparisonQuery.refetch()}
              />
            ) : null}
            {comparisonQuery.data ? (
              <div className={surfaces.metricGrid}>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Score change</span>
                  <span className={surfaces.metricValue}>
                    {comparisonQuery.data.scoreDelta != null
                      ? Number(comparisonQuery.data.scoreDelta).toFixed(1)
                      : '—'}
                  </span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Band changed</span>
                  <span className={surfaces.metricValue}>
                    {comparisonQuery.data.bandChanged ? 'Yes' : 'No'}
                  </span>
                </div>
                <div className={surfaces.metricTile}>
                  <span className={surfaces.metricLabel}>Limiting dims changed</span>
                  <span className={surfaces.metricValue}>
                    {comparisonQuery.data.limitingDimensionsChanged ? 'Yes' : 'No'}
                  </span>
                </div>
              </div>
            ) : null}
          </section>
        ) : null}
      </div>
    </Page>
  );
}
