import tableStyles from '@/core/components/Table.module.scss';
import { ComparisonBandBadge, SufficiencyBadge } from '@/features/recovery/components/ComparisonBandBadge';
import { deviationSummaryCopy } from '@/features/recovery/utils/comparisonCopy';
import { metricTypeLabel } from '@/features/recovery/models/labels';
import type { RecoveryMetricBaseline, RecoveryMetricDeviation } from '@/features/recovery/models/schemas';

interface BaselineDeviationTableProps {
  baselines: RecoveryMetricBaseline[];
  deviations: RecoveryMetricDeviation[];
}

function formatNumber(value: number | null | undefined, decimals = 1): string {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  return value.toFixed(decimals);
}

export function BaselineDeviationTable({ baselines, deviations }: BaselineDeviationTableProps) {
  const deviationByMetric = new Map(deviations.map((deviation) => [deviation.metricType, deviation]));
  const metricTypes = Array.from(
    new Set([...baselines.map((baseline) => baseline.metricType), ...deviations.map((deviation) => deviation.metricType)]),
  );

  if (metricTypes.length === 0) {
    return <p className={tableStyles.subtle}>No baseline data available for this window yet.</p>;
  }

  return (
    <table className={tableStyles.table}>
      <caption className="srOnly">Recovery metric baselines and comparisons</caption>
      <thead>
        <tr>
          <th scope="col">Metric</th>
          <th scope="col">Today</th>
          <th scope="col">Baseline mean</th>
          <th scope="col">Comparison</th>
          <th scope="col">Summary</th>
          <th scope="col">Baseline data</th>
          <th scope="col">Observations</th>
        </tr>
      </thead>
      <tbody>
        {metricTypes.map((metricType) => {
          const baseline = baselines.find((entry) => entry.metricType === metricType);
          const deviation = deviationByMetric.get(metricType);
          return (
            <tr key={metricType}>
              <th scope="row">{metricTypeLabel(metricType)}</th>
              <td className={tableStyles.numeric}>{formatNumber(deviation?.targetValue)}</td>
              <td className={tableStyles.numeric}>{formatNumber(baseline?.mean)}</td>
              <td>
                <ComparisonBandBadge band={deviation?.comparisonBand} />
              </td>
              <td>{deviation ? deviationSummaryCopy(metricType, deviation.comparisonBand) : '—'}</td>
              <td>
                <SufficiencyBadge sufficiency={baseline?.dataSufficiency ?? deviation?.dataSufficiency} />
              </td>
              <td className={tableStyles.numeric}>{baseline?.observationCount ?? 0}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
