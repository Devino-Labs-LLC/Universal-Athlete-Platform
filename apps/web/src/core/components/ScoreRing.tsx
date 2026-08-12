import styles from '@/core/components/ScoreRing.module.scss';

interface ScoreRingProps {
  /** Numeric score when present; null/undefined renders empty ring. */
  score: number | null | undefined;
  max?: number;
  size?: number;
  label?: string;
  emptyLabel?: string;
  tone?: 'accent' | 'cyan' | 'warning' | 'danger' | 'muted';
}

export function ScoreRing({
  score,
  max = 100,
  size = 112,
  label,
  emptyLabel = '—',
  tone = 'accent',
}: ScoreRingProps) {
  const stroke = 8;
  const radius = (size - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const hasScore = score != null && Number.isFinite(score);
  const clamped = hasScore ? Math.min(Math.max(score, 0), max) : 0;
  const progress = hasScore ? clamped / max : 0;
  const dashOffset = circumference * (1 - progress);

  return (
    <div
      className={[styles.wrap, styles[tone]].join(' ')}
      style={{ width: size, height: size }}
      role="img"
      aria-label={
        hasScore
          ? `${label ?? 'Score'}: ${Math.round(clamped)} of ${max}`
          : `${label ?? 'Score'}: not available`
      }
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className={styles.svg}>
        <circle
          className={styles.track}
          cx={size / 2}
          cy={size / 2}
          r={radius}
          strokeWidth={stroke}
          fill="none"
        />
        {hasScore ? (
          <circle
            className={styles.progress}
            cx={size / 2}
            cy={size / 2}
            r={radius}
            strokeWidth={stroke}
            fill="none"
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
            strokeLinecap="round"
          />
        ) : null}
      </svg>
      <div className={styles.value}>
        <span className={styles.number}>{hasScore ? Math.round(clamped) : emptyLabel}</span>
        {label ? <span className={styles.label}>{label}</span> : null}
      </div>
    </div>
  );
}
