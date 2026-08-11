import { Link } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { PerformanceSubNav } from '@/features/performance/components/PerformanceSubNav';
import { PersonalRecordsTable } from '@/features/performance/components/PersonalRecordsTable';
import { useRecentPersonalRecords } from '@/features/performance/hooks/usePersonalRecords';
import { performanceErrorMessage } from '@/features/performance/models/errors';

export function PerformanceLandingPage() {
  const recentRecordsQuery = useRecentPersonalRecords(30, 10);

  return (
    <Page
      title="Performance"
      description="Recent personal records, exercise history, and training load."
      actions={
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <Link to="/app/performance/records">View all records</Link>
          <Link to="/app/performance/load">Training load</Link>
        </div>
      }
    >
      <PerformanceSubNav />

      <section className="card">
        <h2 className="cardTitle">Recent personal records (last 30 days)</h2>
        {recentRecordsQuery.isLoading ? <LoadingView message="Loading personal records…" /> : null}
        {recentRecordsQuery.isError ? (
          <ErrorView
            message={performanceErrorMessage(recentRecordsQuery.error)}
            onRetry={() => recentRecordsQuery.refetch()}
          />
        ) : null}
        {recentRecordsQuery.data ? (
          recentRecordsQuery.data.length === 0 ? (
            <EmptyView
              title="No recent personal records"
              message="Complete training sessions to start building your performance history."
            />
          ) : (
            <PersonalRecordsTable records={recentRecordsQuery.data} />
          )
        ) : null}
      </section>
    </Page>
  );
}
