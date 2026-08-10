import { StyleSheet, Text } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';

export default function WorkoutExecutePlaceholderRoute() {
  const theme = useAppTheme();
  const { occurrenceId } = useLocalSearchParams<{ occurrenceId: string }>();

  return (
    <Screen testID="workout-execute-placeholder">
      <Text style={[styles.title, { color: theme.colors.text }]}>Workout execution (M5)</Text>
      <Text style={[styles.body, { color: theme.colors.textMuted }]}>
        Live workout execution for occurrence {occurrenceId} will arrive in milestone M5. No start
        mutation is performed in M4.
      </Text>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 20,
    fontWeight: '700',
  },
  body: {
    fontSize: 15,
    lineHeight: 22,
  },
});
