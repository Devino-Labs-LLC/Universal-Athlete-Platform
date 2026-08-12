import { Link } from 'react-router-dom';

import { Badge } from '@/core/components/Badge';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDistance, formatDurationSeconds, formatVolumeKg } from '@/features/performance/utils/formatMetrics';
import { formatPerformanceMetricsSummary } from '@/features/performance/utils/formatPerformanceMetrics';
import type { WorkoutOccurrencePerformance } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
import { performanceStatusBadgeTone } from '@/features/performance/utils/performanceVisual';

interface OccurrencePerformanceSummaryProps {
  performance: WorkoutOccurrencePerformance;
}

export function OccurrencePerformanceSummary({ performance }: OccurrencePerformanceSummaryProps) {
  const { totals } = performance;

  return (
    <div className={surfaces.hub} style={{ gap: 'var(--uap-space-3)' }}>
      <div className={surfaces.kpiRow}>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Completed exercises</span>
          <span className={surfaces.metricValue}>{totals.completedExerciseCount}</span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Completed sets</span>
          <span className={surfaces.metricValue}>{totals.completedSetCount}</span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Volume</span>
          <span className={surfaces.metricValue}>
            {totals.totalVolumeKilogramRepetitions != null
              ? formatVolumeKg(totals.totalVolumeKilogramRepetitions)
              : '—'}
          </span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Duration</span>
          <span className={surfaces.metricValue}>
            {totals.totalDurationSeconds != null ? formatDurationSeconds(totals.totalDurationSeconds) : '—'}
          </span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Distance</span>
          <span className={surfaces.metricValue}>
            {totals.totalDistanceMeters != null ? formatDistance(totals.totalDistanceMeters) : '—'}
          </span>
        </div>
        <div className={surfaces.metricTile}>
          <span className={surfaces.metricLabel}>Average RPE</span>
          <span className={surfaces.metricValue}>
            {totals.averageRpe != null ? Number(totals.averageRpe).toFixed(1) : 'Not rated'}
          </span>
        </div>
      </div>

      {performance.exercises.length > 0 ? (
        <div className={surfaces.tableWrap}>
          <table className={tableStyles.table}>
            <caption className="srOnly">Exercises performed this session</caption>
            <thead>
              <tr>
                <th scope="col">Exercise</th>
                <th scope="col">Status</th>
                <th scope="col">Summary</th>
              </tr>
            </thead>
            <tbody>
              {performance.exercises.map((exercise) => (
                <tr key={exercise.executionId}>
                  <th scope="row">
                    <Link
                      className={tableStyles.link}
                      to={`/app/performance/exercises/${exercise.exercisePerformanceKey}`}
                    >
                      {exercise.exerciseName}
                    </Link>
                  </th>
                  <td>
                    <Badge tone={performanceStatusBadgeTone(exercise.status)}>{exercise.status}</Badge>
                  </td>
                  <td>{formatPerformanceMetricsSummary(exercise.metrics)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </div>
  );
}
