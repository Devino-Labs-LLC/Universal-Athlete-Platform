import tableStyles from '@/core/components/Table.module.scss';
import { ComparisonBandBadge } from '@/features/recovery/components/ComparisonBandBadge';
import { readinessDimensionLabel } from '@/features/recovery/models/labels';
import type { ReadinessContribution } from '@/features/recovery/models/schemas';
import surfaces from '@/features/recovery/styles/recoverySurfaces.module.scss';

interface ReadinessContributionsTableProps {
  contributions: ReadinessContribution[];
}

function formatNumber(value: number | null | undefined, decimals = 2): string {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  return value.toFixed(decimals);
}

export function ReadinessContributionsTable({ contributions }: ReadinessContributionsTableProps) {
  if (contributions.length === 0) {
    return <p className={tableStyles.subtle}>No contributing dimensions were available.</p>;
  }

  return (
    <div className={surfaces.tableWrap}>
      <table className={tableStyles.table}>
        <caption className="srOnly">Readiness contribution breakdown by dimension</caption>
        <thead>
          <tr>
            <th scope="col">Dimension</th>
            <th scope="col">Available</th>
            <th scope="col">Comparison</th>
            <th scope="col">Normalized score</th>
            <th scope="col">Weight</th>
            <th scope="col">Weighted contribution</th>
          </tr>
        </thead>
        <tbody>
          {contributions.map((contribution) => (
            <tr key={contribution.dimensionType}>
              <th scope="row">{readinessDimensionLabel(contribution.dimensionType)}</th>
              <td>{contribution.available ? 'Yes' : 'No'}</td>
              <td>
                <ComparisonBandBadge band={contribution.comparisonBand} />
              </td>
              <td className={tableStyles.numeric}>{formatNumber(contribution.normalizedScore)}</td>
              <td className={tableStyles.numeric}>{formatNumber(contribution.effectiveWeight)}</td>
              <td className={tableStyles.numeric}>{formatNumber(contribution.weightedContribution)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
