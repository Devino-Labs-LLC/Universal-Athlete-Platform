import { StyleSheet, Text, View } from 'react-native';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { formatDateDisplay } from '@/src/features/home/utils/formatDateDisplay';

interface TodayHeaderProps {
  greeting: string;
  date: string;
}

export function TodayHeader({ greeting, date }: TodayHeaderProps) {
  const theme = useAppTheme();

  return (
    <View testID="today-header">
      <Text style={[styles.greeting, { color: theme.colors.text }]}>{greeting}</Text>
      <Text style={[styles.date, { color: theme.colors.textMuted }]}>
        {formatDateDisplay(date)}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  greeting: {
    fontSize: 26,
    fontWeight: '700',
  },
  date: {
    fontSize: 15,
    marginTop: 4,
  },
});
