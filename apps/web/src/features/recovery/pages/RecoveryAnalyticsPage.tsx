import { useSearchParams } from 'react-router-dom';

import { ErrorView } from '@/core/components/ErrorView';
import { LoadingView } from '@/core/components/LoadingView';
import { Page } from '@/core/components/Page';
import { todayDateOnly } from '@/core/date/dateOnly';
import { BaselineDeviationTable } from '@/features/recovery/components/BaselineDeviationTable';
import { DiscomfortHistoryTable } from '@/features/recovery/components/DiscomfortHistoryTable';
import { MetricTrendPanel } from '@/features/recovery/components/MetricTrendPanel';
import { RecoverySubNav } from '@/features/recovery/components/RecoverySubNav';
import { recoveryErrorMessage } from '@/features/recovery/models/errors';
import { metricTypeLabel } from '@/features/recovery/models/labels';
import {
  useBodyAreaDiscomfortHistory,
  useRecoveryDashboard,
  useRecoveryMetricTrend,
} from '@/features/recovery/hooks/useRecoveryAnalytics';
import {
  BASELINE_WINDOW_OPTIONS,
  isBaselineWindowDays,
  isRecoveryMetricType,
  RECOVERY_METRIC_TYPES,
  type BaselineWindowDays,
  type RecoveryMetricType,
} from '@/features/recovery/models/schemas';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { dateRangeForTrend, subtractDays } from '@/features/recovery/utils/dateRanges';

const DEFAULT_METRIC: RecoveryMetricType = 'FATIGUE';
const TREND_RANGE_DAYS = 28;

function parseWindowParam(value: string | null): BaselineWindowDays {
  const parsed = Number(value);
  return isBaselineWindowDays(parsed) ? parsed : 14;
}

function parseMetricParam(value: string | null): RecoveryMetricType {
  return value && isRecoveryMetricType(value) ? value : DEFAULT_METRIC;
}

export function RecoveryAnalyticsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const windowDays = parseWindowParam(searchParams.get('window'));
  const metric = parseMetricParam(searchParams.get('metric'));

  const today = todayDateOnly();
  const dashboardQuery = useRecoveryDashboard(windowDays, today, true);
  const trendRange = dateRangeForTrend(TREND_RANGE_DAYS, today);
  const trendQuery = useRecoveryMetricTrend(metric, trendRange.startDate, trendRange.endDate, true);
  const discomfortRange = { startDate: subtractDays(today, 89), endDate: today };
  const discomfortQuery = useBodyAreaDiscomfortHistory(discomfortRange.startDate, discomfortRange.endDate);

  function updateParams(next: { window?: BaselineWindowDays; metric?: RecoveryMetricType }) {
    const params = new URLSearchParams(searchParams);
    if (next.window != null) {
      params.set('window', String(next.window));
    }
    if (next.metric != null) {
      params.set('metric', next.metric);
    }
    setSearchParams(params, { replace: true });
  }

  return (
    <Page
      title="Recovery analytics"
      description="Baselines, trends, and discomfort patterns over time."
      width="wide"
    >
      <RecoverySubNav />

      <div className={surfaces.hub}>
        <section className={surfaces.toolbar} aria-label="Analytics filters">
          <label className={surfaces.filter}>
            <span className={surfaces.filterLabel}>Baseline window</span>
            <select
              className="input"
              value={windowDays}
              onChange={(event) => {
                const next = Number(event.target.value);
                if (isBaselineWindowDays(next)) {
                  updateParams({ window: next });
                }
              }}
            >
              {BASELINE_WINDOW_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option} days
                </option>
              ))}
            </select>
          </label>

          <label className={surfaces.filter}>
            <span className={surfaces.filterLabel}>Trend metric</span>
            <select
              className="input"
              value={metric}
              onChange={(event) => {
                if (isRecoveryMetricType(event.target.value)) {
                  updateParams({ metric: event.target.value });
                }
              }}
            >
              {RECOVERY_METRIC_TYPES.map((option) => (
                <option key={option} value={option}>
                  {metricTypeLabel(option)}
                </option>
              ))}
            </select>
          </label>
        </section>

        <section className={surfaces.panel} aria-labelledby="analytics-baselines-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="analytics-baselines-heading">
              Baselines &amp; comparisons
            </h2>
            <span className={surfaces.panelHint}>{windowDays}-day window</span>
          </div>
          {dashboardQuery.isLoading ? <LoadingView message="Loading baselines…" /> : null}
          {dashboardQuery.isError ? (
            <ErrorView message={recoveryErrorMessage(dashboardQuery.error)} onRetry={() => dashboardQuery.refetch()} />
          ) : null}
          {dashboardQuery.data ? (
            <BaselineDeviationTable
              baselines={dashboardQuery.data.baselines}
              deviations={dashboardQuery.data.metricDeviations}
            />
          ) : null}
        </section>

        <section className={surfaces.panel} aria-labelledby="analytics-trend-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="analytics-trend-heading">
              {metricTypeLabel(metric)} trend
            </h2>
          </div>
          {trendQuery.isLoading ? <LoadingView message="Loading trend…" /> : null}
          {trendQuery.isError ? (
            <ErrorView message={recoveryErrorMessage(trendQuery.error)} onRetry={() => trendQuery.refetch()} />
          ) : null}
          {trendQuery.data ? <MetricTrendPanel trend={trendQuery.data} /> : null}
        </section>

        <section className={surfaces.panel} aria-labelledby="analytics-discomfort-heading">
          <div className={surfaces.panelHeader}>
            <h2 className={surfaces.panelTitle} id="analytics-discomfort-heading">
              Discomfort history
            </h2>
          </div>
          {discomfortQuery.isLoading ? <LoadingView message="Loading discomfort history…" /> : null}
          {discomfortQuery.isError ? (
            <ErrorView
              message={recoveryErrorMessage(discomfortQuery.error)}
              onRetry={() => discomfortQuery.refetch()}
            />
          ) : null}
          {discomfortQuery.data ? <DiscomfortHistoryTable history={discomfortQuery.data} /> : null}
        </section>
      </div>
    </Page>
  );
}
