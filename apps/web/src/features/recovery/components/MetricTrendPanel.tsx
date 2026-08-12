import { Badge } from '@/core/components/Badge';
import { Sparkline } from '@/core/components/Sparkline';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import { trendSummaryCopy } from '@/features/recovery/utils/comparisonCopy';
import { metricTypeLabel, trendDirectionLabel } from '@/features/recovery/models/labels';
import { trendPointNumericValue, type RecoveryMetricTrend } from '@/features/recovery/models/schemas';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';
import { trendDirectionBadgeTone } from '@/features/recovery/utils/readinessVisual';

interface MetricTrendPanelProps {
  trend: RecoveryMetricTrend;
}

function formatNumber(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  return value.toFixed(1);
}

export function MetricTrendPanel({ trend }: MetricTrendPanelProps) {
  const points = trend.points.map((point) => ({
    label: point.date,
    value: trendPointNumericValue(point.value),
  }));
  const latestPoint = [...trend.points].reverse().find((point) => trendPointNumericValue(point.value) != null);
  const currentValue = latestPoint ? trendPointNumericValue(latestPoint.value) : null;

  return (
    <div className={surfaces.hub} style={{ gap: 'var(--uap-space-3)' }}>
      <div className={surfaces.metaRow}>
        <p className={surfaces.trendSummary}>{trendSummaryCopy(trend.metricType, trend.trendDirection)}</p>
        <Badge tone={trendDirectionBadgeTone(trend.trendDirection)}>
          {trendDirectionLabel(trend.trendDirection)}
        </Badge>
      </div>

      <div className={surfaces.kpiRow}>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Current</span>
          <span className={surfaces.metricValue}>{formatNumber(currentValue)}</span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Observations</span>
          <span className={surfaces.metricValue}>{trend.observationCount}</span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Period</span>
          <span className={surfaces.metricValue} style={{ fontSize: 'var(--uap-font-size-sm)' }}>
            {formatDateDisplay(parseDateOnly(trend.startDate))} – {formatDateDisplay(parseDateOnly(trend.endDate))}
          </span>
        </div>
      </div>

      <p className={tableStyles.subtle}>
        {trend.observationCount} observation{trend.observationCount === 1 ? '' : 's'} between{' '}
        {formatDateDisplay(parseDateOnly(trend.startDate))} and {formatDateDisplay(parseDateOnly(trend.endDate))}
      </p>

      {trend.points.length > 0 ? (
        <div>
          <Sparkline
            points={points}
            ariaLabel={`${metricTypeLabel(trend.metricType)} trend`}
            valueFormatter={formatNumber}
          />
        </div>
      ) : null}

      {trend.points.length === 0 ? (
        <p className={tableStyles.subtle}>No observations recorded in this date range.</p>
      ) : (
        <div className={surfaces.tableWrap}>
          <table className={tableStyles.table}>
            <caption className="srOnly">{metricTypeLabel(trend.metricType)} daily values</caption>
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Value</th>
                <th scope="col">3-day avg</th>
                <th scope="col">7-day avg</th>
              </tr>
            </thead>
            <tbody>
              {trend.points.map((point) => (
                <tr key={point.date}>
                  <th scope="row">{formatDateDisplay(parseDateOnly(point.date))}</th>
                  <td className={tableStyles.numeric}>{formatNumber(trendPointNumericValue(point.value))}</td>
                  <td className={tableStyles.numeric}>{formatNumber(point.rollingAverage3)}</td>
                  <td className={tableStyles.numeric}>{formatNumber(point.rollingAverage7)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
