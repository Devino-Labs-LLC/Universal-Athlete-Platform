import {
  formatDecimal,
  formatDistance,
  formatDurationSeconds,
  formatVolumeKg,
} from '@/src/features/home/utils/formatMetrics';
import {
  WeeklyTrainingLoadSummary,
  WorkoutOccurrenceLoadSummary,
} from '@/src/features/performance/models/performanceSchemas';

interface RatedUnratedSummary {
  ratedOccurrenceCount: number;
  unratedOccurrenceCount: number;
}

interface LoadMetricsSummary extends RatedUnratedSummary {
  totalVolumeKilograms?: number | string | null;
  totalDurationSeconds: number;
  totalDistanceMeters?: number | string | null;
  totalSessionRpeLoad?: number | string | null;
  averageSessionRpe?: number | string | null;
}

export function formatRatedUnratedSummary(summary: RatedUnratedSummary): string {
  const parts: string[] = [];
  if (summary.ratedOccurrenceCount > 0) {
    parts.push(`${summary.ratedOccurrenceCount} rated`);
  }
  if (summary.unratedOccurrenceCount > 0) {
    parts.push(`${summary.unratedOccurrenceCount} unrated`);
  }
  return parts.join(' · ');
}

export function formatSessionRpeLoad(
  load: number | string | null | undefined,
): string | null {
  if (load == null) {
    return null;
  }
  return formatDecimal(load);
}

export function formatLoadVolume(volume: number | string | null | undefined): string | null {
  if (volume == null) {
    return null;
  }
  return formatVolumeKg(volume);
}

export function formatDailyLoadSummary(summary: LoadMetricsSummary): string[] {
  const lines: string[] = [];
  lines.push(formatRatedUnratedSummary(summary));
  const volume = formatLoadVolume(summary.totalVolumeKilograms);
  if (volume) {
    lines.push(`Volume: ${volume}`);
  }
  if (summary.totalDurationSeconds > 0) {
    lines.push(`Duration: ${formatDurationSeconds(summary.totalDurationSeconds)}`);
  }
  const distance = summary.totalDistanceMeters;
  if (distance != null && Number(distance) > 0) {
    lines.push(`Distance: ${formatDistance(distance)}`);
  }
  const rpeLoad = formatSessionRpeLoad(summary.totalSessionRpeLoad);
  if (rpeLoad != null) {
    lines.push(`Session load: ${rpeLoad}`);
  } else if (summary.unratedOccurrenceCount > 0) {
    lines.push('Session load: not rated');
  }
  if (summary.averageSessionRpe != null) {
    lines.push(`Avg RPE: ${formatDecimal(summary.averageSessionRpe)}`);
  }
  return lines.filter(Boolean);
}

export function formatWeeklyLoadSummary(summary: WeeklyTrainingLoadSummary): string[] {
  const lines = formatDailyLoadSummary(summary);
  lines.unshift(`${summary.trainingDays} training days`);
  return lines;
}

export function formatOccurrenceLoadSummary(summary: WorkoutOccurrenceLoadSummary): string[] {
  const lines: string[] = [];
  const volume = formatLoadVolume(summary.totalVolumeKilograms);
  if (volume) {
    lines.push(`Volume: ${volume}`);
  }
  if (summary.totalDurationSeconds != null && summary.totalDurationSeconds > 0) {
    lines.push(`Duration: ${formatDurationSeconds(summary.totalDurationSeconds)}`);
  }
  const distance = summary.totalDistanceMeters;
  if (distance != null && Number(distance) > 0) {
    lines.push(`Distance: ${formatDistance(distance)}`);
  }
  const rpeLoad = formatSessionRpeLoad(summary.sessionRpeLoad);
  if (rpeLoad != null) {
    lines.push(`Session load: ${rpeLoad}`);
  } else {
    lines.push('Session load: not rated');
  }
  if (summary.sessionRpe != null) {
    lines.push(`RPE: ${formatDecimal(summary.sessionRpe)}`);
  }
  return lines;
}

export function isOccurrenceRated(summary: WorkoutOccurrenceLoadSummary): boolean {
  return summary.sessionRpeLoad != null;
}
