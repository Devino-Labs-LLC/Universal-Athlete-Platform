import { labelsForMetric, metricDisplayName, type RecoveryRatingMetric } from '@/features/recovery/models/labels';
import styles from '@/features/recovery/components/RatingScale.module.scss';

const VALUES = [1, 2, 3, 4, 5] as const;

interface RatingScaleProps {
  metric: RecoveryRatingMetric;
  value?: number;
  onChange: (value: number | undefined) => void;
  optional?: boolean;
  error?: string;
}

export function RatingScale({ metric, value, onChange, optional = false, error }: RatingScaleProps) {
  const title = metricDisplayName(metric);
  const labels = labelsForMetric(metric);
  const labelId = `rating-label-${metric}`;
  const errorId = `rating-error-${metric}`;
  const groupId = `rating-${metric}`;

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <div className={styles.titleRow}>
          <span className={styles.label} id={labelId}>
            {title}
          </span>
          {optional ? <span className={styles.optional}>Optional</span> : null}
        </div>
        {optional && value != null ? (
          <button
            type="button"
            className={styles.clear}
            onClick={() => onChange(undefined)}
            aria-label={`Clear ${title}`}
          >
            Not reported
          </button>
        ) : null}
      </div>
      <div
        id={groupId}
        role="radiogroup"
        aria-labelledby={labelId}
        aria-describedby={error ? errorId : undefined}
        aria-invalid={error ? true : undefined}
        className={styles.row}
      >
        {VALUES.map((rating) => {
          const selected = value === rating;
          const ratingLabel = labels[rating] ?? String(rating);
          return (
            <button
              key={rating}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={`${title}, ${ratingLabel}, ${rating} of 5`}
              className={[styles.option, selected ? styles.selected : ''].filter(Boolean).join(' ')}
              onClick={() => onChange(rating)}
            >
              <span className={styles.number}>{rating}</span>
              <span className={styles.caption}>{ratingLabel}</span>
            </button>
          );
        })}
      </div>
      {error ? (
        <p className="fieldError" id={errorId}>
          {error}
        </p>
      ) : null}
    </div>
  );
}
