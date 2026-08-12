import { Circle, Svg } from 'react-native-svg';
import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

type ScoreRingSize = 'sm' | 'md' | 'lg';

interface ScoreRingProps {
  /** 0–100 score, or null when unavailable. Never treat missing as zero fill. */
  score: number | null | undefined;
  label?: string;
  size?: ScoreRingSize;
  /** Stroke accent; defaults to cyan (info). Lime for strong athlete status. */
  tone?: 'cyan' | 'lime' | 'success' | 'warning' | 'danger';
  testID?: string;
}

const SIZE_PX: Record<ScoreRingSize, number> = {
  sm: 64,
  md: 88,
  lg: 112,
};

/**
 * Circular readiness-style score. Null/NaN → empty track + em dash (never 0 fill).
 */
export function ScoreRing({
  score,
  label = 'Score',
  size = 'md',
  tone = 'cyan',
  testID,
}: ScoreRingProps) {
  const theme = useAppTheme();
  const available = score != null && !Number.isNaN(score);
  const clamped = available ? Math.min(100, Math.max(0, score)) : null;
  const display = clamped != null ? String(Math.round(clamped)) : '—';

  const dimension = SIZE_PX[size];
  const strokeWidth = size === 'lg' ? 8 : size === 'md' ? 7 : 6;
  const radius = (dimension - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress =
    clamped != null ? (clamped / 100) * circumference : 0;

  const accent =
    tone === 'lime' || tone === 'success'
      ? theme.colors.primary
      : tone === 'warning'
        ? theme.colors.warning
        : tone === 'danger'
          ? theme.colors.danger
          : theme.colors.accentCyan;

  const valueSize =
    size === 'lg' ? 28 : size === 'md' ? 22 : 16;

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLabel={`${label}: ${display}`}
      accessibilityValue={
        clamped != null ? { min: 0, max: 100, now: Math.round(clamped) } : undefined
      }
      style={[styles.wrap, { width: dimension, height: dimension }]}>
      <Svg width={dimension} height={dimension} style={styles.svg}>
        <Circle
          cx={dimension / 2}
          cy={dimension / 2}
          r={radius}
          stroke={theme.colors.borderStrong}
          strokeWidth={strokeWidth}
          fill="none"
        />
        {clamped != null ? (
          <Circle
            cx={dimension / 2}
            cy={dimension / 2}
            r={radius}
            stroke={accent}
            strokeWidth={strokeWidth}
            fill="none"
            strokeLinecap="round"
            strokeDasharray={`${circumference} ${circumference}`}
            strokeDashoffset={circumference - progress}
            transform={`rotate(-90 ${dimension / 2} ${dimension / 2})`}
          />
        ) : null}
      </Svg>
      <View style={styles.center} pointerEvents="none">
        <Text
          style={[
            styles.value,
            {
              color: theme.colors.text,
              fontSize: valueSize,
            },
          ]}>
          {display}
        </Text>
        <Text
          style={[styles.label, { color: theme.colors.textMuted }]}
          numberOfLines={1}>
          {label}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  svg: {
    position: 'absolute',
  },
  center: {
    alignItems: 'center',
    justifyContent: 'center',
    gap: 1,
    paddingHorizontal: 8,
  },
  value: {
    fontWeight: '700',
  },
  label: {
    fontSize: 10,
    fontWeight: '600',
    letterSpacing: 0.4,
    textTransform: 'uppercase',
  },
});
