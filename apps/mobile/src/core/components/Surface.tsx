import { PropsWithChildren } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';

interface SurfaceProps extends PropsWithChildren {
  elevated?: boolean;
  style?: ViewStyle;
  testID?: string;
}

/** Elevated panel used across Home and shared empty/error shells. */
export function Surface({ elevated = false, style, testID, children }: SurfaceProps) {
  const theme = useAppTheme();
  return (
    <View
      testID={testID}
      style={[
        styles.surface,
        {
          backgroundColor: elevated ? theme.colors.surfaceElevated : theme.colors.surface,
          borderColor: theme.colors.border,
          borderRadius: theme.radius.lg,
          padding: theme.spacing.lg,
          gap: theme.spacing.sm,
        },
        style,
      ]}>
      {children}
    </View>
  );
}

interface EyebrowTextProps {
  children: string;
  tone?: 'muted' | 'cyan' | 'lime' | 'danger';
  testID?: string;
}

export function EyebrowText({ children, tone = 'muted', testID }: EyebrowTextProps) {
  const theme = useAppTheme();
  const color =
    tone === 'cyan'
      ? theme.colors.accentCyan
      : tone === 'lime'
        ? theme.colors.primary
        : tone === 'danger'
          ? theme.colors.danger
          : theme.colors.textMuted;

  return (
    <Text
      testID={testID}
      style={[
        styles.eyebrow,
        {
          color,
          fontSize: theme.typography.eyebrow,
        },
      ]}>
      {children}
    </Text>
  );
}

interface SectionHeaderProps {
  title: string;
  subtitle?: string;
}

export function SectionHeader({ title, subtitle }: SectionHeaderProps) {
  const theme = useAppTheme();
  return (
    <View style={styles.sectionHeader}>
      <Text style={[styles.sectionTitle, { color: theme.colors.text }]}>{title}</Text>
      {subtitle ? (
        <Text style={[styles.sectionSubtitle, { color: theme.colors.textMuted }]}>{subtitle}</Text>
      ) : null}
    </View>
  );
}

type StatusTone = 'default' | 'success' | 'warning' | 'danger' | 'info';

interface StatusBadgeProps {
  label: string;
  tone?: StatusTone;
  testID?: string;
}

export function StatusBadge({ label, tone = 'default', testID }: StatusBadgeProps) {
  const theme = useAppTheme();
  const { backgroundColor, color } = (() => {
    switch (tone) {
      case 'success':
        return { backgroundColor: theme.colors.successMuted, color: theme.colors.success };
      case 'warning':
        return { backgroundColor: theme.colors.warningMuted, color: theme.colors.warning };
      case 'danger':
        return { backgroundColor: theme.colors.dangerMuted, color: theme.colors.danger };
      case 'info':
        return { backgroundColor: theme.colors.infoMuted, color: theme.colors.info };
      default:
        return { backgroundColor: theme.colors.surfaceMuted, color: theme.colors.textMuted };
    }
  })();

  return (
    <View
      testID={testID}
      accessibilityRole="text"
      accessibilityLabel={label}
      style={[styles.badge, { backgroundColor }]}>
      <Text style={[styles.badgeLabel, { color }]}>{label}</Text>
    </View>
  );
}

interface CompactInfoRowProps {
  label: string;
  value: string;
  testID?: string;
}

/** Label / value row for session meta, progress, and profile summaries. */
export function CompactInfoRow({ label, value, testID }: CompactInfoRowProps) {
  const theme = useAppTheme();
  return (
    <View testID={testID} style={styles.infoRow}>
      <Text style={[styles.infoLabel, { color: theme.colors.textMuted }]}>{label}</Text>
      <Text style={[styles.infoValue, { color: theme.colors.text }]} numberOfLines={3}>
        {value}
      </Text>
    </View>
  );
}

interface MetricTileProps {
  label: string;
  /** Pass null/undefined to render em dash — never coerce missing to zero. */
  value: string | number | null | undefined;
  caption?: string;
  testID?: string;
}

/** Large numeric metric with unavailable treatment for null. */
export function MetricTile({ label, value, caption, testID }: MetricTileProps) {
  const theme = useAppTheme();
  const display =
    value === null || value === undefined || (typeof value === 'number' && Number.isNaN(value))
      ? '—'
      : String(value);

  return (
    <View
      testID={testID}
      accessibilityLabel={`${label}: ${display}`}
      style={[
        styles.metricTile,
        {
          backgroundColor: theme.colors.surfaceMuted,
          borderColor: theme.colors.border,
        },
      ]}>
      <Text
        style={[
          styles.metricLabel,
          {
            color: theme.colors.textMuted,
            fontSize: theme.typography.eyebrow,
          },
        ]}>
        {label}
      </Text>
      <Text
        style={[
          styles.metricValue,
          {
            color: theme.colors.text,
            fontSize: theme.typography.metric,
          },
        ]}>
        {display}
      </Text>
      {caption ? (
        <Text style={[styles.metricCaption, { color: theme.colors.textMuted }]}>{caption}</Text>
      ) : null}
    </View>
  );
}

interface ScoreRingProps {
  /** 0–100 score, or null when unavailable. */
  score: number | null | undefined;
  label?: string;
  testID?: string;
}

/** Compact readiness-style score display. Null renders unavailable, never 0. */
export function ScoreRing({ score, label = 'Score', testID }: ScoreRingProps) {
  const theme = useAppTheme();
  const available = score != null && !Number.isNaN(score);
  const display = available ? String(Math.round(Math.min(100, Math.max(0, score)))) : '—';

  return (
    <View testID={testID} accessibilityLabel={`${label}: ${display}`} style={styles.scoreWrap}>
      <Text
        style={[
          styles.scoreValue,
          {
            color: theme.colors.text,
            fontSize: theme.typography.metric,
          },
        ]}>
        {display}
      </Text>
      <Text style={[styles.scoreLabel, { color: theme.colors.textMuted }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  surface: {
    borderWidth: 1,
  },
  eyebrow: {
    fontWeight: '700',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  sectionHeader: {
    gap: 4,
  },
  sectionTitle: {
    fontSize: 17,
    fontWeight: '700',
  },
  sectionSubtitle: {
    fontSize: 14,
    lineHeight: 20,
  },
  badge: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 5,
    minHeight: 28,
    justifyContent: 'center',
  },
  badgeLabel: {
    fontSize: 12,
    fontWeight: '600',
  },
  infoRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 12,
    minHeight: 28,
  },
  infoLabel: {
    fontSize: 14,
    flexShrink: 0,
  },
  infoValue: {
    fontSize: 14,
    fontWeight: '600',
    flex: 1,
    textAlign: 'right',
  },
  metricTile: {
    flex: 1,
    minWidth: 96,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 12,
    gap: 4,
  },
  metricLabel: {
    fontWeight: '700',
    letterSpacing: 1.1,
    textTransform: 'uppercase',
  },
  metricValue: {
    fontWeight: '700',
  },
  metricCaption: {
    fontSize: 12,
  },
  scoreWrap: {
    alignItems: 'flex-start',
    gap: 2,
  },
  scoreValue: {
    fontWeight: '700',
  },
  scoreLabel: {
    fontSize: 13,
  },
});
