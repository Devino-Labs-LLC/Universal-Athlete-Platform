import styles from '@/core/components/Sparkline.module.scss';

export interface SparklinePoint {
  label: string;
  value: number | null;
}

interface SparklineProps {
  points: SparklinePoint[];
  width?: number;
  height?: number;
  ariaLabel: string;
  valueFormatter?: (value: number) => string;
}

const PADDING = 4;

export function Sparkline({ points, width = 240, height = 56, ariaLabel, valueFormatter }: SparklineProps) {
  const numericPoints = points
    .map((point, index) => ({ ...point, index }))
    .filter((point): point is SparklinePoint & { index: number; value: number } => point.value != null);

  if (numericPoints.length === 0) {
    return (
      <div className={styles.empty} role="img" aria-label={`${ariaLabel}: no data available`}>
        No data
      </div>
    );
  }

  const values = numericPoints.map((point) => point.value);
  const minValue = Math.min(...values);
  const maxValue = Math.max(...values);
  const range = maxValue - minValue || 1;
  const lastIndex = points.length - 1 || 1;

  const toX = (index: number) => PADDING + (index / lastIndex) * (width - PADDING * 2);
  const toY = (value: number) => height - PADDING - ((value - minValue) / range) * (height - PADDING * 2);

  const path = numericPoints
    .map((point, i) => `${i === 0 ? 'M' : 'L'}${toX(point.index).toFixed(1)},${toY(point.value).toFixed(1)}`)
    .join(' ');

  const lastPoint = numericPoints[numericPoints.length - 1]!;
  const summary = valueFormatter ? valueFormatter(lastPoint.value) : String(lastPoint.value);

  return (
    <svg
      className={styles.sparkline}
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      role="img"
      aria-label={`${ariaLabel}. Latest value: ${summary}`}
    >
      <path d={path} fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" />
      {numericPoints.map((point) => (
        <circle
          key={point.index}
          cx={toX(point.index)}
          cy={toY(point.value)}
          r={point.index === lastPoint.index ? 3 : 1.5}
        />
      ))}
    </svg>
  );
}
