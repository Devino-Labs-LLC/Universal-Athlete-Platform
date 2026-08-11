import tableStyles from '@/core/components/Table.module.scss';
import { adjustmentTypeLabel } from '@/features/recovery/models/labels';
import type { TrainingRecommendationAdjustment } from '@/features/recovery/models/schemas';

interface RecommendationAdjustmentsListProps {
  adjustments: TrainingRecommendationAdjustment[];
}

export function RecommendationAdjustmentsList({ adjustments }: RecommendationAdjustmentsListProps) {
  if (adjustments.length === 0) {
    return <p className={tableStyles.subtle}>No specific adjustments were suggested.</p>;
  }

  const sorted = [...adjustments].sort((a, b) => a.orderIndex - b.orderIndex);

  return (
    <ol style={{ display: 'grid', gap: '0.5rem', paddingLeft: '1.25rem' }}>
      {sorted.map((adjustment) => (
        <li key={adjustment.adjustmentId}>
          <strong>{adjustmentTypeLabel(adjustment.type)}</strong>
          {adjustment.sourceDimensions && adjustment.sourceDimensions.length > 0 ? (
            <span className={tableStyles.subtle}> — related to {adjustment.sourceDimensions.join(', ')}</span>
          ) : null}
        </li>
      ))}
    </ol>
  );
}
