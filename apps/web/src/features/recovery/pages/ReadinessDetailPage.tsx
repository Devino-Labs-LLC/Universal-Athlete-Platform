import { useParams, useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import { ReadinessContributionsTable } from '@/features/recovery/components/ReadinessContributionsTable';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import { baselineSufficiencyLabel, readinessBandLabel, readinessDimensionLabel } from '@/features/recovery/models/labels';
import { useReadinessAssessment, useReadinessComparison } from '@/features/recovery/hooks/useReadiness';

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

  return (
    <Page title="Readiness assessment" description={`Assessment for ${assessment.stateDate}.`}>
      <section className="card" style={{ marginBottom: '1rem' }}>
        <div className="statGrid">
          <div className="stat">
            <span className="statLabel">Readiness band</span>
            <span className="statValue">{readinessBandLabel(assessment.readinessBand)}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Data sufficiency</span>
            <span className="statValue">{baselineSufficiencyLabel(assessment.dataSufficiency)}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Score</span>
            <span className="statValue">{assessment.readinessScore != null ? Number(assessment.readinessScore).toFixed(1) : '—'}</span>
          </div>
        </div>
        {assessment.limitingDimensions.length > 0 ? (
          <p className={tableStyles.subtle} style={{ marginTop: '0.75rem' }}>
            Limiting dimensions: {assessment.limitingDimensions.map(readinessDimensionLabel).join(', ')}
          </p>
        ) : null}
        {assessment.strongestDimensions.length > 0 ? (
          <p className={tableStyles.subtle}>
            Strongest dimensions: {assessment.strongestDimensions.map(readinessDimensionLabel).join(', ')}
          </p>
        ) : null}
      </section>

      <section className="card">
        <h2 className="cardTitle">Contributions</h2>
        <ReadinessContributionsTable contributions={assessment.contributions} />
      </section>

      {compareToId ? (
        <section className="card" style={{ marginTop: '1rem' }}>
          <h2 className="cardTitle">Comparison</h2>
          {comparisonQuery.isLoading ? <LoadingView message="Loading comparison…" /> : null}
          {comparisonQuery.isError ? (
            <ErrorView
              message={recoveryErrorMessage(comparisonQuery.error)}
              onRetry={() => comparisonQuery.refetch()}
            />
          ) : null}
          {comparisonQuery.data ? (
            <div className="statGrid">
              <div className="stat">
                <span className="statLabel">Score change</span>
                <span className="statValue">
                  {comparisonQuery.data.scoreDelta != null ? Number(comparisonQuery.data.scoreDelta).toFixed(1) : '—'}
                </span>
              </div>
              <div className="stat">
                <span className="statLabel">Band changed</span>
                <span className="statValue">{comparisonQuery.data.bandChanged ? 'Yes' : 'No'}</span>
              </div>
              <div className="stat">
                <span className="statLabel">Limiting dimensions changed</span>
                <span className="statValue">{comparisonQuery.data.limitingDimensionsChanged ? 'Yes' : 'No'}</span>
              </div>
            </div>
          ) : null}
        </section>
      ) : null}
    </Page>
  );
}
