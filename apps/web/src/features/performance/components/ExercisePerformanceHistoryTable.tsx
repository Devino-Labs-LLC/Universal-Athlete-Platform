import { Badge } from '@/core/components/Badge';
import tableStyles from '@/core/components/Table.module.scss';
import { formatDateDisplay, parseDateOnly } from '@/core/date/dateOnly';
import {
  formatPerformanceMetricsSummary,
  performanceMetricsPrIndicators,
} from '@/features/performance/utils/formatPerformanceMetrics';
import type { ExerciseExecutionPerformance } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';
import { performanceStatusBadgeTone } from '@/features/performance/utils/performanceVisual';

interface ExercisePerformanceHistoryTableProps {
  entries: ExerciseExecutionPerformance[];
}

export function ExercisePerformanceHistoryTable({ entries }: ExercisePerformanceHistoryTableProps) {
  if (entries.length === 0) {
    return <p className={tableStyles.subtle}>No completed sessions found for this exercise.</p>;
  }

  return (
    <div className={surfaces.tableWrap}>
      <table className={tableStyles.table}>
        <caption className="srOnly">Exercise performance history</caption>
        <thead>
          <tr>
            <th scope="col">Date</th>
            <th scope="col">Status</th>
            <th scope="col">Summary</th>
            <th scope="col">Indicators</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((entry) => (
            <tr key={entry.executionId}>
              <th scope="row">{formatDateDisplay(parseDateOnly(entry.scheduledDate))}</th>
              <td>
                <Badge tone={performanceStatusBadgeTone(entry.status)}>{entry.status}</Badge>
              </td>
              <td>{formatPerformanceMetricsSummary(entry.metrics)}</td>
              <td>{performanceMetricsPrIndicators(entry.metrics).join(', ') || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
