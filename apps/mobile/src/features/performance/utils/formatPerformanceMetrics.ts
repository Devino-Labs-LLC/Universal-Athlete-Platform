import {
  formatDecimal,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import { ExercisePerformanceMetrics } from '@/src/features/performance/models/performanceSchemas';
import { formatPerformanceMeasurement } from '@/src/features/performance/utils/formatPersonalRecord';

export function formatPerformanceMetricsSummary(metrics: ExercisePerformanceMetrics): string {
  const parts: string[] = [];

  if (metrics.completedSetCount > 0) {
    parts.push(`${metrics.completedSetCount} sets`);
  }
  if (metrics.totalRepetitions != null) {
    parts.push(`${metrics.totalRepetitions} reps`);
  }
  if (metrics.heaviestWeight) {
    const weight = formatPerformanceMeasurement(metrics.heaviestWeight);
    if (weight) {
      parts.push(weight);
    }
  }
  if (metrics.totalVolume) {
    const volume = formatPerformanceMeasurement(metrics.totalVolume);
    if (volume) {
      parts.push(`vol ${volume}`);
    } else if (metrics.totalVolume.normalizedValue != null) {
      parts.push(`vol ${formatVolumeKg(metrics.totalVolume.normalizedValue)}`);
    }
  }
  if (metrics.totalDurationSeconds != null) {
    parts.push(formatDurationSeconds(metrics.totalDurationSeconds));
  }
  if (metrics.totalDistance) {
    const distance = formatPerformanceMeasurement(metrics.totalDistance);
    if (distance) {
      parts.push(distance);
    }
  }
  if (metrics.averageRpe != null) {
    parts.push(`RPE ${formatDecimal(metrics.averageRpe)}`);
  }

  return parts.length > 0 ? parts.join(' · ') : 'No metrics recorded';
}

export function performanceMetricsPrIndicators(metrics: ExercisePerformanceMetrics): string[] {
  const indicators: string[] = [];
  if (metrics.bestEstimatedOneRepMax?.estimated) {
    indicators.push('Est. 1RM');
  }
  if (metrics.heaviestWeight && !metrics.heaviestWeight.estimated) {
    indicators.push('Heaviest');
  }
  if (metrics.bestSetVolume) {
    indicators.push('Best vol');
  }
  if (metrics.longestSetDistance) {
    indicators.push('Distance PR');
  }
  if (metrics.longestSetDurationSeconds != null) {
    indicators.push('Duration PR');
  }
  return indicators;
}
