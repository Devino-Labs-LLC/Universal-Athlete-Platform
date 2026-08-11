import { Link } from 'react-router-dom';

import tableStyles from '@/core/components/Table.module.scss';
import { formatDistance, formatDurationSeconds, formatVolumeKg } from '@/features/performance/utils/formatMetrics';
import { formatPerformanceMetricsSummary } from '@/features/performance/utils/formatPerformanceMetrics';
import type { WorkoutOccurrencePerformance } from '@/features/performance/models/schemas';

interface OccurrencePerformanceSummaryProps {
  performance: WorkoutOccurrencePerformance;
}

export function OccurrencePerformanceSummary({ performance }: OccurrencePerformanceSummaryProps) {
  const { totals } = performance;

  return (
    <div>
      <div className="statGrid">
        <div className="stat">
          <span className="statLabel">Completed exercises</span>
          <span className="statValue">{totals.completedExerciseCount}</span>
        </div>
        <div className="stat">
          <span className="statLabel">Completed sets</span>
          <span className="statValue">{totals.completedSetCount}</span>
        </div>
        <div className="stat">
          <span className="statLabel">Volume</span>
          <span className="statValue">
            {totals.totalVolumeKilogramRepetitions != null
              ? formatVolumeKg(totals.totalVolumeKilogramRepetitions)
              : '—'}
          </span>
        </div>
        <div className="stat">
          <span className="statLabel">Duration</span>
          <span className="statValue">
            {totals.totalDurationSeconds != null ? formatDurationSeconds(totals.totalDurationSeconds) : '—'}
          </span>
        </div>
        <div className="stat">
          <span className="statLabel">Distance</span>
          <span className="statValue">
            {totals.totalDistanceMeters != null ? formatDistance(totals.totalDistanceMeters) : '—'}
          </span>
        </div>
        <div className="stat">
          <span className="statLabel">Average RPE</span>
          <span className="statValue">{totals.averageRpe != null ? Number(totals.averageRpe).toFixed(1) : 'Not rated'}</span>
        </div>
      </div>

      {performance.exercises.length > 0 ? (
        <table className={tableStyles.table} style={{ marginTop: '1rem' }}>
          <caption className="srOnly">Exercises performed this session</caption>
          <thead>
            <tr>
              <th scope="col">Exercise</th>
              <th scope="col">Summary</th>
            </tr>
          </thead>
          <tbody>
            {performance.exercises.map((exercise) => (
              <tr key={exercise.executionId}>
                <th scope="row">
                  <Link className={tableStyles.link} to={`/app/performance/exercises/${exercise.exercisePerformanceKey}`}>
                    {exercise.exerciseName}
                  </Link>
                </th>
                <td>{formatPerformanceMetricsSummary(exercise.metrics)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  );
}
