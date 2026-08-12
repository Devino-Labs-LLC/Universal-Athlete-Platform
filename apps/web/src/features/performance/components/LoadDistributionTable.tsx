import tableStyles from '@/core/components/Table.module.scss';
import { exerciseCategoryLabel, movementPatternLabel } from '@/features/performance/models/labels';
import { formatVolumeKg } from '@/features/performance/utils/formatMetrics';
import type { WorkoutLoadCategorySummary, WorkoutLoadMovementSummary } from '@/features/performance/models/schemas';
import surfaces from '@/features/performance/styles/performanceSurfaces.module.scss';

interface CategoryDistributionTableProps {
  summaries: WorkoutLoadCategorySummary[];
}

function volumeNumber(value: number | string | null | undefined): number | null {
  if (value == null) {
    return null;
  }
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : null;
}

function BarCell({ value, max }: { value: number | null; max: number }) {
  if (value == null) {
    return <span className={tableStyles.numeric}>—</span>;
  }
  const width = max > 0 ? Math.max(4, Math.round((value / max) * 100)) : 0;
  return (
    <div className={surfaces.barTrack}>
      <div
        className={surfaces.barFill}
        style={{
          width: `${width}%`,
          minWidth: value > 0 ? '4px' : 0,
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
  const volumes = summaries.map((summary) => volumeNumber(summary.volumeKilograms)).filter((value): value is number => value != null);
  const max = volumes.length > 0 ? Math.max(...volumes) : 0;

  return (
    <div className={surfaces.tableWrap}>
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
                <BarCell value={volumeNumber(summary.volumeKilograms)} max={max} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

interface MovementDistributionTableProps {
  summaries: WorkoutLoadMovementSummary[];
}

export function MovementDistributionTable({ summaries }: MovementDistributionTableProps) {
  if (summaries.length === 0) {
    return <p className={tableStyles.subtle}>No movement pattern breakdown available.</p>;
  }
  const volumes = summaries.map((summary) => volumeNumber(summary.volumeKilograms)).filter((value): value is number => value != null);
  const max = volumes.length > 0 ? Math.max(...volumes) : 0;

  return (
    <div className={surfaces.tableWrap}>
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
                <BarCell value={volumeNumber(summary.volumeKilograms)} max={max} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
