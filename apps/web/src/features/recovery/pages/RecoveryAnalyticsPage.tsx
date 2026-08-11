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
import { useBodyAreaDiscomfortHistory, useRecoveryDashboard, useRecoveryMetricTrend } from '@/features/recovery/hooks/useRecoveryAnalytics';
import {
  BASELINE_WINDOW_OPTIONS,
  isBaselineWindowDays,
  isRecoveryMetricType,
  RECOVERY_METRIC_TYPES,
  type BaselineWindowDays,
  type RecoveryMetricType,
} from '@/features/recovery/models/schemas';
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
    <Page title="Recovery analytics" description="Baselines, trends, and discomfort patterns over time.">
      <RecoverySubNav />

      <section className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '1.5rem', flexWrap: 'wrap' }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <span className="label">Baseline window</span>
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

          <label style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
            <span className="label">Trend metric</span>
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
        </div>
      </section>

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">Baselines &amp; comparisons ({windowDays}-day window)</h2>
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

      <section className="card" style={{ marginBottom: '1rem' }}>
        <h2 className="cardTitle">{metricTypeLabel(metric)} trend</h2>
        {trendQuery.isLoading ? <LoadingView message="Loading trend…" /> : null}
        {trendQuery.isError ? (
          <ErrorView message={recoveryErrorMessage(trendQuery.error)} onRetry={() => trendQuery.refetch()} />
        ) : null}
        {trendQuery.data ? <MetricTrendPanel trend={trendQuery.data} /> : null}
      </section>

      <section className="card">
        <h2 className="cardTitle">Discomfort history</h2>
        {discomfortQuery.isLoading ? <LoadingView message="Loading discomfort history…" /> : null}
        {discomfortQuery.isError ? (
          <ErrorView message={recoveryErrorMessage(discomfortQuery.error)} onRetry={() => discomfortQuery.refetch()} />
        ) : null}
        {discomfortQuery.data ? <DiscomfortHistoryTable history={discomfortQuery.data} /> : null}
      </section>
    </Page>
  );
}
