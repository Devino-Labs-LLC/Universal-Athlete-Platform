import { useParams, useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import { useAthleteStateComparison } from '@/features/recovery/hooks/useAthleteState';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';

export function AthleteStateComparePage() {
  const { snapshotId } = useParams<{ snapshotId: string }>();
  const [searchParams] = useSearchParams();
  const otherSnapshotId = searchParams.get('other') ?? undefined;

  const comparisonQuery = useAthleteStateComparison(snapshotId, otherSnapshotId);

  if (!otherSnapshotId) {
    return (
      <Page
        title="Compare athlete state"
        description="Provide a snapshot to compare against using the ?other= query parameter."
        width="wide"
      >
        <p className={tableStyles.subtle}>No comparison snapshot was specified.</p>
      </Page>
    );
  }

  if (comparisonQuery.isLoading) {
    return <LoadingView message="Loading comparison…" />;
  }

  if (comparisonQuery.isError) {
    return (
      <ErrorView message={recoveryErrorMessage(comparisonQuery.error)} onRetry={() => comparisonQuery.refetch()} />
    );
  }

  const comparison = comparisonQuery.data!;

  return (
    <Page
      title="Compare athlete state"
      description={`Comparing ${comparison.olderStateDate} (v${comparison.olderVersion}) to ${comparison.newerStateDate} (v${comparison.newerVersion}).`}
      width="wide"
    >
      <div className={surfaces.hub}>
        <section className={surfaces.panel} aria-labelledby="compare-flags-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="compare-flags-heading">
              What changed
            </h2>
            <span className={surfaces.panelHint}>Historical comparison · reference only</span>
          </div>
          <div className={surfaces.metricGrid}>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Recovery</span>
              <span className={surfaces.metricValue}>{comparison.recoveryChanged ? 'Yes' : 'No'}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Baseline</span>
              <span className={surfaces.metricValue}>{comparison.baselineChanged ? 'Yes' : 'No'}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Training load</span>
              <span className={surfaces.metricValue}>{comparison.trainingLoadChanged ? 'Yes' : 'No'}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Schedule</span>
              <span className={surfaces.metricValue}>{comparison.scheduleChanged ? 'Yes' : 'No'}</span>
            </div>
            <div className={surfaces.metricTile}>
              <span className={surfaces.metricLabel}>Discomfort</span>
              <span className={surfaces.metricValue}>{comparison.discomfortChanged ? 'Yes' : 'No'}</span>
            </div>
          </div>
        </section>

        <section className={surfaces.panel} aria-labelledby="field-diff-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="field-diff-heading">
              Field differences
            </h2>
          </div>
          {comparison.fieldDifferences.length > 0 ? (
            <div className={surfaces.tableWrap}>
              <table className={tableStyles.table}>
                <caption className="srOnly">Field-level differences</caption>
                <thead>
                  <tr>
                    <th scope="col">Field</th>
                    <th scope="col">Previous</th>
                    <th scope="col">New</th>
                  </tr>
                </thead>
                <tbody>
                  {comparison.fieldDifferences.map((difference) => (
                    <tr key={difference.field}>
                      <th scope="row">{difference.field}</th>
                      <td>{difference.previousValue ?? '—'}</td>
                      <td>{difference.newValue ?? '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className={tableStyles.subtle}>No field-level differences were reported.</p>
          )}
        </section>
      </div>
    </Page>
  );
}
