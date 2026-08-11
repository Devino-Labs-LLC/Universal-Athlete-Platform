import tableStyles from '@/core/components/Table.module.scss';
import { exerciseCategoryLabel, movementPatternLabel } from '@/features/performance/models/labels';
import { formatVolumeKg } from '@/features/performance/utils/formatMetrics';
import type { WorkoutLoadCategorySummary, WorkoutLoadMovementSummary } from '@/features/performance/models/schemas';

interface CategoryDistributionTableProps {
  summaries: WorkoutLoadCategorySummary[];
}

function BarCell({ value, max }: { value: number; max: number }) {
  const width = max > 0 ? Math.max(4, Math.round((value / max) * 100)) : 0;
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <div
        style={{
          height: '0.6rem',
          width: `${width}%`,
          minWidth: value > 0 ? '4px' : 0,
          background: 'var(--uap-accent)',
          borderRadius: '999px',
        }}
        aria-hidden="true"
      />
      <span className={tableStyles.numeric}>{formatVolumeKg(value)}</span>
    </div>
  );
}

export function CategoryDistributionTable({ summaries }: CategoryDistributionTableProps) {
  if (summaries.length === 0) {
    return <p className={tableStyles.subtle}>No category breakdown available.</p>;
  }
  const max = Math.max(...summaries.map((s) => Number(s.volumeKilograms ?? 0)));

  return (
    <table className={tableStyles.table}>
      <caption className="srOnly">Training load by exercise category</caption>
      <thead>
        <tr>
          <th scope="col">Category</th>
          <th scope="col">Sets</th>
          <th scope="col">Volume</th>
        </tr>
      </thead>
      <tbody>
        {summaries.map((summary) => (
          <tr key={summary.category}>
            <th scope="row">{exerciseCategoryLabel(summary.category)}</th>
            <td className={tableStyles.numeric}>{summary.completedSetCount}</td>
            <td>
              <BarCell value={Number(summary.volumeKilograms ?? 0)} max={max} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

interface MovementDistributionTableProps {
  summaries: WorkoutLoadMovementSummary[];
}

export function MovementDistributionTable({ summaries }: MovementDistributionTableProps) {
  if (summaries.length === 0) {
    return <p className={tableStyles.subtle}>No movement pattern breakdown available.</p>;
  }
  const max = Math.max(...summaries.map((s) => Number(s.volumeKilograms ?? 0)));

  return (
    <table className={tableStyles.table}>
      <caption className="srOnly">Training load by movement pattern</caption>
      <thead>
        <tr>
          <th scope="col">Movement pattern</th>
          <th scope="col">Sets</th>
          <th scope="col">Volume</th>
        </tr>
      </thead>
      <tbody>
        {summaries.map((summary) => (
          <tr key={summary.primaryMovementPattern}>
            <th scope="row">{movementPatternLabel(summary.primaryMovementPattern)}</th>
            <td className={tableStyles.numeric}>{summary.completedSetCount}</td>
            <td>
              <BarCell value={Number(summary.volumeKilograms ?? 0)} max={max} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
