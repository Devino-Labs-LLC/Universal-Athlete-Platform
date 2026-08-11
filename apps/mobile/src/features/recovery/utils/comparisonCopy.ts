import { comparisonBandLabel, metricTypeLabel } from '@/src/features/recovery/models/recoveryLabels';

export function deviationSummaryCopy(
  metricType: string,
  comparisonBand: string,
): string {
  const metric = metricTypeLabel(metricType);

  // Prefer factual metric-specific phrasing where direction matters.
  if (metricType === 'FATIGUE') {
    if (comparisonBand === 'ABOVE_BASELINE' || comparisonBand === 'FAR_ABOVE_BASELINE') {
      return 'Above your usual reported fatigue';
    }
    if (comparisonBand === 'BELOW_BASELINE' || comparisonBand === 'FAR_BELOW_BASELINE') {
      return 'Below your usual reported fatigue';
    }
  }

  const band = comparisonBandLabel(comparisonBand);
  return `${metric}: ${band}`;
}

export function trendSummaryCopy(metricType: string, trendDirection: string): string {
  const metric = metricTypeLabel(metricType);
  const direction = trendDirection.toLowerCase().replace(/_/g, ' ');
  return `${metric} trend: ${direction}.`;
}
