import { useParams, useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import tableStyles from '@/core/components/Table.module.scss';
import { useAthleteStateComparison } from '@/features/recovery/hooks/useAthleteState';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';

export function AthleteStateComparePage() {
  const { snapshotId } = useParams<{ snapshotId: string }>();
  const [searchParams] = useSearchParams();
  const otherSnapshotId = searchParams.get('other') ?? undefined;

  const comparisonQuery = useAthleteStateComparison(snapshotId, otherSnapshotId);

  if (!otherSnapshotId) {
    return (
      <Page title="Compare athlete state" description="Provide a snapshot to compare against using the ?other= query parameter.">
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
    >
      <section className="card">
        <div className="statGrid">
          <div className="stat">
            <span className="statLabel">Recovery changed</span>
            <span className="statValue">{comparison.recoveryChanged ? 'Yes' : 'No'}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Baseline changed</span>
            <span className="statValue">{comparison.baselineChanged ? 'Yes' : 'No'}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Training load changed</span>
            <span className="statValue">{comparison.trainingLoadChanged ? 'Yes' : 'No'}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Schedule changed</span>
            <span className="statValue">{comparison.scheduleChanged ? 'Yes' : 'No'}</span>
          </div>
          <div className="stat">
            <span className="statLabel">Discomfort changed</span>
            <span className="statValue">{comparison.discomfortChanged ? 'Yes' : 'No'}</span>
          </div>
        </div>

        {comparison.fieldDifferences.length > 0 ? (
          <table className={tableStyles.table} style={{ marginTop: '1rem' }}>
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
        ) : (
          <p className={tableStyles.subtle} style={{ marginTop: '1rem' }}>
            No field-level differences were reported.
          </p>
        )}
      </section>
    </Page>
  );
}
