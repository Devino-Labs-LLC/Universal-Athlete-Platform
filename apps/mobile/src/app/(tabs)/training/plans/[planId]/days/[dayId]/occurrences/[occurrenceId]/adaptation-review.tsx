import { StyleSheet, Text } from 'react-native';
import { useLocalSearchParams } from 'expo-router';

import { useAppTheme } from '@/src/app/theme/ThemeProvider';
import { Screen } from '@/src/core/components/Screen';

export default function AdaptationReviewPlaceholderRoute() {
  const theme = useAppTheme();
  const { occurrenceId } = useLocalSearchParams<{ occurrenceId: string }>();

  return (
    <Screen testID="adaptation-review-placeholder">
      <Text style={[styles.title, { color: theme.colors.text }]}>Adaptation review (M7)</Text>
      <Text style={[styles.body, { color: theme.colors.textMuted }]}>
        Reviewing and applying workout adaptations for occurrence {occurrenceId} is planned for
        milestone M7.
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
