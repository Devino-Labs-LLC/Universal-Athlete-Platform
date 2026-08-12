import { useSearchParams } from 'react-router-dom';

import { EmptyView } from '@/core/components/EmptyView';
import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { PerformanceSubNav } from '@/features/performance/components/PerformanceSubNav';
import { PersonalRecordsTable } from '@/features/performance/components/PersonalRecordsTable';
import { usePersonalRecords } from '@/features/performance/hooks/usePersonalRecords';
import { PERSONAL_RECORD_TYPE_LABELS } from '@/features/performance/models/labels';
import { performanceErrorMessage } from '@/features/performance/models/errors';
import { isPersonalRecordType, PERSONAL_RECORD_TYPES } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';

export function PersonalRecordsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const recordTypeParam = searchParams.get('type');
  const recordType = recordTypeParam && isPersonalRecordType(recordTypeParam) ? recordTypeParam : undefined;

  const recordsQuery = usePersonalRecords({ recordType });

  function handleTypeChange(next: string) {
    const params = new URLSearchParams(searchParams);
    if (next) {
      params.set('type', next);
    } else {
      params.delete('type');
    }
    setSearchParams(params, { replace: true });
  }

  return (
    <Page
      title="Personal records"
      description="All personal records across exercises, grouped by exercise identity."
      width="wide"
      actions={
        <label className={surfaces.filter}>
          <span className={surfaces.filterLabel}>Type</span>
          <select className="input" value={recordType ?? ''} onChange={(event) => handleTypeChange(event.target.value)}>
            <option value="">All types</option>
            {PERSONAL_RECORD_TYPES.map((type) => (
              <option key={type} value={type}>
                {PERSONAL_RECORD_TYPE_LABELS[type]}
              </option>
            ))}
          </select>
        </label>
      }
    >
      <PerformanceSubNav />

      <div className={surfaces.hub}>
        <section className={surfaces.panel} aria-labelledby="records-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="records-heading">
              Records
            </h2>
            <span className={surfaces.panelHint}>
              {recordType ? PERSONAL_RECORD_TYPE_LABELS[recordType] : 'All types'}
            </span>
          </div>
          {recordsQuery.isLoading ? <LoadingView message="Loading personal records…" /> : null}
          {recordsQuery.isError ? (
            <ErrorView message={performanceErrorMessage(recordsQuery.error)} onRetry={() => recordsQuery.refetch()} />
          ) : null}
          {recordsQuery.data ? (
            recordsQuery.data.length === 0 ? (
              <EmptyView
                title="No personal records"
                message="Complete training sessions to start building your performance history."
              />
            ) : (
              <PersonalRecordsTable records={recordsQuery.data} />
            )
          ) : null}
        </section>
      </div>
    </Page>
  );
}
