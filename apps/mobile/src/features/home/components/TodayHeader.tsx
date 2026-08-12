import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { EyebrowText } from '@/src/core/components/Surface';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface TodayHeaderProps {
  greeting: string;
  date: string;
}

export function TodayHeader({ greeting, date }: TodayHeaderProps) {
  const theme = useAppTheme();

  return (
    <View testID="today-header" style={styles.wrap}>
      <EyebrowText tone="cyan">Today</EyebrowText>
      <Text
        accessibilityRole="header"
        style={[
          styles.greeting,
          {
            color: theme.colors.text,
            fontSize: theme.typography.display,
          },
        ]}>
        {greeting}
      </Text>
      <Text style={[styles.date, { color: theme.colors.textMuted }]}>
        {formatDateDisplay(date)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    gap: 6,
    marginBottom: 4,
  },
  greeting: {
    fontWeight: '700',
  },
  date: {
    fontSize: 15,
  },
});
