import { useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { CategoryDistributionTable, MovementDistributionTable } from '@/features/performance/components/LoadDistributionTable';
import { PerformanceSubNav } from '@/features/performance/components/PerformanceSubNav';
import { TrainingLoadHistoryTable } from '@/features/performance/components/TrainingLoadHistoryTable';
import { useTrainingLoadHistory } from '@/features/performance/hooks/useTrainingLoadHistory';
import { loadRangeLabel, trainingLoadGranularityLabel } from '@/features/performance/models/labels';
import { performanceErrorMessage } from '@/features/performance/models/errors';
import { isTrainingLoadGranularity, TRAINING_LOAD_GRANULARITIES, type TrainingLoadGranularity } from '@/features/performance/models/schemas';
import { aggregateCategorySummaries, aggregateMovementSummaries } from '@/features/performance/utils/aggregateDistributions';
import { dateRangeForLoadHistory, isLoadRangeDays, LOAD_RANGE_OPTIONS, type LoadRangeDays } from '@/features/performance/utils/dateRanges';

function parseModeParam(value: string | null): TrainingLoadGranularity {
  return value && isTrainingLoadGranularity(value) ? value : 'WEEKLY';
}

function parseRangeParam(value: string | null): LoadRangeDays {
  const parsed = Number(value);
  return isLoadRangeDays(parsed) ? parsed : 28;
}

export function TrainingLoadPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const mode = parseModeParam(searchParams.get('mode'));
  const rangeDays = parseRangeParam(searchParams.get('range'));
  const { startDate, endDate } = dateRangeForLoadHistory(rangeDays, todayDateOnly());

  const historyQuery = useTrainingLoadHistory(mode, startDate, endDate, { size: 200 });

  function updateParams(next: { mode?: TrainingLoadGranularity; range?: LoadRangeDays }) {
    const params = new URLSearchParams(searchParams);
    if (next.mode) {
      params.set('mode', next.mode);
    }
    if (next.range != null) {
      params.set('range', String(next.range));
    }
    setSearchParams(params, { replace: true });
  }

  return (
    <Page
      title="Training load"
      description={`Load history from ${startDate} to ${endDate}.`}
      actions={
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="label">Granularity</span>
            <select
              className="input"
              value={mode}
              onChange={(event) => {
                if (isTrainingLoadGranularity(event.target.value)) {
                  updateParams({ mode: event.target.value });
                }
              }}
            >
              {TRAINING_LOAD_GRANULARITIES.map((option) => (
                <option key={option} value={option}>
                  {trainingLoadGranularityLabel(option)}
                </option>
              ))}
            </select>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span className="label">Range</span>
            <select
              className="input"
              value={rangeDays}
              onChange={(event) => {
                const next = Number(event.target.value);
                if (isLoadRangeDays(next)) {
                  updateParams({ range: next });
                }
              }}
            >
              {LOAD_RANGE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {loadRangeLabel(option)}
                </option>
              ))}
            </select>
          </label>
        </div>
      }
    >
      <PerformanceSubNav />

      {historyQuery.isLoading ? <LoadingView message="Loading training load…" /> : null}
      {historyQuery.isError ? (
        <ErrorView message={performanceErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />
      ) : null}

      {historyQuery.data ? (
        <div style={{ display: 'grid', gap: '1rem' }}>
          <section className="card">
            <h2 className="cardTitle">{trainingLoadGranularityLabel(mode)} summary</h2>
            <TrainingLoadHistoryTable history={historyQuery.data} />
          </section>

          <section className="card">
            <h2 className="cardTitle">By category</h2>
            <CategoryDistributionTable summaries={aggregateCategorySummaries(historyQuery.data)} />
          </section>

          <section className="card">
            <h2 className="cardTitle">By movement pattern</h2>
            <MovementDistributionTable summaries={aggregateMovementSummaries(historyQuery.data)} />
          </section>
        </div>
      ) : null}
    </Page>
  );
}
