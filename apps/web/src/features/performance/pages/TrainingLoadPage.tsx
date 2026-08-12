import { useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { TRAINING_LOAD_MAX_PAGE_SIZE } from '@/features/performance/api/trainingLoadApi';
import { CategoryDistributionTable, MovementDistributionTable } from '@/features/performance/components/LoadDistributionTable';
import { PerformanceSubNav } from '@/features/performance/components/PerformanceSubNav';
import { TrainingLoadHistoryTable } from '@/features/performance/components/TrainingLoadHistoryTable';
import { useTrainingLoadHistory } from '@/features/performance/hooks/useTrainingLoadHistory';
import { loadRangeLabel, trainingLoadGranularityLabel } from '@/features/performance/models/labels';
import { performanceErrorMessage } from '@/features/performance/models/errors';
import { isTrainingLoadGranularity, TRAINING_LOAD_GRANULARITIES, type TrainingLoadGranularity } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
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

  // Backend rejects size outside 1..MAX_PAGE_SIZE (100) with INVALID_TRAINING_LOAD_DATE_RANGE.
  const historyQuery = useTrainingLoadHistory(mode, startDate, endDate, {
    size: TRAINING_LOAD_MAX_PAGE_SIZE,
  });

  function updateParams(next: { mode?: TrainingLoadGranularity; range?: LoadRangeDays }) {
    const params = new URLSearchParams(searchParams);
    if (next.mode) {
      params.set('mode', next.mode);
    }
    if (next.range != null) {
      params.set('range', String(next.range));
    }
    setSearchParams(params);
  }

  return (
    <Page
      title="Training load"
      description={`Load history from ${startDate} to ${endDate}.`}
      width="wide"
    >
      <PerformanceSubNav />

      <div className={surfaces.hub}>
        <section className={surfaces.toolbar} aria-label="Load filters">
          <label className={surfaces.filter}>
            <span className={surfaces.filterLabel}>Granularity</span>
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

          <label className={surfaces.filter}>
            <span className={surfaces.filterLabel}>Range</span>
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
        </section>

        {historyQuery.isLoading ? <LoadingView message="Loading training load…" /> : null}
        {historyQuery.isError ? (
          <ErrorView message={performanceErrorMessage(historyQuery.error)} onRetry={() => historyQuery.refetch()} />
        ) : null}

        {historyQuery.data ? (
          <>
            <section className={surfaces.panel} aria-labelledby="load-summary-heading">
              <div className={surfaces.panelHeader}>
                <h2 className={surfaces.panelTitle} id="load-summary-heading">
                  {trainingLoadGranularityLabel(mode)} summary
                </h2>
                <span className={surfaces.panelHint}>
                  {startDate} → {endDate}
                </span>
              </div>
              <TrainingLoadHistoryTable history={historyQuery.data} />
            </section>

            <section className={surfaces.panel} aria-labelledby="load-category-heading">
              <div className={surfaces.panelHeader}>
                <h2 className={surfaces.panelTitle} id="load-category-heading">
                  By category
                </h2>
              </div>
              <CategoryDistributionTable summaries={aggregateCategorySummaries(historyQuery.data)} />
            </section>

            <section className={surfaces.panel} aria-labelledby="load-movement-heading">
              <div className={surfaces.panelHeader}>
                <h2 className={surfaces.panelTitle} id="load-movement-heading">
                  By movement pattern
                </h2>
              </div>
              <MovementDistributionTable summaries={aggregateMovementSummaries(historyQuery.data)} />
            </section>
          </>
        ) : null}
      </div>
    </Page>
  );
}
