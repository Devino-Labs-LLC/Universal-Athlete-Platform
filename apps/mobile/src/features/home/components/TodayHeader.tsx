import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText } from '@/src/core/components/Surface';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface TodayHeaderProps {
  greeting: string;
  date: string;
}

/** Compact date/greeting context — not the visual hero (readiness is). */
export function TodayHeader({ greeting, date }: TodayHeaderProps) {
  const theme = useAppTheme();

  return (
    <View testID="today-header" style={styles.wrap}>
      <View style={styles.topRow}>
        <EyebrowText tone="cyan">Today</EyebrowText>
        <Text style={[styles.date, { color: theme.colors.textMuted }]}>
          {formatDateDisplay(date)}
        </Text>
      </View>
      <Text
        accessibilityRole="header"
        style={[
          styles.greeting,
          {
            color: theme.colors.text,
            fontSize: theme.typography.pageTitle,
          },
        ]}>
        {greeting}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: 4,
    marginBottom: 2,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
  },
  greeting: {
    fontWeight: '700',
    letterSpacing: -0.3,
  },
  date: {
    fontSize: 13,
    fontWeight: '600',
  },
});
